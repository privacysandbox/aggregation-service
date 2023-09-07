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

package com.google.aggregate.adtech.worker;

/** Return codes for aggregation worker. */
public enum AggregationWorkerReturnCode {
  /**
   * Unable to process the job because the user exhausted the allocated budget to aggregate the
   * reports in this batch. This can happen if some reports were already processed in an earlier
   * batch. This error is not transient and the job cannot be retried.
   */
  PRIVACY_BUDGET_EXHAUSTED,

  /**
   * Unable to process the reports because an error with the privacy budget service occurred. Note
   * that this is different from {@code PRIVACY_BUDGET_EXHAUSTED}. The error is not transient if the
   * failure was due to permission issue.
   */
  PRIVACY_BUDGET_ERROR,

  /**
   * The job had invalid configuration and could not be processed. This error is not transient and
   * job cannot be retried.
   */
  INVALID_JOB,

  /**
   * The Aggregation Service failed to write the summary report. The job cannot be retried if the
   * failure was due to permission issue
   */
  RESULT_WRITE_ERROR,

  /** Internal Error encountered. This error is transient and the job can be retried. */
  INTERNAL_ERROR,

  /**
   * Aggregation service did not have access to storage or other requested resources. This error is
   * not transient and the job cannot be retried.
   */
  PERMISSION_ERROR,

  /**
   * Aggregation service could not download the job input data from cloud storage, potentially due
   * to permission or file format issues or file was missing. This error is not transient and the
   * job cannot be retried.
   */
  INPUT_DATA_READ_FAILED,

  /** Aggregation Job completed successfully. */
  SUCCESS,

  /**
   * Aggregation Job completed but with errors. Some reports may have errors. Error details are
   * added to the response error summary.
   */
  SUCCESS_WITH_ERRORS,

  /**
   * Aggregation Job running on Debug Mode succeeded, but would have failed if running in non-debug
   * mode due to a privacy budget error.
   */
  DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR,

  /**
   * Aggregation Job running on Debug Mode succeeded, but would have failed if running in non-debug
   * mode due to privacy budget exhaustion.
   */
  DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED,

  /**
   * Aggregation Job failed early because the report error counts exceeded a threshold. The
   * threshold can be set in job.request_info.job_parameters for the key
   * "report_error_threshold_percentage".
   */
  REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD,

  /**
   * Aggregation service is not authenticated to call privacy budget service. This error is likely
   * caused by incorrect IAM policy or role setup. This error is not transient and the job cannot be
   * retried.
   */
  PRIVACY_BUDGET_AUTHENTICATION_ERROR,

  /**
   * Aggregation service is authenticated but unauthorized to call privacy budget service. This
   * error is likely caused by incorrect setup during the onboarding process or config changes in
   * privacy budget server. This error is not transient and the job cannot be retried.
   */
  PRIVACY_BUDGET_AUTHORIZATION_ERROR;

  /**
   * Convert the parameter failure code into the equivalent code for debug mode. Namely for privacy
   * budget.
   *
   * @param code
   * @return
   */
  public static AggregationWorkerReturnCode getDebugEquivalent(AggregationWorkerReturnCode code) {
    switch (code) {
      case PRIVACY_BUDGET_ERROR:
        return DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR;
      case PRIVACY_BUDGET_EXHAUSTED:
        return DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED;
      default:
        return code;
    }
  }
}
