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

import com.google.aggregate.privacy.noise.DpNoiseApplier.DpNoiseParams;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.privacy.differentialprivacy.LaplaceNoise;
import java.util.OptionalDouble;

/**
 * Contains factory methods for Google's differential privacy noise related params.
 *
 * <p>TODO: Move this and related classes under model.
 */
@AutoOneOf(Distribution.class)
public abstract class DpNoiseParamsFactory {

  public abstract Distribution distribution();

  public abstract LaplaceNoiseParams laplace();

  /**
   * Creates {@link DpNoiseParams} with epsilon and noise from {@param noiseParameters} and
   * sensitivity from {@param sensitivity}.
   */
  public static DpNoiseParamsFactory ofLaplace(PrivacyParameters privacyParameters) {
    return AutoOneOf_DpNoiseParamsFactory.laplace(
        LaplaceNoiseParams.builder()
            .setEpsilon(privacyParameters.getEpsilon())
            .setL1Sensitivity(privacyParameters.getL1Sensitivity())
            .build());
  }

  /**
   * Implementation of {@link DpNoiseParams} that constructs an {@link LaplaceNoise} object with
   * delta equal to {@code OptionalDouble.empty()}.
   */
  @AutoValue
  public abstract static class LaplaceNoiseParams implements DpNoiseParams {

    @Memoized
    public DiscreteNoise noise() {
      return new LaplaceDiscreteNoise();
    }

    @Override
    public OptionalDouble delta() {
      return OptionalDouble.empty();
    }

    static Builder builder() {
      return new AutoValue_DpNoiseParamsFactory_LaplaceNoiseParams.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

      abstract Builder setEpsilon(double epsilon);

      abstract Builder setL1Sensitivity(long l1Sensitivity);

      abstract LaplaceNoiseParams build();
    }
  }
}
