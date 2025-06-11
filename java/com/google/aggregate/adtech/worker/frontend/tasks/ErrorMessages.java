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

package com.google.aggregate.adtech.worker.frontend.tasks;

/** Contains error messages for the frontend. */
public final class ErrorMessages {

  /** Error message to display when a job is not found. */
  public static final String JOB_NOT_FOUND_MESSAGE =
      "Job with job_request_id '%s' could not be found";

  /**
   * Error message to display when a job is not found, with certain characters escaped.
   *
   * <p>{@link #JOB_NOT_FOUND_MESSAGE} is converted to this format during proto serialization to
   * JSON. Certain special characters are escaped for security reasons.
   */
  public static final String JOB_NOT_FOUND_HTML_ESCAPED_MESSAGE =
      "Job with job_request_id \\u0027%s\\u0027 could not be found";

  /** Error message to display when a duplicate job message is submitted. */
  public static final String DUPLICATE_JOB_MESSAGE =
      "Duplicate job_request_id provided: job_request_id=%s is not unique.";

  /** Error message to display when there is an issue with the database. */
  public static final String DB_ERROR_MESSAGE = "Internal error occurred when reaching the DB";
}
