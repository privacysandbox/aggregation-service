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

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

public final class AggregationWorker {

  private Injector injector;

  /** Creates a {@code ServiceManager} to run the aggregation worker */
  public ServiceManager createServiceManager() {
    return injector.getInstance(
        Key.get(ServiceManager.class, Annotations.WorkerServiceManager.class));
  }

  /**
   * Creates an aggregation worker from a Guice module that provides all the component wirings
   *
   * @param module Guice module that provides bindings for the worker.
   */
  public static AggregationWorker fromModule(Module module) {
    return new AggregationWorker(Guice.createInjector(module));
  }

  public Injector getInjector() {
    return injector;
  }

  private AggregationWorker(Injector injector) {
    this.injector = injector;
  }
}
