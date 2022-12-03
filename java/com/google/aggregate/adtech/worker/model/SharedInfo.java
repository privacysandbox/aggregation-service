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

package com.google.aggregate.adtech.worker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * Plaintext shared_info from the report. Contains information the ad-tech can view but must also be
 * used by the aggregate service to process reports.
 *
 * <p>Intended to match the spec here:
 *
 * <p>https://github.com/WICG/conversion-measurement-api/blob/d732ca597b5efbfdeb44523879a2c9cf2fcf4aa1/AGGREGATE.md#aggregatable-reports
 */
@AutoValue
@JsonDeserialize(builder = SharedInfo.Builder.class)
@JsonSerialize(as = SharedInfo.class)
public abstract class SharedInfo {

  public static final String DEFAULT_VERSION = "";
  public static final String VERSION_0_1 = "0.1";
  public static final String LATEST_VERSION = VERSION_0_1;
  public static final boolean DEFAULT_DEBUG_MODE = false;
  public static final String PRIVACY_BUDGET_KEY_DELIMITER = "-";

  public static Builder builder() {
    return new AutoValue_SharedInfo.Builder()
        .setVersion(DEFAULT_VERSION)
        .setReportDebugMode(DEFAULT_DEBUG_MODE);
  }

  // Version of the report
  @JsonProperty("version")
  public abstract String version();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("api")
  public abstract Optional<String> api();

  // Privacy Budget Key will not be passed in SharedInfo starting version 0.1
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("privacy_budget_key")
  public abstract Optional<String> privacyBudgetKey();

  @JsonProperty("scheduled_report_time")
  public abstract Instant scheduledReportTime();

  // Ad-tech eTLD+1
  @JsonProperty("reporting_origin")
  public abstract String reportingOrigin();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("attribution_destination")
  public abstract Optional<String> destination();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("source_registration_time")
  public abstract Optional<Instant> sourceRegistrationTime();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("report_id")
  public abstract Optional<String> reportId();

  // String Debug mode value for writing json.
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("debug_mode")
  public abstract Optional<String> reportDebugModeString();

  // Convert the debugMode string field to boolean.
  @JsonIgnore
  public final boolean getReportDebugMode() {
    return reportDebugModeString().isPresent() && reportDebugModeString().get().equals("enabled");
  }

  @AutoValue.Builder
  // Ignore unknown as some fields will be present that aren't used right now
  @JsonIgnoreProperties(ignoreUnknown = true)
  public abstract static class Builder {

    // Used by Jackson for JSON deserialization
    @JsonCreator
    public static Builder builder() {
      return new AutoValue_SharedInfo.Builder().setReportDebugMode(DEFAULT_DEBUG_MODE);
    }

    @JsonProperty("version")
    public abstract Builder setVersion(String value);

    @JsonProperty("api")
    public abstract Builder setApi(String value);

    @JsonProperty("privacy_budget_key")
    public abstract Builder setPrivacyBudgetKey(String value);

    @JsonProperty("scheduled_report_time")
    public abstract Builder setScheduledReportTime(Instant value);

    @JsonProperty("reporting_origin")
    public abstract Builder setReportingOrigin(String value);

    @JsonProperty("attribution_destination")
    public abstract Builder setDestination(String value);

    @JsonProperty("source_registration_time")
    public abstract Builder setSourceRegistrationTime(Instant value);

    @JsonProperty("report_id")
    public abstract Builder setReportId(String value);

    @JsonProperty("debug_mode")
    public abstract Builder setReportDebugModeString(String value);

    /**
     * Use boolean values for debug mode in the program and convert it to string value enabled for
     * result json files.
     */
    @JsonIgnore
    public final Builder setReportDebugMode(Boolean value) {
      if (value) {
        return setReportDebugModeString("enabled");
      }
      return this;
    }

    public abstract SharedInfo build(); // not public
  }
  /**
   * If version is set to "0.1", returns privacy budget key hash using following shared Info fields-
   * api, version, reporting_origin, destination and source_registration_time. If version is not
   * "0.1" and Privacy Budget Key is provided, returns Privacy Budget Key as provided.
   */
  public String getPrivacyBudgetKey() {
    if (version().equals(VERSION_0_1)) {
      String privacyBudgetKeyHashInput =
          api().get()
              + PRIVACY_BUDGET_KEY_DELIMITER
              + version()
              + PRIVACY_BUDGET_KEY_DELIMITER
              + reportingOrigin()
              + PRIVACY_BUDGET_KEY_DELIMITER
              + destination().get()
              + PRIVACY_BUDGET_KEY_DELIMITER
              + sourceRegistrationTime().get();
      return Hashing.sha256()
          .newHasher()
          .putBytes(privacyBudgetKeyHashInput.getBytes(StandardCharsets.UTF_8))
          .hash()
          .toString();
    }

    if (privacyBudgetKey().isPresent()) {
      return privacyBudgetKey().get();
    }
    throw new IllegalStateException("Unable to get Privacy Budget Key");
  }
}
