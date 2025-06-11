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

import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode;

import java.util.Optional;
import java.util.logging.Logger;

/** Checks the received job has not exhausted its retries. */
public final class JobValidatorCheckRetryLimit implements JobValidator {

  private static final Logger logger =
      Logger.getLogger(JobValidatorCheckRetryLimit.class.getName());

  private final int maxNumAttempts;

  /** Creates a new instance of the {@code JobValidatorCheckRetryLimit} class. */
  public JobValidatorCheckRetryLimit(int maxNumAttempts) {
    this.maxNumAttempts = maxNumAttempts;
  }

  /**
   * Validates received job has not exhausted its number of retries.
   *
   * <p>If a job is picked up by a worker for processing but the worker crashes mid processing, the
   * job goes back to the queue and is picked up by another worker. We would like to limit the
   * number of times a job is attempted for processing. This validator checks that the job has not
   * already exhausted its processing attempts.
   */
  @Override
  public boolean validate(Optional<Job> job, String jobKeyString) {
    int numPrevAttempts = job.get().numAttempts();

    logger.info(
        String.format(
            "received job %s with status %s. The job has been last updated on %s."
                + " It has been attempted processing %d times, and the limit on"
                + " number of attempts is %d.",
            jobKeyString,
            job.get().jobStatus(),
            job.get().updateTime(),
            numPrevAttempts,
            maxNumAttempts));

    return numPrevAttempts < maxNumAttempts;
  }

  @Override
  public String getDescription() {
    return "validator for checking received job has not exhausted its number of retries.";
  }

  @Override
  public String getValidationErrorMessage() {
    return String.format(
        "Number of retry attempts exhausted, the job failed to process after %d tries.",
        maxNumAttempts);
  }

  @Override
  public ReturnCode getValidationErrorReturnCode() {
    return ReturnCode.RETRIES_EXHAUSTED;
  }

  @Override
  public boolean reportValidationError() {
    // The job has failed to process after multiple retries.
    return true;
  }
}
