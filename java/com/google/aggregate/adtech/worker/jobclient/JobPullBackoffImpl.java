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

/**
 * Backoff that blocks the main thread for 5 seconds before signaling to try again with SQS.
 *
 * <p>In reality, the SQS API should poll internally repeatedly, and this is mainly to keep the
 * compatibility with other pullers that can be used in the worker.
 */
public final class JobPullBackoffImpl implements JobPullBackoff {

  private static final Long THREAD_SLEEP_TIME_MILLIS = 5000L;

  /** Blocks the main thread for 5 seconds, then signals a retry. */
  @Override
  public Boolean get() {
    try {
      Thread.sleep(THREAD_SLEEP_TIME_MILLIS);
    } catch (InterruptedException e) {
      throw new BackoffException(e);
    }

    return true;
  }
}
