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

package com.google.aggregate.adtech.worker.jobclient;

import com.google.aggregate.adtech.worker.jobclient.model.ErrorReason;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.model.JobRetryRequest;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;

import java.util.Optional;

/** Interface for handling aggregation jobs */
public interface JobClient {

  /**
   * Blocking call to receive the next aggregation job item.
   *
   * @return an {@code Optional} of a {@code Job}. Will be empty if no job is available and the
   *     {@code JobPullBackoff} mechanism decides to terminate polling.
   */
  Optional<Job> getJob() throws JobClientException;

  /**
   * Releases a held job after a provided delay so that it may be picked up by another worker. The
   * maximum delay that can be set is 10 minutes.
   *
   * @param jobRetryRequest JobRetryRequest of the job to release.
   */
  void returnJobForRetry(JobRetryRequest jobRetryRequest) throws JobClientException;

  /**
   * Marks aggregation work completed either successfully or with a non-retryable error, by updating
   * the job queue and metadata db.
   *
   * @param jobResult
   */
  void markJobCompleted(JobResult jobResult) throws JobClientException;

  /**
   * Appends error message to a Job's ErrorSummary field within its ResultInfo.
   *
   * @param jobKey JobKey of the Job to append to
   * @param error Error message to append to ErrorSummary
   * @throws JobClientException When the Job's metadata cannot be accessed, if the Job is not
   *     IN_PROGRESS, or if the error message cannot be saved
   */
  void appendJobErrorMessage(JobKey jobKey, String error) throws JobClientException;

  /** Represents an exception thrown by the {@code JobClient} class. */
  class JobClientException extends Exception {

    /** Error reason for this exception. */
    public final ErrorReason reason;

    /** Creates a new instance of the {@code JobClientException} class. */
    public JobClientException(Throwable cause, ErrorReason reason) {
      super(cause);
      this.reason = reason;
    }

    /** Creates a new instance of the {@code JobClientException} class. */
    public JobClientException(String message, ErrorReason reason) {
      super(message);
      this.reason = reason;
    }

    /** Returns {@code ErrorReason} for the exception */
    public ErrorReason getReason() {
      return reason;
    }

    /** String representation of the {@code JobClientException} class. */
    @Override
    public String toString() {
      return String.format("ERROR: %s\n%s", reason, super.toString());
    }
  }
}
