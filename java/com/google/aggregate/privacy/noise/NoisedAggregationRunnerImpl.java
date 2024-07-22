/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.privacy.noise;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.ParallelAggregatedFactNoising;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.errorprone.annotations.Var;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Implementation of {@code NoisedAggregationRunner} that uses Google's differential privacy library
 * through {@code NoiseApplier} to apply noise.
 */
public final class NoisedAggregationRunnerImpl implements NoisedAggregationRunner {
  private static final double TOLERANCE = 0.0001;
  private final Supplier<NoiseApplier> noiseApplierSupplier;
  private final Supplier<PrivacyParameters> privacyParams;
  private final Supplier<Double> thresholdSupplier;
  private final Optional<ListeningExecutorService> noisingForkJoinPool;

  @Inject
  NoisedAggregationRunnerImpl(
      Supplier<NoiseApplier> noiseApplierSupplier,
      Supplier<PrivacyParameters> privacyParams,
      @Threshold Supplier<Double> thresholdSupplier,
      @ParallelAggregatedFactNoising boolean parallelNoising,
      @CustomForkJoinThreadPool ListeningExecutorService forkJoinPool) {
    this.noiseApplierSupplier = noiseApplierSupplier;
    this.privacyParams = privacyParams;
    this.thresholdSupplier = thresholdSupplier;
    this.noisingForkJoinPool = parallelNoising ? Optional.of(forkJoinPool) : Optional.empty();
  }

  @Override
  public NoisedAggregationResult threshold(
      Iterable<AggregatedFact> aggregatedFacts, Optional<Double> debugPrivacyEpsilon) {
    final Supplier<PrivacyParameters> requestScopedPrivacyParamsSupplier =
        getScopedPrivacyParamSupplier(debugPrivacyEpsilon);
    final Supplier<Double> requestScopedThresholdSupplier =
        getScopedThreshold(debugPrivacyEpsilon, requestScopedPrivacyParamsSupplier);
    double threshold = requestScopedThresholdSupplier.get();

    ImmutableList<AggregatedFact> thresholdedFacts =
        Streams.stream(aggregatedFacts)
            .filter(
                aggregatedFactItem ->
                    (DoubleMath.fuzzyCompare(aggregatedFactItem.getMetric(), threshold, TOLERANCE)
                        >= 0))
            .collect(toImmutableList());

    return NoisedAggregationResult.create(
        requestScopedPrivacyParamsSupplier.get(), thresholdedFacts);
  }

  /*
   * Noises AggregatedFact#metric using Google's DP library. AggregatedFact#metric is interpreted as
   * unnoised data and copied to the AggregatedFact#unnoisedMetric field, and the noised value is
   * set in the AggregatedFact#metric field.
   */
  @Override
  public NoisedAggregationResult noise(
      Iterable<AggregatedFact> aggregatedFact, Optional<Double> debugPrivacyEpsilon) {
    final Supplier<PrivacyParameters> requestScopedPrivacyParamsSupplier =
        getScopedPrivacyParamSupplier(debugPrivacyEpsilon);
    final Supplier<NoiseApplier> requestScopedNoiseApplier =
        getScopedNoiseApplier(debugPrivacyEpsilon, requestScopedPrivacyParamsSupplier);

    @Var ImmutableList<AggregatedFact> noisedFacts;
    if (this.noisingForkJoinPool.isPresent()) {
      try {
        noisedFacts =
            this.noisingForkJoinPool
                .get()
                .submit(
                    () ->
                        Streams.stream(aggregatedFact)
                            .parallel()
                            .map(fact -> noiseSingleFact(fact, requestScopedNoiseApplier))
                            .collect(toImmutableList()))
                .get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException("Exception while noising aggregated data.");
      }
    } else {
      noisedFacts =
          Streams.stream(aggregatedFact)
              .map((fact -> noiseSingleFact(fact, requestScopedNoiseApplier)))
              .collect(toImmutableList());
    }

    return NoisedAggregationResult.create(requestScopedPrivacyParamsSupplier.get(), noisedFacts);
  }

  private AggregatedFact noiseSingleFact(
      AggregatedFact aggregatedFact, Supplier<NoiseApplier> scopedNoiseApplier) {
    return noiseSingleFact(aggregatedFact, scopedNoiseApplier.get());
  }

  private AggregatedFact noiseSingleFact(AggregatedFact aggregatedFact, NoiseApplier noiseApplier) {
    long unnoisedMetric = aggregatedFact.getMetric();
    aggregatedFact.setUnnoisedMetric(Optional.of(unnoisedMetric));
    aggregatedFact.setMetric(noiseApplier.noiseMetric(unnoisedMetric));
    return aggregatedFact;
  }

  private Supplier<PrivacyParameters> getScopedPrivacyParamSupplier(
      Optional<Double> debugPrivacyEpsilon) {
    if (debugPrivacyEpsilon.isPresent()) {
      PrivacyParameters globalPrivacyParams = privacyParams.get();
      PrivacyParameters overridenPrivacyParams =
          PrivacyParameters.newBuilder()
              .setDelta(globalPrivacyParams.getDelta())
              .setL1Sensitivity(globalPrivacyParams.getL1Sensitivity())
              .setEpsilon(debugPrivacyEpsilon.get())
              .build();
      return () -> overridenPrivacyParams;
    }
    return privacyParams;
  }

  private Supplier<NoiseApplier> getScopedNoiseApplier(
      Optional<Double> debugPrivacyEpsilon,
      Supplier<PrivacyParameters> scopedPrivacyParametersSupplier) {
    // TODO find better way of per request dependencies
    if (debugPrivacyEpsilon.isPresent() && noiseApplierSupplier.get() instanceof DpNoiseApplier) {
      NoiseApplier overridenNoiseSupplier =
          new DpNoiseApplier(DpNoiseParamsFactory.ofLaplace(scopedPrivacyParametersSupplier.get()));
      return () -> overridenNoiseSupplier;
    }
    return noiseApplierSupplier;
  }

  private Supplier<Double> getScopedThreshold(
      Optional<Double> debugPrivacyEpsilon,
      Supplier<PrivacyParameters> scopedPrivacyParametersSupplier) {
    if (debugPrivacyEpsilon.isPresent()) {
      return () -> new ThresholdSupplier(scopedPrivacyParametersSupplier).get();
    }
    return thresholdSupplier;
  }
}
