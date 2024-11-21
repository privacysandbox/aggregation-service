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

import static java.time.temporal.ChronoUnit.DAYS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
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

  public static final String VERSION_0_1 = "0.1";
  public static final String VERSION_1_0 = "1.0";
  public static final String LATEST_VERSION = VERSION_1_0;
  public static final String MAJOR_VERSION_ZERO = "0";
  public static final String MAJOR_VERSION_ONE = "1";
  // TODO(b/303480127): Remove isZero() from Version class in ReportVersionValidator when
  // MAJOR_VERSION_ZERO is removed from SUPPORTED_MAJOR_VERSIONS.
  public static final ImmutableSet<String> SUPPORTED_MAJOR_VERSIONS =
      ImmutableSet.of(MAJOR_VERSION_ZERO, MAJOR_VERSION_ONE);
  public static final boolean DEFAULT_DEBUG_MODE = false;
  public static final String ATTRIBUTION_REPORTING_API = "attribution-reporting";
  public static final String ATTRIBUTION_REPORTING_DEBUG_API = "attribution-reporting-debug";
  public static final String PROTECTED_AUDIENCE_API = "protected-audience";
  public static final String SHARED_STORAGE_API = "shared-storage";
  // Used by {@link ErrorCounter} to form error messages. @SupportedApis is the list used for
  // validation at runtime. The two lists would differ temporarily when support for a new API is
  // under development.
  public static final ImmutableSet<String> SUPPORTED_APIS =
      ImmutableSet.of(
          ATTRIBUTION_REPORTING_API,
          ATTRIBUTION_REPORTING_DEBUG_API,
          PROTECTED_AUDIENCE_API,
          SHARED_STORAGE_API);
  // Max age of reports accepted for aggregation.
  public static final Duration MAX_REPORT_AGE = Duration.of(90, DAYS);
  public static final ImmutableSet<String> SUPPORTED_OPERATIONS =
      ImmutableSet.of(Payload.HISTOGRAM_OPERATION);

  public static Builder builder() {
    return new AutoValue_SharedInfo.Builder().setReportDebugMode(DEFAULT_DEBUG_MODE);
  }

  public abstract Builder toBuilder();

  // TODO(b/263901045) : consider moving version to api specific code.
  // Version of the report
  @JsonProperty("version")
  public abstract String version();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("api")
  public abstract Optional<String> api();

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
}
