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

import com.google.common.collect.ImmutableMap;
import com.google.scp.shared.api.model.HttpMethod;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandler;
import com.google.scp.shared.gcp.util.CloudFunctionServiceBase;

import java.util.regex.Pattern;

import static com.google.scp.shared.api.model.HttpMethod.POST;

/** Base class for worker scale in http function */
public class WorkerScaleInHttpFunctionBase extends CloudFunctionServiceBase {

  private static final Pattern manageInstancesUrlPattern =
      Pattern.compile("/manageInstances", Pattern.CASE_INSENSITIVE);

  private final WorkerScaleInRequestHandler workerScaleInRequestHandler;

  /** Creates a new instance of the {@code WorkerScaleInHttpFunction} class. */
  public WorkerScaleInHttpFunctionBase(WorkerScaleInRequestHandler workerScaleInRequestHandler) {
    this.workerScaleInRequestHandler = workerScaleInRequestHandler;
  }

  @Override
  protected ImmutableMap<HttpMethod, ImmutableMap<Pattern, CloudFunctionRequestHandler>>
      getRequestHandlerMap() {
    return ImmutableMap.of(
        POST, ImmutableMap.of(manageInstancesUrlPattern, this.workerScaleInRequestHandler));
  }
}
