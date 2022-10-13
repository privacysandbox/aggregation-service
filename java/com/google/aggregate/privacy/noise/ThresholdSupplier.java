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

import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.Inject;
import java.util.function.Supplier;

/**
 * Threshold Supplier computes the threshold using the privacy parameters l1sensitivity, epsilon and
 * delta. The threshold is going to be applied post noise. The noise is a random sample from a
 * laplace distribution with mean zero and scale = l1sensitivity/epsilon. The threshold is
 * calculated as scale * (epsilon + ln(1/delta)). The privacy analysis is out of scope for this
 * code.
 */
@Threshold
public class ThresholdSupplier implements Supplier<Double> {

  private final PrivacyParameters privacyParameters;

  @Inject
  public ThresholdSupplier(Supplier<PrivacyParameters> privacyParametersSupplier) {
    this.privacyParameters = privacyParametersSupplier.get();
  }

  @Override
  public Double get() {
    checkArgument(
        privacyParameters.getDelta() > 0 && privacyParameters.getDelta() < 1,
        "Delta should be greater than zero and less than 1");
    double scale = privacyParameters.getL1Sensitivity() / privacyParameters.getEpsilon();
    double tempValue = privacyParameters.getEpsilon() + Math.log(1 / privacyParameters.getDelta());
    return scale * tempValue;
  }
}
