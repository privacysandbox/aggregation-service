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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_ATTRIBUTION_REPORT_TO;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_FILTERING_IDS;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_FILTERING_IDS_DELIMITER;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_INPUT_REPORT_COUNT;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_REPORTING_SITE;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.common.primitives.Longs;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Map;
import java.util.Optional;

/** Validates the job parameters are valid. */
public final class JobValidator {

  /**
   * validates the job parameters are valid.
   *
   * @param domainOptional if the output domain is optional. If not set, then output_domain path
   *     should be set.
   */
  public static void validate(Optional<Job> job, boolean domainOptional) {
    checkArgument(job.isPresent(), "Job metadata not found.");
    String jobKey = toJobKeyString(job.get().jobKey());
    validateReportingOriginAndSite(job.get());
    Map<String, String> jobParams = job.get().requestInfo().getJobParametersMap();
    checkArgument(
        domainOptional
            || (jobParams.containsKey(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME)
                && jobParams.containsKey(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX)
                && (!jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME).isEmpty()
                    || !jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX).isEmpty())),
        String.format(
            "Job parameters for the job '%s' does not have output domain location specified in"
                + " 'output_domain_bucket_name' and 'output_domain_blob_prefix' fields. Please"
                + " refer to the API documentation for output domain parameters at"
                + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md",
            jobKey));
    String reportErrorThreshold =
        jobParams.getOrDefault(JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, null);
    checkArgument(
        reportErrorThreshold == null || validPercentValue(reportErrorThreshold),
        String.format(
            "Job parameters for the job '%s' should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter.",
            jobKey));
    checkArgument(
        isAValidCount(jobParams.get(JOB_PARAM_INPUT_REPORT_COUNT)),
        String.format(
            "Job parameters for the job '%s' should have a valid non-negative value for"
                + " 'input_report_count' parameter.",
            jobKey));

    String filteringIds = jobParams.getOrDefault(JOB_PARAM_FILTERING_IDS, null);
    checkArgument(
        validStringOfIntegers(filteringIds, JOB_PARAM_FILTERING_IDS_DELIMITER),
        String.format(
            "Job parameters for the job '%s' should have comma separated integers for"
                + " 'filtering_ids' parameter.",
            jobKey));
  }

  /**
   * Validates that exactly one of the two fields 'JOB_PARAM_ATTRIBUTION_REPORT_TO' and
   * 'reporting_site' is specified and the specified field is non-empty
   */
  private static void validateReportingOriginAndSite(Job job) {
    Map<String, String> jobParams = job.requestInfo().getJobParametersMap();
    String jobKey = toJobKeyString(job.jobKey());
    boolean bothSiteAndOriginSpecified =
        jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && jobParams.containsKey(JOB_PARAM_REPORTING_SITE);
    boolean neitherSiteOrOriginSpecified =
        !jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && !jobParams.containsKey(JOB_PARAM_REPORTING_SITE);
    if (bothSiteAndOriginSpecified || neitherSiteOrOriginSpecified) {
      throw new IllegalArgumentException(
          String.format(
              "Exactly one of 'attribution_report_to' and 'reporting_site' fields should be"
                  + " specified for the Job %s. It is recommended to use 'reporting_site'"
                  + " parameter. Parameter 'attribution_report_to' will be deprecated in the next"
                  + " major version upgrade of the API",
              jobKey));
    }
    // Verify that either the field 'JOB_PARAM_ATTRIBUTION_REPORT_TO' is not specified or is
    // non-empty.
    boolean emptyAttributionReportToSpecified =
        jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && jobParams.get(JOB_PARAM_ATTRIBUTION_REPORT_TO).trim().isEmpty();
    checkArgument(
        !emptyAttributionReportToSpecified,
        String.format(
            "The 'attribution_report_to' field in the Job parameters is empty for" + " the Job %s.",
            jobKey));
    // Verify that either the field 'reporting_site' is not specified or is non-empty.
    boolean emptyReportingSiteSpecified =
        jobParams.containsKey(JOB_PARAM_REPORTING_SITE)
            && jobParams.get(JOB_PARAM_REPORTING_SITE).trim().isEmpty();
    checkArgument(
        !emptyReportingSiteSpecified,
        String.format(
            "The 'reporting_site' field in the Job parameters is empty for the Job" + " %s.",
            jobKey));
  }

  /** Checks if the string represents a non-negative number or is empty. */
  private static boolean isAValidCount(String countInString) {
    return countInString == null
        || countInString.trim().isEmpty()
        || (Longs.tryParse(countInString.trim()) != null
            && Longs.tryParse(countInString.trim()) >= 0);
  }

  /** Checks if the given string is a list of integers separated by delimiter. */
  private static boolean validStringOfIntegers(String stringOfNumbers, String delimiter) {
    try {
      NumericConversions.getUnsignedLongsFromString(stringOfNumbers, delimiter);
      return true;
    } catch (IllegalArgumentException iae) {
      return false;
    }
  }

  /** Validates that the string representation has a valid percentage value. */
  private static boolean validPercentValue(String percentageInString) {
    try {
      NumericConversions.getPercentageValue(percentageInString);
      return true;
    } catch (IllegalArgumentException iae) {
      return false;
    }
  }
}
