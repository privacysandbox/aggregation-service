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

package com.google.aggregate.adtech.worker.autoscaling.app.gcp;

import com.google.inject.Guice;
import com.google.inject.Injector;

/** Handles requests to scale in the worker instance group. */
public final class WorkerScaleInHttpFunction extends WorkerScaleInHttpFunctionBase {

  /** Creates a new instance of the {@code WorkerScaleInHttpFunction} class. */
  public WorkerScaleInHttpFunction() {
    this(Guice.createInjector(new WorkerScaleInModule()));
  }

  /** Creates a new instance of the {@code WorkerScaleInHttpFunction} class from injector. */
  public WorkerScaleInHttpFunction(Injector injector) {
    this(injector.getInstance(WorkerScaleInRequestHandler.class));
  }
  /** Creates a new instance of the {@code WorkerScaleInHttpFunction} class from request handler. */
  public WorkerScaleInHttpFunction(WorkerScaleInRequestHandler workerScaleInRequestHandler) {
    super(workerScaleInRequestHandler);
  }
}
