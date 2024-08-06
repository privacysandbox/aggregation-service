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

import static com.google.aggregate.adtech.worker.model.SharedInfo.SUPPORTED_MAJOR_VERSIONS;

import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;

/**
 * Describes a category of error message, used to distinguish between processing or validation
 * failures reported by the {@link ErrorSummary} class.
 */
public enum ErrorCounter {
  ATTRIBUTION_REPORT_TO_MALFORMED(
      "Report's shared_info.reporting_origin domain is malformed. Domain must be syntactically"
          + " valid and have an Effective Top Level Domain (eTLD)."),
  ATTRIBUTION_REPORT_TO_MISMATCH(
      "Report's shared_info.reporting_origin value does not match attribution_report_to value set"
          + " in the Aggregation job parameters. Aggregation request job parameters must have"
          + " attribution_report_to set to report's shared_info.reporting_origin value."),
  DECRYPTION_ERROR(
      "Unable to decrypt the report. This may be caused by: tampered aggregatable report shared"
          + " info, corrupt encrypted report, or other such issues."),
  DEBUG_NOT_ENABLED(
      "Reports without shared_info.debug_mode enabled cannot be processed in a debug run."),
  DECRYPTION_KEY_NOT_FOUND("Could not find decryption key on private key endpoint."),
  DECRYPTION_KEY_FETCH_ERROR(
      "Fetching the decryption key for report decryption failed. This can happen using an"
          + " unapproved aggregation service binary, running the aggregation service binary in"
          + " debug mode, key corruption or service availability issues."),
  NUM_REPORTS_WITH_ERRORS(
      "Total number of reports that had an error. These reports were not considered in aggregation."
          + " See additional error messages for details on specific reasons."),
  ORIGINAL_REPORT_TIME_TOO_OLD(
      String.format(
          "Report's shared_info.scheduled_report_time is too old, reports cannot be older than %s"
              + " days.",
          SharedInfo.MAX_REPORT_AGE.toDays())),
  INTERNAL_ERROR("Internal error occurred during operation."),
  REPORTING_SITE_MISMATCH(
          "Report's shared_info.reporting_origin value does not belong to the reporting_site value set"
                  + " in the Aggregation job parameters. Aggregation request job parameters must have"
                  + " reporting_site set to the site which corresponds to the shared_info.reporting_origin"
                  + " value."),
  UNSUPPORTED_OPERATION(
      String.format(
          "Report's operation is unsupported. Supported operations are %s.",
          SharedInfo.SUPPORTED_OPERATIONS)),
  UNSUPPORTED_REPORT_API_TYPE(
      String.format(
          "The report's API type is not supported for aggregation. Supported APIs are %s",
          SharedInfo.SUPPORTED_APIS)),
  REQUIRED_SHAREDINFO_FIELD_INVALID("One or more required SharedInfo fields are empty or invalid."),
  INVALID_REPORT_ID("Report ID missing or invalid in SharedInfo."),
  UNSUPPORTED_SHAREDINFO_VERSION(
      String.format(
          "Report has an unsupported version value in its shared_info. Supported values for report"
              + " shared_info major version(s) are: %s",
          SUPPORTED_MAJOR_VERSIONS));
  private String description;

  ErrorCounter(String description) {
    this.description = description;
  }

  /**
   * Returns the description of the error category.
   */
  public String getDescription() {
    return description;
  }
}
