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

package com.google.aggregate.adtech.worker.exceptions;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;

/** Exception to wrap Aggregation. */
public class AggregationJobProcessException extends Exception {

  private AggregationWorkerReturnCode code;

  /** Builds a new AggregationJobProcessException with error code string and specified message. */
  public AggregationJobProcessException(AggregationWorkerReturnCode code, String message) {
    super(message);
    this.setCode(code);
  }

  /** Builds a new AggregationJobProcessException with error code, cause and message. */
  public AggregationJobProcessException(
      AggregationWorkerReturnCode code, String message, Throwable cause) {
    super(message, cause);
    this.setCode(code);
  }

  public AggregationWorkerReturnCode getCode() {
    return code;
  }

  public void setCode(AggregationWorkerReturnCode code) {
    this.code = code;
  }
}
