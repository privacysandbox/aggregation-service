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

import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.util.Optional;

/** Performs validation on requests to create jobs. */
public interface CreateJobRequestValidator {

  /**
   * Performs some validation on the {@code CreateJobRequest}. Optional will be empty if the
   * validation succeeds or will contain an error message validation fails.
   */
  Optional<String> validate(CreateJobRequest createJobRequest);
}
