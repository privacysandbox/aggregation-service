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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.aggregate.privacy.noise.Annotations.DpValue;
import com.google.aggregate.privacy.noise.DpNoiseParamsFactory.LaplaceNoiseParams;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.inject.Inject;
import java.util.OptionalDouble;

/** {@link NoiseApplier} implementation using Google's Differential Privacy library. */
public final class DpNoiseApplier implements NoiseApplier {

  private final DpNoiseParamsFactory valueNoiseParams;

  @Inject
  DpNoiseApplier(@DpValue DpNoiseParamsFactory valueNoiseParams) {
    this.valueNoiseParams = valueNoiseParams;
  }

  @Override
  public Long noiseMetric(Long metric) {
    return noise(metric, valueNoiseParams);
  }

  private static Long noise(Long rawValue, DpNoiseParamsFactory noiseParams) {
    checkArgument(
        noiseParams.distribution().equals(Distribution.LAPLACE),
        "Only Laplace noising distribution supported. Got: " + noiseParams.distribution());

    LaplaceNoiseParams laplaceNoiseParams = noiseParams.laplace();
    return laplaceNoiseParams
        .noise()
        .addNoise(rawValue, laplaceNoiseParams.l1Sensitivity(), laplaceNoiseParams.epsilon());
  }

  /**
   * Interface to hold differential privacy noise related parameters.
   *
   * <p>TODO: Move this interface under model.
   */
  public interface DpNoiseParams {

    double epsilon();

    OptionalDouble delta();

    long l1Sensitivity();
  }
}
