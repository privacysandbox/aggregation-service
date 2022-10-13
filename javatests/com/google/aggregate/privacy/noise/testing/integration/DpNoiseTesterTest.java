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

package com.google.aggregate.privacy.noise.testing.integration;

import static com.google.common.collect.Streams.concat;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.privacy.noise.DpNoiseApplier.DpNoiseParams;
import com.google.aggregate.privacy.noise.DpNoiseParamsFactory;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.aggregate.privacy.noise.testing.proto.NoiseTester.DpNoiseTesterParams;
import com.google.aggregate.privacy.noise.testing.proto.NoiseTester.DpNoiseTesterParams.NoiseValueType;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Singleton;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class DpNoiseTesterTest {

  // These values are used to also test the validity of the StatisticalTestUtil.verifyApproximateDp
  // function from
  // https://github.com/google/differential-privacy/blob/main/java/tests/com/google/privacy/differentialprivacy/testing/StatisticalTestsUtilTest.java
  private static final ImmutableList<Long> SAMPLES_A =
      ImmutableList.of(1L, 2L, 2L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L);
  private static final ImmutableList<Long> SAMPLES_B =
      ImmutableList.of(1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 4L, 4L, 5L);

  private static final double DEFAULT_EPSILON = 1.0;
  private static final double DEFAULT_DELTA_TOLERANCE = 0.00001;
  private static final int NUM_VOTES = 2;

  // These values are set to 0 so that noise applier returns the provided samples without any
  // modification.
  private static final int RAW_VALUE = 0;
  private static final int DISTANCE = 0;

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject private DpNoiseParamsSupplier dpNoiseParamsSupplier;
  @Inject private DpNoiseTesterParamsSupplier dpNoiseTesterParamsSupplier;
  @Inject private PrivacyParameters privacyParameters;

  // Under test.
  @Inject private DpNoiseTester dpNoiseTester;

  @TestParameter private NoiseValueType noiseValueType;

  private FakeNoiseApplier.Builder fakeNoiseBuilder;
  private Consumer<Iterator<Long>> fakeNoiseSetter;

  @Before
  public void setUp() {

    dpNoiseTesterParamsSupplier.setDpNoiseTesterParams(
        DpNoiseTesterParams.newBuilder()
            .setRawValue(RAW_VALUE)
            .setDistance(DISTANCE)
            .setNumberOfSamples(SAMPLES_A.size())
            .setNumberOfVotes(NUM_VOTES)
            .setDeltaTolerance(DEFAULT_DELTA_TOLERANCE)
            .setNoiseValueType(noiseValueType)
            .build());

    // Call noise or count FakeNoiseApplier based on test parameter noiseValueType.
    fakeNoiseBuilder = FakeNoiseApplier.builder();
    fakeNoiseSetter = fakeNoiseBuilder::setNextValueNoiseToAdd;
  }

  @Test
  public void noiseIsDifferentiallyPrivate_sameDistribution() {
    // Here we concat two lists since FakeNoiseApplier uses a single iterator to noise two samples
    // and repeat for NUM_VOTES.
    fakeNoiseSetter.accept(
        concat(SAMPLES_A.stream(), SAMPLES_A.stream(), SAMPLES_A.stream(), SAMPLES_A.stream())
            .iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(fakeNoiseBuilder.build());

    ImmutableList<Boolean> isDpVotes = dpNoiseTester.noiseIsDifferentiallyPrivate();

    assertThat(isDpVotes.size()).isEqualTo(NUM_VOTES);
    assertThat(isDpVotes).doesNotContain(false);
  }

  @Test
  public void noiseIsDifferentiallyPrivate_differentDistribution() {
    // Here we concat two lists since FakeNoiseApplier uses a single iterator to noise two samples
    // and repeat for NUM_VOTES.
    fakeNoiseSetter.accept(
        concat(SAMPLES_A.stream(), SAMPLES_B.stream(), SAMPLES_A.stream(), SAMPLES_B.stream())
            .iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(fakeNoiseBuilder.build());

    ImmutableList<Boolean> isDpVotes = dpNoiseTester.noiseIsDifferentiallyPrivate();

    assertThat(isDpVotes.size()).isEqualTo(NUM_VOTES);
    assertThat(isDpVotes).doesNotContain(true);
  }

  /** Supplies {@link DpNoiseParamsFactory} using laplace distribution. */
  private static final class DpNoiseParamsSupplier implements Supplier<DpNoiseParamsFactory> {
    PrivacyParameters privacyParameters;
    DpNoiseParamsFactory dpNoiseParamsFactory;

    @Inject
    DpNoiseParamsSupplier(PrivacyParameters privacyParameters) {
      this.privacyParameters = privacyParameters;
      this.dpNoiseParamsFactory = DpNoiseParamsFactory.ofLaplace(privacyParameters);
    }

    @Override
    public DpNoiseParamsFactory get() {
      return dpNoiseParamsFactory;
    }
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(NoisedSamplesGenerator.class).in(TestScoped.class);
      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(DpNoiseParamsSupplier.class).in(TestScoped.class);
      bind(DpNoiseTesterParamsSupplier.class).in(TestScoped.class);

      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }

    @Provides
    @Singleton
    PrivacyParameters provideNoiseParameters(PrivacyParametersSupplier privacyParametersSupplier) {
      return privacyParametersSupplier.get().toBuilder()
          .setNoiseParameters(
              NoiseParameters.newBuilder().setDistribution(Distribution.LAPLACE).build())
          .setEpsilon(DEFAULT_EPSILON)
          .build();
    }

    @Provides
    Supplier<DpNoiseParamsFactory> provideDpNoiseParamsFactory(DpNoiseParamsSupplier supplier) {
      return supplier;
    }

    @Provides
    Supplier<DpNoiseParams> provideDpNoiseParams(DpNoiseParamsSupplier supplier) {
      return () -> supplier.get().laplace();
    }

    @Provides
    Supplier<DpNoiseTesterParams> provideDpNoiseTesterParams(DpNoiseTesterParamsSupplier supplier) {
      return supplier;
    }
  }
}
