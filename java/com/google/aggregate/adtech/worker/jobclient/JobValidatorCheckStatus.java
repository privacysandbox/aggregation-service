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
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Checks whether a job status is valid for processing.
 *
 * <p>Returns true if the job status is not FINISHED.
 */
public final class JobValidatorCheckStatus implements JobValidator {

  private static final Logger logger = Logger.getLogger(JobValidatorCheckStatus.class.getName());

  /** Validates that the received job is valid for processing. */
  @Override
  public boolean validate(Optional<Job> job, String jobKeyString) {
    logger.info(
        String.format("received job %s with status %s.", jobKeyString, job.get().jobStatus()));

    if (job.get().jobStatus() == JobStatus.FINISHED) {
      logger.warning(String.format("Job '%s' is already finished, nothing to do.", jobKeyString));
      return false;
    }
    return true;
  }

  @Override
  public String getDescription() {
    return "validator for checking whether received job is in valid status.";
  }

  @Override
  public String getValidationErrorMessage() {
    return "Job already finished.";
  }

  @Override
  public ReturnCode getValidationErrorReturnCode() {
    return ReturnCode.UNSPECIFIED_ERROR;
  }

  @Override
  public boolean reportValidationError() {
    // We currently report errors using the job metadata, if job is finished already, we do not want
    // to update the metadata entry.
    return false;
  }
}
