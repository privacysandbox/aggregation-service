/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.privacy.noise.testing;

import com.google.aggregate.privacy.noise.JobScopedPrivacyParams;
import com.google.aggregate.privacy.noise.ThresholdSupplier;
import javax.inject.Inject;

/** A fake version of threshold supplier that allows tests to configure deterministic thresholds. */
public final class FakeThresholdSupplier extends ThresholdSupplier {

  private double threshold;

  @Inject
  FakeThresholdSupplier() {
    this(0.0);
  }

  public FakeThresholdSupplier(double threshold) {
    this.threshold = threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  @Override
  public Double get(JobScopedPrivacyParams unused) {
    return threshold;
  }
}
