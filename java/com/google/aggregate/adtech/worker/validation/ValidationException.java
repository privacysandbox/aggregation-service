/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.adtech.worker.validation;

import com.google.aggregate.adtech.worker.model.ErrorCounter;

/** Exception to wrap Validation related Exceptions. */
public class ValidationException extends RuntimeException {

  private ErrorCounter code;

  /** Builds a new ValidationException with error code string and specified message. */
  public ValidationException(ErrorCounter code, String message) {
    super(message);
    this.setCode(code);
  }

  public ErrorCounter getCode() {
    return code;
  }

  public void setCode(ErrorCounter code) {
    this.code = code;
  }
}
