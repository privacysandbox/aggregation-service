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

package com.google.aggregate.adtech.worker.frontend.tasks.validation;

import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;

/** Validates that the jobRequestId is shorter or equal to the length limit. */
public final class JobRequestIdLengthValidator implements RequestInfoValidator {

  private static final int MAX_LENGTH = 128;

  @Override
  public Optional<String> validate(RequestInfo requestInfo) {
    if (requestInfo.getJobRequestId().length() <= MAX_LENGTH) {
      return Optional.empty();
    }

    return Optional.of(errorMessage(requestInfo.getJobRequestId().length()));
  }

  private String errorMessage(int jobRequestIdLength) {
    return String.format(
        "job_request_id is too long. Max length allowed is %d, length provided was %d",
        MAX_LENGTH, jobRequestIdLength);
  }
}
