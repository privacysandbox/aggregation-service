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

package com.google.aggregate.adtech.worker.frontend.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;

/** Exception thrown by an implementor of JsonSerializerFacade. */
public final class JsonSerializerFacadeException extends Exception {

  /** Creates a new instance of the {@code JsonSerializerFacadeException} class. */
  public JsonSerializerFacadeException(Throwable cause) {
    super(cause);
  }

  @Override
  public String getMessage() {
    String message;
    String location = null;
    Throwable cause = this.getCause();
    if (cause instanceof JsonProcessingException) {
      if (cause.getCause() != null) {
        message = cause.getCause().getMessage();
      } else {
        message = ((JsonProcessingException) cause).getOriginalMessage();
      }
      location = "\r\n at " + ((JsonProcessingException) cause).getLocation();
    }
    // IllegalArgumentException thrown when null
    else {
      message = cause.getMessage();
    }
    return String.format("%s%s", message, location);
  }
}
