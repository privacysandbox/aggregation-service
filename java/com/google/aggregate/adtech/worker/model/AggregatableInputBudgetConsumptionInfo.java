/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.auto.value.AutoValue;

/**
 * Budget consumption relevant information for aggregatable input. Aggregatable input is a
 * combination of aggregateable reports and filtering ids. The budget consumption related
 * information is captured in PrivacyBudgetKeyInput.
 */
@AutoValue
public abstract class AggregatableInputBudgetConsumptionInfo {
  public static Builder builder() {
    return new AutoValue_AggregatableInputBudgetConsumptionInfo.Builder();
  }

  @JsonView(Views.UsedInPrivacyBudgeting.class)
  @JsonProperty("aggregateable_input_budget_id")
  public abstract PrivacyBudgetKeyInput privacyBudgetKeyInput();

  @JsonCreator
  public static AggregatableInputBudgetConsumptionInfo create(
      @JsonProperty("aggregateable_input_budget_id") PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    return builder().setPrivacyBudgetKeyInput(privacyBudgetKeyInput).build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPrivacyBudgetKeyInput(PrivacyBudgetKeyInput privacyBudgetKeyInput);

    public abstract AggregatableInputBudgetConsumptionInfo build();
  }
}
