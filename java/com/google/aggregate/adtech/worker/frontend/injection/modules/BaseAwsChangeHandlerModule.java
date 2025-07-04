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

package com.google.aggregate.adtech.worker.frontend.injection.modules;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;

/**
 * Base for the {@code AwsChangeHandlerModule} which registers instances of the {@code
 * JobMetadataChangeHandler} class.
 */
public abstract class BaseAwsChangeHandlerModule extends AbstractModule {

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  protected void configureModule() {}

  /** Gets the change handlers for this module. */
  public abstract ImmutableList<Class<? extends JobMetadataChangeHandler>> getChangeHandlerImpls();

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    Multibinder<JobMetadataChangeHandler> multibinder =
        Multibinder.newSetBinder(binder(), JobMetadataChangeHandler.class);
    getChangeHandlerImpls().forEach(handler -> multibinder.addBinding().to(handler));

    configureModule();
  }
}
