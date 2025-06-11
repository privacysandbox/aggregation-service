/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.service.model;

/** Constants used by frontend service. */
public final class Constants {

  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";
  public static final String JOB_PARAM_REPORTING_SITE = "reporting_site";

  public static final String JOB_PARAM_INPUT_DATA_BLOB_PREFIXES = "input_data_blob_prefixes";
  public static final String JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX = "output_domain_blob_prefix";
  public static final String JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME = "output_domain_bucket_name";
  public static final String JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT = "debug_privacy_budget_limit";
}
