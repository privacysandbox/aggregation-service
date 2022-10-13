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

import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.testing.proto.NoiseTester.DpNoiseTesterParams;
import com.google.inject.Inject;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Generates noised samples for testing that the noising algorithm is differentially private.
 *
 * <p>Uses{@link NoiseApplier} to generate noised samples from rawValue and distance.
 */
public final class NoisedSamplesGenerator {

  Supplier<DpNoiseTesterParams> dpNoiseTesterParams;
  Supplier<NoiseApplier> dpNoiseApplier;

  @Inject
  public NoisedSamplesGenerator(
      Supplier<DpNoiseTesterParams> dpNoiseTesterParams, Supplier<NoiseApplier> noiseApplier) {
    this.dpNoiseTesterParams = dpNoiseTesterParams;
    this.dpNoiseApplier = noiseApplier;
  }

  /** Noises a single value repeatedly and store result in a {@code Long[]}. */
  public Long[] generateNoisedSamples() {
    return generateNoisedSamples(dpNoiseTesterParams.get().getRawValue());
  }

  /**
   * Noises a single value shifted by some constant repeatedly and store result in a {@code Long[]}.
   *
   * <p>The constant should be chosen such that the noised distribution of values returned by {@code
   * generateNoisedSamples} is indistinguishable from the noised distribution of values returned by
   * {@code generateShiftedNoisedSamples}
   */
  public Long[] generateShiftedNoisedSamples() {
    return generateNoisedSamples(
        dpNoiseTesterParams.get().getRawValue() + dpNoiseTesterParams.get().getDistance());
  }

  private Long[] generateNoisedSamples(long sample) {
    return Stream.generate(() -> noiseSample(sample))
        .limit(dpNoiseTesterParams.get().getNumberOfSamples())
        .toArray(Long[]::new);
  }

  private Long noiseSample(long sample) {
    switch (dpNoiseTesterParams.get().getNoiseValueType()) {
      case VALUE:
        return dpNoiseApplier.get().noiseMetric(sample);
      default:
        throw new IllegalStateException(
            "Unexpected value: " + dpNoiseTesterParams.get().getNoiseValueType());
    }
  }
}
