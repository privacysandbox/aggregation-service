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
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.function.Supplier;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DpNoiseApplierTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject JobScopedPrivacyParamsFactory jobScopedPrivacyParamsFactory;
  @Inject DpNoiseApplier noiseApplier;

  @Test
  public void noiseDoesNotCrash() throws Exception {
    JobScopedPrivacyParams privacyParams =
        jobScopedPrivacyParamsFactory.fromRequestInfo(RequestInfo.newBuilder().build());

    // This test does not validate result. It passes if value is noised without throwing any
    // exceptions.
    Long noisedValue = noiseApplier.noiseMetric(100L, privacyParams);

    assertThat(noisedValue).isNotNull();
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(NoiseApplier.class).to(DpNoiseApplier.class);
    }

    @Provides
    @Singleton
    Supplier<PrivacyParameters> provideDefaultNoiseParameters() {
      return () ->
          PrivacyParameters.newBuilder()
              .setNoiseParameters(
                  NoiseParameters.newBuilder().setDistribution(Distribution.LAPLACE).build())
              .setEpsilon(1)
              .setL1Sensitivity(4)
              .setDelta(5.0)
              .build();
    }
  }
}
