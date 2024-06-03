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

package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;

/** PrivacyBudgetKeyGenerator is used to generate privacy budget key for Reports */
public interface PrivacyBudgetKeyGenerator {
  String PRIVACY_BUDGET_KEY_DELIMITER = "-";

  String generatePrivacyBudgetKey(PrivacyBudgetKeyInput privacyBudgetKeyInput);

  /** An input object containing values for generating Privacy Budget Key. */
  @AutoValue
  abstract class PrivacyBudgetKeyInput {
    public static Builder builder() {
      return new AutoValue_PrivacyBudgetKeyGenerator_PrivacyBudgetKeyInput.Builder();
    }

    public abstract SharedInfo sharedInfo();

    /** Queried filteringId to be included in the budget key calculation for reports > V1.0. */
    public abstract Optional<UnsignedLong> filteringId();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract PrivacyBudgetKeyInput build();

      public abstract Builder setSharedInfo(SharedInfo sharedInfo);

      public abstract Builder setFilteringId(UnsignedLong filteringId);
    }
  }
}
