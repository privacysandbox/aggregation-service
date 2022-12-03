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
   * Unable to process job because the privacy budget was exhausted.
   *
   * <p>Note: this does not mean an error with privacy budget service occurred. The privacy budget
   * service was able to handle the request but budget was not available.
   */
  PRIVACY_BUDGET_EXHAUSTED,

  /**
   * Unable to process the reports because an error with the privacy budget service occurred. Note
   * that this is different from {@code PRIVACY_BUDGET_EXHAUSTED}.
   */
  PRIVACY_BUDGET_ERROR,

  /** The job had invalid configuration and could not be processed. */
  INVALID_JOB,

  /** Error encountered while logging result. */
  RESULT_LOGGING_ERROR,

  /** Error encountered while processing domain. */
  DOMAIN_PROCESS_EXCEPTION,

  /** A permission issue occurred and the job couldn't be processed. */
  PERMISSION_ERROR,

  /** Report or Domain input data ead filed. */
  INPUT_DATA_READ_FAILED,

  /** Aggregation Job completed successfully. */
  SUCCESS
}
