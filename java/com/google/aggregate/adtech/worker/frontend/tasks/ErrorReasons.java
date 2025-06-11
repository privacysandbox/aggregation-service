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

/** Reasons why a ServiceException was thrown. */
public enum ErrorReasons {
  /** There was an error with JSON posted to the server. */
  JSON_ERROR,

  /** There was an internal server error. */
  SERVER_ERROR,

  /** There was a validation error. */
  VALIDATION_FAILED,

  /** The job key already exists. */
  DUPLICATE_JOB_KEY,

  /** A required argument is missing. */
  ARGUMENT_MISSING,

  /** The requested job was not found. */
  JOB_NOT_FOUND
}
