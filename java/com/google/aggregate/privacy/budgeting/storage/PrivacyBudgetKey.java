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

package com.google.aggregate.privacy.budgeting.storage;

import com.google.auto.value.AutoValue;
import java.time.Instant;

/**
 * Wrapper for the composite primary key used in the privacy budget database. The key consists of
 * the privacy budget key and the original report time.
 */
@AutoValue
public abstract class PrivacyBudgetKey {
  public static PrivacyBudgetKey.Builder builder() {
    return new AutoValue_PrivacyBudgetKey.Builder();
  }

  /** The privacy budget key in the encrypted report that the browser encodes. */
  public abstract String privacyBudgetKey();

  /**
   * The coarse 1-hour time interval that the conversion occurred that is also part of the encrypted
   * report.
   */
  public abstract Instant originalReportTime();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract PrivacyBudgetKey.Builder setPrivacyBudgetKey(String privacyBudgetKey);

    public abstract PrivacyBudgetKey.Builder setOriginalReportTime(Instant originalReportTime);

    public abstract PrivacyBudgetKey build();
  }
}
