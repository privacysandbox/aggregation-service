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

package com.google.aggregate.adtech.worker.testing;

import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.ResultLoggerModule;
import javax.inject.Singleton;

/** {@code ResultLoggerModule} to be used in testing. */
public final class InMemoryResultLoggerModule extends ResultLoggerModule {

  @Override
  public Class<? extends ResultLogger> getResultLoggerImplementation() {
    return InMemoryResultLogger.class;
  }

  @Override
  public void configureModule() {
    bind(InMemoryResultLogger.class).in(Singleton.class);
  }
}
