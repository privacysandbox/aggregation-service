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

/** Static utilities relating to Job. */
public final class JobUtils {

  public static final String JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX = "output_domain_blob_prefix";

  public static final String JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME = "output_domain_bucket_name";

  public static final String JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE =
      "report_error_threshold_percentage";

  public static final String JOB_PARAM_INPUT_REPORT_COUNT = "input_report_count";

  public static final String JOB_PARAM_FILTERING_IDS = "filtering_ids";

  public static final String JOB_PARAM_FILTERING_IDS_DELIMITER = ",";

  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";

  public static final String JOB_PARAM_REPORTING_SITE = "reporting_site";

  private JobUtils() {}
}
