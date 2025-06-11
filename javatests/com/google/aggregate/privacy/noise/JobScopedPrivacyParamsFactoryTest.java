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
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.LaplaceDpParams;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.Mechanism;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JobScopedPrivacyParamsFactoryTest {

  private static final Double DEFAULT_EPSILON = 0.1;

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject JobScopedPrivacyParamsFactory factory;

  @Test
  public void forDefaultPrivacyParams() throws Exception {
    JobScopedPrivacyParams privacyParams =
        factory.fromRequestInfo(RequestInfo.getDefaultInstance());

    assertThat(privacyParams.mechanism()).isEqualTo(Mechanism.LAPLACE_DP);
    assertThat(privacyParams.laplaceDp())
        .isEqualTo(
            LaplaceDpParams.builder().setEpsilon(0.1).setL1Sensitivity(4L).setDelta(5.00).build());
  }

  @Test
  public void forDebugPrivacyEpsilon() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .putJobParameters(JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON, "64.0")
            .build();
    JobScopedPrivacyParams privacyParams = factory.fromRequestInfo(requestInfo);

    assertThat(privacyParams.mechanism()).isEqualTo(Mechanism.LAPLACE_DP);
    assertThat(privacyParams.laplaceDp())
        .isEqualTo(
            LaplaceDpParams.builder().setEpsilon(64.0).setL1Sensitivity(4L).setDelta(5.00).build());
  }

  @Test
  public void forDebugPrivacyEpsilon_ignoreIfMalformed() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .putJobParameters(JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON, "invalid")
            .build();
    JobScopedPrivacyParams privacyParams = factory.fromRequestInfo(requestInfo);

    assertThat(privacyParams.mechanism()).isEqualTo(Mechanism.LAPLACE_DP);
    assertThat(privacyParams.laplaceDp())
        .isEqualTo(
            LaplaceDpParams.builder()
                .setEpsilon(DEFAULT_EPSILON)
                .setL1Sensitivity(4L)
                .setDelta(5.00)
                .build());
  }

  @Test
  public void forDebugPrivacyEpsilon_failIfTooLarge() {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .putJobParameters(JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON, "64.1")
            .build();
    assertThrows(AggregationJobProcessException.class, () -> factory.fromRequestInfo(requestInfo));
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(DEFAULT_EPSILON);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
    }

    @Provides
    Supplier<PrivacyParameters> provideNoiseParameters(
        PrivacyParametersSupplier privacyParametersSupplier) {
      return privacyParametersSupplier;
    }
  }
}
