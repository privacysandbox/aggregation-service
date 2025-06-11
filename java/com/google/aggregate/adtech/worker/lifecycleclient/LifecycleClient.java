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

package com.google.aggregate.adtech.worker.lifecycleclient;

import java.util.Optional;

/** Interface for handling lifecycle of the cloud instance. */
public interface LifecycleClient {

  /**
   * Gets the Lifecycle state of the instance.
   *
   * @return lifecycle state of the instance
   * @throws LifecycleClientException
   */
  // TODO: Convert String to enum
  Optional<String> getLifecycleState() throws LifecycleClientException;

  /**
   * Handle scale-in lifecycle action of this cloud instance.
   *
   * @return boolean: True for successful scale-in. False, if there was no scale-in action
   */
  boolean handleScaleInLifecycleAction() throws LifecycleClientException;

  /** Represents an exception thrown by the {@code LifecycleClient} class. */
  final class LifecycleClientException extends Exception {

    /** Creates a new instance from a {@code Throwable}. */
    public LifecycleClientException(Throwable cause) {
      super(cause);
    }

    /** Creates a new instance from a String message. */
    public LifecycleClientException(String message) {
      super(message);
    }
  }
}
