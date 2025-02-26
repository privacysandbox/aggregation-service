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

import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.LaplaceDpParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ThresholdSupplierTest {

  @Test
  public void threshold_whenDelta_0() {
    JobScopedPrivacyParams privacyParams = getPrivacyParams(4l, 64, 0);
    ThresholdSupplier thresholdSupplier = new ThresholdSupplier();
    assertThrows(IllegalArgumentException.class, () -> thresholdSupplier.get(privacyParams));
  }

  @Test
  public void threshold_whenDelta_1() {
    JobScopedPrivacyParams privacyParams = getPrivacyParams(4l, 64, 1);
    ThresholdSupplier thresholdSupplier = new ThresholdSupplier();
    assertThrows(IllegalArgumentException.class, () -> thresholdSupplier.get(privacyParams));
  }

  @Test
  public void threshold_whenDelta_1e_5() {
    JobScopedPrivacyParams privacyParams = getPrivacyParams(1, 1, 1e-5);
    ThresholdSupplier thresholdSupplier = new ThresholdSupplier();
    assertThat(thresholdSupplier.get(privacyParams)).isEqualTo(12.512925464970229);
  }

  @Test
  public void threshold_whenDelta_1e_5_L1_16() {
    JobScopedPrivacyParams privacyParams =
        getPrivacyParams(Double.valueOf(Math.pow(2, 16)).longValue(), 1, 1e-5);
    ThresholdSupplier thresholdSupplier = new ThresholdSupplier();
    assertThat(thresholdSupplier.get(privacyParams)).isEqualTo(820047.0832722889);
  }

  @Test
  public void threshold_whenDelta_1e_5_L1_16_eps_64() {
    JobScopedPrivacyParams privacyParams =
        getPrivacyParams(Double.valueOf(Math.pow(2, 16)).longValue(), 64, 1e-5);
    ThresholdSupplier thresholdSupplier = new ThresholdSupplier();
    assertThat(thresholdSupplier.get(privacyParams)).isEqualTo(77325.23567612951);
  }

  private JobScopedPrivacyParams getPrivacyParams(
      long l1Sensitivity, double epsilon, double delta) {
    return JobScopedPrivacyParams.ofLaplace(
        LaplaceDpParams.builder()
            .setEpsilon(epsilon)
            .setL1Sensitivity(l1Sensitivity)
            .setDelta(delta)
            .build());
  }
}
