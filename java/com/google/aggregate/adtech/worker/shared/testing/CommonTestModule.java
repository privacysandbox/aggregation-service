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

package com.google.aggregate.adtech.worker.shared.testing;

import com.google.inject.AbstractModule;

import java.time.Clock;

/** Contains bindings for common components used for testing. */
public final class CommonTestModule extends AbstractModule {
  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    bind(Clock.class).to(FakeClock.class);
  }
}
