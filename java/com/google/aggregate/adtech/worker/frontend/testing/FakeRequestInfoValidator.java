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

package com.google.aggregate.adtech.worker.frontend.testing;

import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;

/** Simple fake implementation of {@code RequestInfoValidator} for use in tests. */
public class FakeRequestInfoValidator implements RequestInfoValidator {
  private Optional<String> validateReturnValue;

  /** Returns a new instance of the {@code FakeRequestInfoValidator} class. */
  public FakeRequestInfoValidator() {
    validateReturnValue = Optional.empty();
  }

  @Override
  public Optional<String> validate(RequestInfo unused) {
    return validateReturnValue;
  }

  /** Sets the value to return from the {@code validate} method. */
  public void setValidateReturnValue(Optional<String> validateReturnValue) {
    this.validateReturnValue = validateReturnValue;
  }
}
