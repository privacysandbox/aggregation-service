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

/**
 * Wrapper for the privacy budget in the privacy budget database. This contains the values
 * associated with a {@link com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey}.
 */
@AutoValue
public abstract class PrivacyBudget {
  public static PrivacyBudget.Builder builder() {
    return new AutoValue_PrivacyBudget.Builder();
  }

  /**
   * The total budget consumed by the {@link
   * com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey}.
   */
  public abstract int consumedBudget();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract PrivacyBudget.Builder setConsumedBudget(int consumedBudget);

    public abstract PrivacyBudget build();
  }
}
