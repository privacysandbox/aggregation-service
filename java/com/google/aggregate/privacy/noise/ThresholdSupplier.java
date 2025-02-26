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

import com.google.inject.Inject;

/**
 * Threshold Supplier computes the threshold using the privacy parameters l1sensitivity, epsilon and
 * delta. The threshold is going to be applied post noise. The noise is a random sample from a
 * laplace distribution with mean zero and scale = l1sensitivity/epsilon. The threshold is
 * calculated as scale * (epsilon + ln(1/delta)). The privacy analysis is out of scope for this
 * code.
 */
public class ThresholdSupplier {

  @Inject
  public ThresholdSupplier() {}

  public Double get(JobScopedPrivacyParams privacyParams) {
    double delta = privacyParams.laplaceDp().delta();
    double epsilon = privacyParams.laplaceDp().epsilon();
    checkArgument(delta > 0 && delta < 1, "Delta should be greater than zero and less than 1");
    double scale = privacyParams.laplaceDp().l1Sensitivity() / epsilon;
    double tempValue = epsilon + Math.log(1 / delta);
    return scale * tempValue;
  }
}
