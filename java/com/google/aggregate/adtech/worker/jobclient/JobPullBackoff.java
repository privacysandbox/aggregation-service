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

package com.google.aggregate.adtech.worker.jobclient;

import java.util.function.Supplier;

/**
 * Interface for backing off when nothing can be pulled from the job queue.
 *
 * <p>This should supply true if the worker should keep pulling, or false to abort. Additionally,
 * this supplier can block for some time before producing an answer. For example, if the supplier
 * blocks for 3 seconds, and then returns true, that means that the worker will spend 3 seconds
 * waiting for this decision, get a positive decision, and then move on with pulling again.
 */
public interface JobPullBackoff extends Supplier<Boolean> {

  /** Represents an exception thrown by the {@code JobPullBackoff} class. */
  final class BackoffException extends RuntimeException {

    /** Creates a new instance of the {@code BackoffException} class. */
    public BackoffException(Throwable cause) {
      super(cause);
    }
  }
}
