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

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.privacy.noise.DpNoiseParamsFactory.LaplaceNoiseParams;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DpNoiseParamsFactoryTest {
  private static final double EPSILON = 1.0;

  // sensitivity for value.
  private static final int L1_SENSITIVITY_VALUE = 5;

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject PrivacyParameters privacyParameters;

  @Before
  public void setUp() {
    privacyParameters =
        privacyParameters.toBuilder()
            .setEpsilon(EPSILON)
            .setNoiseParameters(NoiseParameters.newBuilder().setDistribution(Distribution.LAPLACE))
            .setL1Sensitivity(L1_SENSITIVITY_VALUE)
            .build();
  }

  @Test
  public void ofLaplaceNoiseAndValueSensitivity() {
    DpNoiseParamsFactory noiseParamsFactory = DpNoiseParamsFactory.ofLaplace(privacyParameters);

    LaplaceNoiseParams expected =
        LaplaceNoiseParams.builder()
            .setL1Sensitivity(L1_SENSITIVITY_VALUE)
            .setEpsilon(EPSILON)
            .build();
    assertThat(noiseParamsFactory.distribution()).isEqualTo(Distribution.LAPLACE);
    assertThat(noiseParamsFactory.laplace()).isEqualTo(expected);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
    }

    @Provides
    PrivacyParameters provideNoiseParameters(PrivacyParametersSupplier privacyParametersSupplier) {
      return privacyParametersSupplier.get();
    }
  }
}
