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

/**
 * Responsible for performing validation on a job
 *
 * <p>performs high level checks on the received job to determine whether the job does not need to
 * be processed by the worker and can be dropped.
 */
public interface JobValidator {

  /** Performs validation on a received job. */
  boolean validate(Optional<Job> job, String jobKeyString);

  /** Returns a short description of what the validator does. */
  String getDescription();

  /** Returns a short error message for reporting back to user. */
  String getValidationErrorMessage();

  /** Returns the error {@code ReturnCode} for reporting back to user. */
  ReturnCode getValidationErrorReturnCode();

  /** Whether a validation error should be reported back to user. */
  boolean reportValidationError();
}
