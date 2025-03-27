/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.model;

import static java.time.temporal.ChronoUnit.HOURS;

import com.google.auto.value.AutoValue;
import java.time.Instant;

@AutoValue
public abstract class PrivacyBudgetUnit {

  public static PrivacyBudgetUnit createHourTruncatedUnit(
      String privacyBudgetKey, Instant scheduledReportTime, String reportingOrigin) {
    return Builder.builder()
        .privacyBudgetKey(privacyBudgetKey)
        .scheduledReportTime(scheduledReportTime.truncatedTo(HOURS))
        .reportingOrigin(reportingOrigin)
        .build();
  }

  public abstract Builder toBuilder();

  public abstract String privacyBudgetKey();

  public abstract Instant scheduledReportTime();

  public abstract String reportingOrigin();

  @AutoValue.Builder
  abstract static class Builder {
    static Builder builder() {
      return new AutoValue_PrivacyBudgetUnit.Builder();
    }

    abstract Builder privacyBudgetKey(String privacyBudgetKey);

    abstract Builder scheduledReportTime(Instant reportingWindow);

    abstract Builder reportingOrigin(String reportingOrigin);

    abstract PrivacyBudgetUnit build();
  }
}
