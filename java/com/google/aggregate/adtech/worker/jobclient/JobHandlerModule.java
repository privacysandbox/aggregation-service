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

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Job handler module to get the next job, and record work completion. */
public abstract class JobHandlerModule extends AbstractModule {

  /** Returns a {@code Class} object for the job client implementation */
  public abstract Class<? extends JobClient> getJobClientImpl();

  /**
   * Arbitrary Guice configurations done by the module implementation (not including job puller
   * binding and backoff binding)
   */
  public void customConfigure() {}

  /**
   * Configures injected dependencies for this module. Includes a binding for {@code JobClient} in
   * addition to any bindings in the {@code customConfigure} method.
   */
  @Override
  protected final void configure() {
    bind(JobClient.class).to(getJobClientImpl());
    customConfigure();
  }

  /** The maximum number of times a job can be attempted. */
  @BindingAnnotation
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface JobClientJobMaxNumAttemptsBinding {}

  /** Binding annotation for the list of job validators. */
  @BindingAnnotation
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface JobClientJobValidatorsBinding {}
}
