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

package com.google.aggregate.adtech.worker;

import com.google.inject.AbstractModule;
import javax.inject.Singleton;

/** Abstract module class to be implemented for the various {@code ResultLogger} implementations */
public abstract class ResultLoggerModule extends AbstractModule {

  /** Returns the {@code Class} for the {@code ResultLogger} implementation the module will use. */
  public abstract Class<? extends ResultLogger> getResultLoggerImplementation();

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies for
   * the implementation.
   */
  public void configureModule() {}

  @Override
  protected final void configure() {
    bind(ResultLogger.class).to(getResultLoggerImplementation()).in(Singleton.class);
    configureModule();
  }
}
