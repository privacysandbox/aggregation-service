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
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import java.util.List;
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
  private final ThresholdSupplier thresholdSupplier;
  private final Optional<ListeningExecutorService> noisingForkJoinPool;

  @Inject
  NoisedAggregationRunnerImpl(
      Supplier<NoiseApplier> noiseApplierSupplier,
      ThresholdSupplier thresholdSupplier,
      @ParallelAggregatedFactNoising boolean parallelNoising,
      @CustomForkJoinThreadPool ListeningExecutorService forkJoinPool) {
    this.noiseApplierSupplier = noiseApplierSupplier;
    this.thresholdSupplier = thresholdSupplier;
    this.noisingForkJoinPool = parallelNoising ? Optional.of(forkJoinPool) : Optional.empty();
  }

  @Override
  public NoisedAggregationResult createResultSet(
      List<AggregatedFact> facts, JobScopedPrivacyParams privacyParams) {
    return NoisedAggregationResult.create(privacyParams, ImmutableList.copyOf(facts));
  }

  @Override
  public ImmutableList<AggregatedFact> thresholdAggregatedFacts(
      List<AggregatedFact> aggregatedFacts, JobScopedPrivacyParams privacyParams) {
    double threshold = thresholdSupplier.get(privacyParams);

    return threshold(aggregatedFacts, threshold);
  }

  @Override
  public NoisedAggregationResult threshold(
      Iterable<AggregatedFact> aggregatedFacts, JobScopedPrivacyParams privacyParams) {
    double threshold = thresholdSupplier.get(privacyParams);

    ImmutableList<AggregatedFact> thresholdedFacts = threshold(aggregatedFacts, threshold);

    return NoisedAggregationResult.create(privacyParams, thresholdedFacts);
  }

  /*
   * Noises AggregatedFact#metric using Google's DP library. AggregatedFact#metric is interpreted as
   * unnoised data and copied to the AggregatedFact#unnoisedMetric field, and the noised value is
   * set in the AggregatedFact#metric field.
   */
  @Override
  public NoisedAggregationResult noise(
      Iterable<AggregatedFact> aggregatedFact, JobScopedPrivacyParams privacyParams) {
    ImmutableList<AggregatedFact> noisedFacts;
    if (this.noisingForkJoinPool.isPresent()) {
      try {
        noisedFacts =
            this.noisingForkJoinPool
                .get()
                .submit(
                    () ->
                        Streams.stream(aggregatedFact)
                            .parallel()
                            .map(fact -> noiseSingleFact(fact, privacyParams))
                            .collect(toImmutableList()))
                .get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException("Exception while noising aggregated data.");
      }
    } else {
      noisedFacts =
          Streams.stream(aggregatedFact)
              .map((fact -> noiseSingleFact(fact, privacyParams)))
              .collect(toImmutableList());
    }

    return NoisedAggregationResult.create(privacyParams, noisedFacts);
  }

  private ImmutableList<AggregatedFact> threshold(
      Iterable<AggregatedFact> aggregatedFacts, double threshold) {
    return Streams.stream(aggregatedFacts)
        .filter(
            aggregatedFactItem ->
                (DoubleMath.fuzzyCompare(aggregatedFactItem.getMetric(), threshold, TOLERANCE)
                    >= 0))
        .collect(toImmutableList());
  }

  @Override
  public AggregatedFact noiseSingleFact(
      AggregatedFact aggregatedFact, JobScopedPrivacyParams privacyParams) {
    long unnoisedMetric = aggregatedFact.getMetric();
    aggregatedFact.setUnnoisedMetric(Optional.of(unnoisedMetric));
    aggregatedFact.setMetric(noiseApplierSupplier.get().noiseMetric(unnoisedMetric, privacyParams));
    return aggregatedFact;
  }
}
