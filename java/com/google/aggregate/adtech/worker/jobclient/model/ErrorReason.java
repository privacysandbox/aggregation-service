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

package com.google.aggregate.adtech.worker.jobclient.model;

/** Error reasons for job client. */
public enum ErrorReason {
  // Could not pull new job from the queue.
  JOB_PULL_FAILED,
  // Could not release the job back to the queue for other workers to pick up.
  RETURN_JOB_FOR_RETRY_FAILED,
  // Could not retrieve the job queue receipt handle from job cache, in order to marking job
  // completed.
  JOB_RECEIPT_HANDLE_NOT_FOUND,
  // Could not retrieve the job metadata entry.
  JOB_METADATA_NOT_FOUND,
  // The job metadata indicates a job status which is different from what expected.
  WRONG_JOB_STATUS,
  // Encountered an error when trying to mark job completed.
  JOB_MARK_COMPLETION_FAILED,
  // Could not update ErrorSummary by appending an error message.
  JOB_ERROR_SUMMARY_UPDATE_FAILED,
  // The delay was out of range
  JOB_DELAY_OUT_OF_RANGE,
  // An unspecified fatal error occurred.
  UNSPECIFIED_ERROR
}
