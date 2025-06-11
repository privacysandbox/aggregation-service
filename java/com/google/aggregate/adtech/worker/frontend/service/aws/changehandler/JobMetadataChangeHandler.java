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

package com.google.aggregate.adtech.worker.frontend.service.aws.changehandler;

import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;

/**
 * JobMetadataChangeHandler processes changes to the JobMetadata objects. This can be made generic
 * later if the need arises.
 */
public interface JobMetadataChangeHandler {

  /** Returns true if the handler can handle the data. */
  boolean canHandle(JobMetadata data);

  /**
   * Executes the actual handler logic.
   *
   * @throws ChangeHandlerException (unchecked) if an exception occurs in handling
   */
  void handle(JobMetadata data);

  /**
   * Exception to be thrown by handler implementations. Extends {@link RuntimeException} since
   * handler failure should be fatal and the caller will be restarted to try the request again.
   */
  class ChangeHandlerException extends RuntimeException {
    ChangeHandlerException(Throwable e) {
      super(e);
    }
  }
}
