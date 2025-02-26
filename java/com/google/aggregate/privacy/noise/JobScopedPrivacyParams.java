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

package com.google.aggregate.privacy.noise;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;

/** Contains privacy parameters that have been configured for a specific job. */
@AutoOneOf(JobScopedPrivacyParams.Mechanism.class)
public abstract class JobScopedPrivacyParams {
  public enum Mechanism {
    LAPLACE_DP;
  }

  /** Indicates which differential privacy mechanism to use. */
  public abstract Mechanism mechanism();

  /** Contains privacy parameters for use with the {@code LAPLACE_DP} mechanism. */
  public abstract LaplaceDpParams laplaceDp();

  public static JobScopedPrivacyParams ofLaplace(LaplaceDpParams laplaceDpParams) {
    return AutoOneOf_JobScopedPrivacyParams.laplaceDp(laplaceDpParams);
  }

  /** Contains privacy parameters for use with the {@code LAPLACE_DP} mechanism. */
  @AutoValue
  public abstract static class LaplaceDpParams {

    public abstract double epsilon();

    abstract long l1Sensitivity();

    /**
     * This is only used for optional output domains. It would be more suitable in its own Params
     * class, but we'll put it here for now.
     */
    abstract double delta();

    public static Builder builder() {
      return new AutoValue_JobScopedPrivacyParams_LaplaceDpParams.Builder();
    }

    /** Builds privacy parameters for use with the {@code LAPLACE_DP} mechanism. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setEpsilon(double epsilon);

      public abstract Builder setL1Sensitivity(long l1Sensitivity);

      public abstract Builder setDelta(double delta);

      public abstract LaplaceDpParams build();
    }
  }
}
