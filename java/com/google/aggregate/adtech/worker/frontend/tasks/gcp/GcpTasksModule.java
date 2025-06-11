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

package com.google.aggregate.adtech.worker.frontend.tasks.gcp;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTask;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.JobRequestIdCharactersValidator;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.JobRequestIdLengthValidator;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import java.time.Clock;

/** Defines dependencies for GCP implementation of FrontendService Tasks. */
public final class GcpTasksModule extends AbstractModule {

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    bind(CreateJobTask.class).to(GcpCreateJobTask.class);
    bindValidators();
  }

  /** Provides an instance of the {@code Clock} class. */
  @Provides
  @Singleton
  public Clock provideClock() {
    return Clock.systemUTC();
  }

  private void bindValidators() {
    Multibinder<RequestInfoValidator> requestInfoValidatorMultibinder =
        Multibinder.newSetBinder(binder(), RequestInfoValidator.class);

    requestInfoValidatorMultibinder.addBinding().to(JobRequestIdCharactersValidator.class);
    requestInfoValidatorMultibinder.addBinding().to(JobRequestIdLengthValidator.class);
  }
}
