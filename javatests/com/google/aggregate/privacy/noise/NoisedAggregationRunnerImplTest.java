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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.ParallelAggregatedFactNoising;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NoisedAggregationRunnerImplTest {

  private static final ImmutableList<Long> VALUE_NOISE_LIST = ImmutableList.of(10L, -10L);
  private static final AggregatedFact NOISED_FACT1 =
      AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 15, 5L);
  private static final AggregatedFact NOISED_FACT2 =
      AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 490, 500L);

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject private CustomDeltaPrivacyParamsSupplier customDeltaPrivacyParamsSupplier;
  @Inject private FakeThresholdSupplier thresholdSupplier;

  // Under test.
  @Inject private Provider<NoisedAggregationRunner> noisedAggregationRunner;
  @Inject private NoisedAggregationRunnerFlagsHelper aggregationRunnerFlagsHelper;

  @Before
  public void setUp() {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder().setNextValueNoiseToAdd(VALUE_NOISE_LIST.iterator()).build());
    aggregationRunnerFlagsHelper.setParallelAggregatedFactNoisingEnabled(false);
  }

  @Test
  public void noise() {
    thresholdSupplier.setThreshold(0);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void noise_parallelNoisingEnabled() {
    thresholdSupplier.setThreshold(0);
    aggregationRunnerFlagsHelper.setParallelAggregatedFactNoisingEnabled(true);
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextValueNoiseToAdd(ImmutableList.of(10L, 10L).iterator())
            .build());

    AggregatedFact noisedFact1 =
        AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 15, /* unnoisedMetric= */ 5L);
    AggregatedFact noisedFact2 =
        AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 510, /* unnoisedMetric= */ 500L);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(noisedFact1, noisedFact2);
  }

  @Test
  public void noise_noThresholding() {
    thresholdSupplier.setThreshold(10000);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ false, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void threshold() {
    thresholdSupplier.setThreshold(30);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  @Test
  public void countEqualToIntThreshold() {
    thresholdSupplier.setThreshold(10);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void countEqualToDoubleThreshold() {
    thresholdSupplier.setThreshold(10.0001);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void countLessThanDoubleThreshold() {
    thresholdSupplier.setThreshold(25.0002);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.empty());

    assertThat(result.privacyParameters()).isEqualTo(customDeltaPrivacyParamsSupplier.get());
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  @Test
  public void testDebugEpsilonRequestScoped() {
    thresholdSupplier.setThreshold(25.0002);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(), /* doThreshold= */ true, /* debugPrivacyEpsilon= */ Optional.of(0.2));

    // debugEpsilon is not used with constant noise, but we can check that the epsilon is updated in
    // the privacy params.
    assertThat(result.privacyParameters().getL1Sensitivity())
        .isEqualTo(customDeltaPrivacyParamsSupplier.get().getL1Sensitivity());
    assertThat(result.privacyParameters().getDelta())
        .isEqualTo(customDeltaPrivacyParamsSupplier.get().getDelta());
    assertThat(result.privacyParameters().getEpsilon()).isEqualTo(0.2);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  private NoisedAggregationResult noiseAndThreshold(
      ImmutableList<AggregatedFact> input,
      boolean doThreshold,
      Optional<Double> debugPrivacyEpsilon) {
    NoisedAggregationResult noisedAggregationResult =
        noisedAggregationRunner.get().noise(input, debugPrivacyEpsilon);

    return doThreshold
        ? noisedAggregationRunner
            .get()
            .threshold(noisedAggregationResult.noisedAggregatedFacts(), debugPrivacyEpsilon)
        : noisedAggregationResult;
  }

  private static ImmutableList<AggregatedFact> getTestFacts() {
    AggregatedFact fact1 =
        AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 5, /* unnoisedMetric= */ 5L);
    AggregatedFact fact2 =
        AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 500, /* unnoisedMetric= */ 500L);

    return ImmutableList.of(fact1, fact2);
  }

  /**
   * Test implementation of {@code PrivacyParameters} supplier to allow delta to be modified for
   * testing threshold.
   */
  private static final class CustomDeltaPrivacyParamsSupplier
      implements Supplier<PrivacyParameters> {

    private final PrivacyParametersSupplier privacyParams;
    private double delta = 1e-5;

    /**
     * Supplies {@code PrivacyParameters} from text config in order to populate all required fields
     * of the proto.
     */
    @Inject
    private CustomDeltaPrivacyParamsSupplier(PrivacyParametersSupplier privacyParametersSupplier) {
      privacyParams = privacyParametersSupplier;
    }

    @Override
    public PrivacyParameters get() {
      return privacyParams.get().toBuilder().setDelta(delta).build();
    }

    public void setDelta(double delta) {
      this.delta = delta;
    }
  }

  private static final class FakeThresholdSupplier implements Supplier<Double> {

    private double threshold;

    public void setThreshold(double threshold) {
      this.threshold = threshold;
    }

    @Override
    public Double get() {
      return threshold;
    }
  }

  private static final class NoisedAggregationRunnerFlagsHelper {
    private boolean parallelAggregatedFactNoisingEnabled = false;

    public void setParallelAggregatedFactNoisingEnabled(boolean parallelEnabled) {
      this.parallelAggregatedFactNoisingEnabled = parallelEnabled;
    }

    public boolean isParallelAggregatedFactNoisingEnabled() {
      return parallelAggregatedFactNoisingEnabled;
    }
  }

  private static final class TestEnv extends AbstractModule {
    private final NoisedAggregationRunnerFlagsHelper flagsHelper =
        new NoisedAggregationRunnerFlagsHelper();

    @Override
    protected void configure() {
      bind(NoisedAggregationRunner.class).to(NoisedAggregationRunnerImpl.class);
      bind(CustomDeltaPrivacyParamsSupplier.class).in(TestScoped.class);
      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(FakeThresholdSupplier.class).in(TestScoped.class);

      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
      bind(NoisedAggregationRunnerFlagsHelper.class).toInstance(flagsHelper);
    }

    @Provides
    @ParallelAggregatedFactNoising
    boolean provideParallelAggregatedFactNoising() {
      return flagsHelper.isParallelAggregatedFactNoisingEnabled();
    }

    @Provides
    Supplier<PrivacyParameters> providePrivacyParamConfig(
        CustomDeltaPrivacyParamsSupplier supplier) {
      return supplier;
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }

    @Provides
    @Threshold
    Supplier<Double> provideThreshold(FakeThresholdSupplier thresholdSupplier) {
      return thresholdSupplier;
    }

    @Provides
    @Singleton
    @CustomForkJoinThreadPool
    ListeningExecutorService provideCustomForkJoinThreadPool() {
      return newDirectExecutorService();
    }
  }
}
