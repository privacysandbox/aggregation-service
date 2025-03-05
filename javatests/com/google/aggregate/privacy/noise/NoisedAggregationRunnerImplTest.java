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
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.LaplaceDpParams;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeThresholdSupplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.math.BigInteger;
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

  private static final double DEFAULT_EPSILON = 0.1;
  private static final long DEFAULT_L1_SENSITIVITY = 4;
  private static final double DEFAULT_DELTA = 5.00;
  private static final JobScopedPrivacyParams DEFAULT_PRIVACY_PARAMS =
      JobScopedPrivacyParams.ofLaplace(
          LaplaceDpParams.builder()
              .setEpsilon(DEFAULT_EPSILON)
              .setL1Sensitivity(DEFAULT_L1_SENSITIVITY)
              .setDelta(DEFAULT_DELTA)
              .build());

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject private FakeThresholdSupplier thresholdSupplier;

  // Under test.
  @Inject private JobScopedPrivacyParamsFactory privacyParamsFactory;
  @Inject private Provider<NoisedAggregationRunner> noisedAggregationRunner;
  @Inject private NoisedAggregationRunnerFlagsHelper aggregationRunnerFlagsHelper;

  @Before
  public void setUp() {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder().setNextValueNoiseToAdd(VALUE_NOISE_LIST.iterator()).build());
    aggregationRunnerFlagsHelper.setParallelAggregatedFactNoisingEnabled(false);
  }

  @Test
  public void noise() throws Exception {
    thresholdSupplier.setThreshold(0);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void noise_parallelNoisingEnabled() throws Exception {
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
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(noisedFact1, noisedFact2);
  }

  @Test
  public void noise_noThresholding() throws Exception {
    thresholdSupplier.setThreshold(10000);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ false,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void threshold() throws Exception {
    thresholdSupplier.setThreshold(30);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  @Test
  public void countEqualToIntThreshold() throws Exception {
    thresholdSupplier.setThreshold(10);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void countEqualToDoubleThreshold() throws Exception {
    thresholdSupplier.setThreshold(10.0001);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT1, NOISED_FACT2);
  }

  @Test
  public void countLessThanDoubleThreshold() throws Exception {
    thresholdSupplier.setThreshold(25.0002);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(RequestInfo.getDefaultInstance()));

    assertThat(result.privacyParameters()).isEqualTo(DEFAULT_PRIVACY_PARAMS);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  @Test
  public void testDebugEpsilonRequestScoped() throws Exception {
    thresholdSupplier.setThreshold(25.0002);

    NoisedAggregationResult result =
        noiseAndThreshold(
            getTestFacts(),
            /* doThreshold= */ true,
            privacyParamsFactory.fromRequestInfo(
                RequestInfo.newBuilder()
                    .putJobParameters(JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON, "0.2")
                    .build()));

    // debugEpsilon is not used with constant noise, but we can check that the epsilon is updated in
    // the privacy params.
    assertThat(result.privacyParameters().laplaceDp().l1Sensitivity())
        .isEqualTo(DEFAULT_L1_SENSITIVITY);
    assertThat(result.privacyParameters().laplaceDp().delta()).isEqualTo(DEFAULT_DELTA);
    assertThat(result.privacyParameters().laplaceDp().epsilon()).isEqualTo(0.2);
    assertThat(result.noisedAggregatedFacts()).containsExactly(NOISED_FACT2);
  }

  private NoisedAggregationResult noiseAndThreshold(
      ImmutableList<AggregatedFact> input,
      boolean doThreshold,
      JobScopedPrivacyParams privacyParams) {
    NoisedAggregationResult noisedAggregationResult =
        noisedAggregationRunner.get().noise(input, privacyParams);

    return doThreshold
        ? noisedAggregationRunner
            .get()
            .threshold(noisedAggregationResult.noisedAggregatedFacts(), privacyParams)
        : noisedAggregationResult;
  }

  private static ImmutableList<AggregatedFact> getTestFacts() {
    AggregatedFact fact1 =
        AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 5, /* unnoisedMetric= */ 5L);
    AggregatedFact fact2 =
        AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 500, /* unnoisedMetric= */ 500L);

    return ImmutableList.of(fact1, fact2);
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
    Supplier<PrivacyParameters> provideDefaultPrivacyParams(PrivacyParametersSupplier supplier) {
      return supplier;
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }

    @Provides
    ThresholdSupplier provideThreshold(FakeThresholdSupplier thresholdSupplier) {
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
