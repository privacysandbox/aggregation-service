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

import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Base class for tasks to create a job. */
public abstract class CreateJobTaskBase implements CreateJobTask {

  protected final Set<RequestInfoValidator> requestInfoValidators;

  /** Creates a new instance of the {@code CreateJobTaskBase} class. */
  public CreateJobTaskBase(Set<RequestInfoValidator> requestInfoValidators) {
    this.requestInfoValidators = requestInfoValidators;
  }

  /** Creates a job. */
  public abstract void createJob(RequestInfo requestInfo) throws ServiceException;

  /** Validates a job request. */
  protected void validate(RequestInfo requestInfo) throws ServiceException {
    String validationError =
        requestInfoValidators.stream()
            .map(requestInfoValidator -> requestInfoValidator.validate(requestInfo))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining("\n"));

    if (!validationError.isEmpty()) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT, ErrorReasons.VALIDATION_FAILED.toString(), validationError);
    }
  }
}
