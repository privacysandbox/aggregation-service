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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.Views;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.UnsignedLong;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** PrivacyBudgetKeyGenerator is used to generate privacy budget key for Reports */
public interface PrivacyBudgetKeyGenerator {
  String PRIVACY_BUDGET_KEY_DELIMITER = "-";

  String generatePrivacyBudgetKey(PrivacyBudgetKeyInput privacyBudgetKeyInput);

  boolean validatePrivacyBudgetKeyInput(PrivacyBudgetKeyInput privacyBudgetKeyInput);

  /** An input object containing values for generating Privacy Budget Key. */
  @JsonView(Views.UsedInPrivacyBudgeting.class)
  @AutoValue
  abstract class PrivacyBudgetKeyInput {
    public static Builder builder() {
      return new AutoValue_PrivacyBudgetKeyGenerator_PrivacyBudgetKeyInput.Builder();
    }

    @JsonProperty("relevant_shared_info")
    public abstract SharedInfo sharedInfo();

    /** Queried filteringId to be included in the budget key calculation for reports > V1.0. */
    @JsonView(Views.UsedInPrivacyBudgeting.class)
    @JsonProperty("filtering_id")
    public abstract Optional<UnsignedLong> filteringId();

    @JsonCreator
    public static PrivacyBudgetKeyInput create(
        @JsonProperty("relevant_shared_info") SharedInfo sharedInfo,
        @JsonProperty("filtering_id") UnsignedLong filteringId) {
      return builder().setSharedInfo(sharedInfo).setFilteringId(filteringId).build();
    }

    /**
     * Only include {shared_info, filtering_id} fields that are part of privacy budget key
     * calculation in equality check.
     */
    @Override
    public boolean equals(Object pbkInput) {
      if (this == pbkInput) {
        return true;
      }
      if (pbkInput == null || getClass() != pbkInput.getClass()) {
        return false;
      }
      PrivacyBudgetKeyInput that = (PrivacyBudgetKeyInput) pbkInput;
      return getPrivacyBudgetingRelevantSharedInfo()
              .equals(that.getPrivacyBudgetingRelevantSharedInfo())
          && this.filteringId().equals(that.filteringId());
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          this.getPrivacyBudgetingRelevantSharedInfo(),
          filteringId().orElse(UnsignedLong.valueOf(0)));
    }

    /**
     * Returns a representation of {@link PrivacyBudgetKeyInput#sharedInfo()} that includes only the
     * properties relevant for privacy budgeting.
     *
     * <p>This method is used to determine equality between {@link
     * PrivacyBudgetKeyInput#sharedInfo()} objects within the context of privacy budgeting.
     *
     * <p>For example, if two `SharedInfo` objects differ only in their `report_id`, this method
     * ensures that the SharedInfo are treated as one for privacy budgeting debugging purposes. This
     * prevents duplicate entries in privacy budgeting debugging information.
     *
     * <p><b>Important:</b> When a new field is added to `shared_info` and included in privacy
     * budgeting calculations, it must also be added to this method to maintain consistency.
     *
     * @return A representation of `sharedInfo()` containing only privacy-budgeting relevant
     *     properties.
     */
    private SharedInfo getPrivacyBudgetingRelevantSharedInfo() {
      return SharedInfo.builder()
          .setApi(sharedInfo().api().orElse(""))
          .setVersion(sharedInfo().version())
          .setReportingOrigin(sharedInfo().reportingOrigin())
          .setScheduledReportTime(sharedInfo().scheduledReportTime())
          .setSourceRegistrationTime(sharedInfo().sourceRegistrationTime().orElse(Instant.EPOCH))
          .setDestination(sharedInfo().destination().orElse(""))
          .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract PrivacyBudgetKeyInput build();

      public abstract Builder setSharedInfo(SharedInfo sharedInfo);

      public abstract Builder setFilteringId(UnsignedLong filteringId);
    }
  }
}
