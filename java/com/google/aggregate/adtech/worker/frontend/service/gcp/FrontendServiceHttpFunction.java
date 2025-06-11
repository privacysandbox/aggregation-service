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

package com.google.aggregate.adtech.worker.frontend.service.gcp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.aggregate.adtech.worker.frontend.service.gcp.GcpFrontendServiceModule.FrontendServiceVersionBinding;

/** Handles requests to FrontendService and returns HTTP Response. */
public final class FrontendServiceHttpFunction extends FrontendServiceHttpFunctionBase {

  /** Creates a new instance of the {@code FrontendServiceHttpFunction} class. */
  public FrontendServiceHttpFunction() {
    this(Guice.createInjector(new GcpFrontendServiceModule()));
  }

  /**
   * Creates a new instance of the {@code FrontendServiceHttpFunction} class with the given {@link
   * Injector}.
   */
  public FrontendServiceHttpFunction(Injector injector) {
    this(
        injector.getInstance(CreateJobRequestHandler.class),
        injector.getInstance(GetJobRequestHandler.class),
        injector.getInstance(PutJobRequestHandler.class),
        injector.getInstance(GetJobByIdRequestHandler.class),
        injector.getInstance(Key.get(Integer.class, FrontendServiceVersionBinding.class)));
  }

  /**
   * Creates a new instance of the {@code FrontendServiceHttpFunction} class with the given {@link
   * CreateJobRequestHandler}, {@link GetJobRequestHandler}, {@link PutJobRequestHandler}, {@link
   * GetJobByIdRequestHandler} and version.
   */
  public FrontendServiceHttpFunction(
      CreateJobRequestHandler createJobRequestHandler,
      GetJobRequestHandler getJobRequestHandler,
      PutJobRequestHandler putJobRequestHandler,
      GetJobByIdRequestHandler getJobByIdRequestHandler,
      int version) {
    super(
        createJobRequestHandler,
        getJobRequestHandler,
        putJobRequestHandler,
        getJobByIdRequestHandler,
        version);
  }
}
