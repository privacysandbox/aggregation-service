/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.adtech.worker.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.errorprone.annotations.Var;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Static utilities relating to Job. */
public final class JobUtils {

  private static Logger logger = LoggerFactory.getLogger(JobUtils.class);

  public static final String JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX = "output_domain_blob_prefix";

  public static final String JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME = "output_domain_bucket_name";

  public static final String JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE =
      "report_error_threshold_percentage";

  public static final String JOB_PARAM_INPUT_REPORT_COUNT = "input_report_count";

  public static final String JOB_PARAM_FILTERING_IDS = "filtering_ids";

  public static final String JOB_PARAM_FILTERING_IDS_DELIMITER = ",";

  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";

  public static final String JOB_PARAM_REPORTING_SITE = "reporting_site";

  public static final String JOB_PARAM_DEBUG_PRIVACY_EPSILON = "debug_privacy_epsilon";

  private static final UnsignedLong FILTERING_ID_DEFAULT = UnsignedLong.ZERO;

  /**
   * Returns the filtering IDs from the job.
   *
   * <p>If the filtering IDs are not set, returns a set containing only 0.
   */
  public static ImmutableSet<UnsignedLong> getFilteringIdsFromJobOrDefault(Job job) {
    @Var
    ImmutableSet<UnsignedLong> filteringIds =
        NumericConversions.getUnsignedLongsFromString(
            job.requestInfo().getJobParametersMap().get(JOB_PARAM_FILTERING_IDS),
            JOB_PARAM_FILTERING_IDS_DELIMITER);

    if (filteringIds.isEmpty()) {
      filteringIds = ImmutableSet.of(FILTERING_ID_DEFAULT);
    }

    return filteringIds;
  }

  /** Gets the input report count from the job request if provided. */
  public static Optional<Long> getInputReportCountFromJobParams(Map<String, String> jobParams) {
    String inputReportCount = jobParams.get(JobUtils.JOB_PARAM_INPUT_REPORT_COUNT);
    if (inputReportCount == null || inputReportCount.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(Long.parseLong(inputReportCount.trim()));
  }

  /**
   * Gets the report error threshold from the job request otherwise returns the default set for the
   * job.
   */
  public static double getReportErrorThresholdPercentage(
      Map<String, String> jobParams, double defaultReportErrorThresholdPercentage) {
    String jobParamsReportErrorThresholdPercentage =
        jobParams.getOrDefault(JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, null);
    if (jobParamsReportErrorThresholdPercentage != null) {
      return NumericConversions.getPercentageValue(jobParamsReportErrorThresholdPercentage);
    }
    logger.info(
        String.format(
            "Job parameters didn't have a report error threshold configured. Taking the default"
                + " percentage value %f",
            defaultReportErrorThresholdPercentage));
    return defaultReportErrorThresholdPercentage;
  }

  private JobUtils() {}
}
