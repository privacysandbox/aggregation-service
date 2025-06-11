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

package com.google.aggregate.adtech.worker.lifecycleclient.local;

import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;

import java.util.Optional;

/** Lifecycle client for local testing. */
public final class LocalLifecycleClient implements LifecycleClient {

  @Override
  public Optional<String> getLifecycleState() {
    return Optional.empty();
  }

  @Override
  public boolean handleScaleInLifecycleAction() {
    return false;
  }
}
