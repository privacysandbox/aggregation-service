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

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.UNSUPPORTED_REPORT_VERSION;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import java.security.AccessControlException;

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

  /**
   * Creates a {@link AggregationJobProcessException} corresponding to the {@code
   * ValidationException}.
   */
  public static AggregationJobProcessException createFromValidationException(
      ValidationException validationException) {
    AggregationWorkerReturnCode aggregationWorkerReturnCode;
    switch (validationException.getCode()) {
      case UNSUPPORTED_SHAREDINFO_VERSION:
        aggregationWorkerReturnCode = UNSUPPORTED_REPORT_VERSION;
        break;
      default:
        return new AggregationJobProcessException(
            INVALID_JOB, "Error due to validation exception.", validationException);
    }
    return new AggregationJobProcessException(
        aggregationWorkerReturnCode, validationException.getMessage(), validationException);
  }

  /**
   * Creates a {@link AggregationJobProcessException} corresponding to the {@code RuntimeException}.
   *
   * <p>If there is no suitable AggregationJobProcessException, then the original runtimeException
   * is rethrown.
   */
  public static AggregationJobProcessException createFromRuntimeException(
      RuntimeException runtimeException) {
    if (isSameExceptionClass(runtimeException, ResultLogException.class)) {
      return new AggregationJobProcessException(
          AggregationWorkerReturnCode.RESULT_WRITE_ERROR,
          "Exception occurred while writing result.",
          runtimeException);
    } else if (isSameExceptionClass(runtimeException, AccessControlException.class)) {
      return new AggregationJobProcessException(
          AggregationWorkerReturnCode.PERMISSION_ERROR,
          "Exception because of missing permission.",
          runtimeException);
    } else if (isSameExceptionClass(runtimeException, InternalServerException.class)) {
      return new AggregationJobProcessException(
          AggregationWorkerReturnCode.INTERNAL_ERROR,
          "Internal Service Exception when processing reports.",
          runtimeException);
    } else if (isSameExceptionClass(runtimeException, ValidationException.class)) {
      return AggregationJobProcessException.createFromValidationException(
          (ValidationException) runtimeException);
    } else if (isSameExceptionClass(runtimeException, ConcurrentShardReadException.class)) {
      return new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "Exception while reading reports input data.", runtimeException);
    } else if (isSameExceptionClass(runtimeException, DomainReadException.class)) {
      return new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "Exception while reading domain input data.", runtimeException);
    }
    throw runtimeException;
  }

  public AggregationWorkerReturnCode getCode() {
    return code;
  }

  public void setCode(AggregationWorkerReturnCode code) {
    this.code = code;
  }

  private static boolean isSameExceptionClass(
      RuntimeException givenException, Class<? extends RuntimeException> toCompareException) {
    return givenException.getClass().equals(toCompareException);
  }
}
