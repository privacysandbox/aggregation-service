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

import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.scp.protos.shared.api.v1.ErrorResponseProto.Details;
import com.google.scp.protos.shared.api.v1.ErrorResponseProto.ErrorResponse;
import com.google.scp.shared.api.model.Code;
import java.util.List;

/** Contains an unknown error response used for testing. */
public final class UnknownErrorResponse {

  /** Gets an unknown error response. */
  public static ErrorResponse getUnknownErrorResponse() {
    return ErrorResponse.newBuilder()
        .setCode(Code.UNKNOWN.getRpcStatusCode())
        .setMessage(ErrorReasons.SERVER_ERROR.toString())
        .addAllDetails(
            List.of(Details.newBuilder().setReason(ErrorReasons.SERVER_ERROR.toString()).build()))
        .build();
  }
}
