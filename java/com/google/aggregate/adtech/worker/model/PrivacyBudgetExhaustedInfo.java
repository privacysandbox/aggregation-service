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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/** Information provided to adtech for debugging PRIVACY_BUDGET_EXHAUSTED scenarios. */
@AutoValue
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("privacy_budget_exhausted_info")
@JsonSerialize(as = PrivacyBudgetExhaustedInfo.class)
public abstract class PrivacyBudgetExhaustedInfo {
  public static Builder builder() {
    return new AutoValue_PrivacyBudgetExhaustedInfo.Builder();
  }

  @JsonView(Views.UsedInPrivacyBudgeting.class)
  @JsonProperty("aggregatable_input_budget_consumption_info")
  public abstract ImmutableSet<AggregatableInputBudgetConsumptionInfo>
      aggregatableInputBudgetConsumptionInfos();

  @JsonCreator
  public static PrivacyBudgetExhaustedInfo create(
      @JsonProperty("aggregatable_input_budget_consumption_info")
          ImmutableSet<AggregatableInputBudgetConsumptionInfo>
              aggregatableInputBudgetConsumptionInfos) {
    return builder()
        .setAggregatableInputBudgetConsumptionInfos(aggregatableInputBudgetConsumptionInfos)
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setAggregatableInputBudgetConsumptionInfos(
        ImmutableSet<AggregatableInputBudgetConsumptionInfo>
            aggregatableInputBudgetConsumptionInfos);

    public abstract PrivacyBudgetExhaustedInfo build();
  }
}
