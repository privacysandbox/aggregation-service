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

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.GcpComputeInstance;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.ManageTerminatingWaitInstancesTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.RequestScaleInTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.RequestUpdateTask;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandlerBase;

import java.util.List;
import java.util.Map;

/** Handles requests to WorkerScaleIn Http Cloud Function and returns HTTP Response. */
public class WorkerScaleInRequestHandler extends CloudFunctionRequestHandlerBase<String, String> {

  private final ManageTerminatingWaitInstancesTask manageTerminatingWaitInstancesTask;
  private final RequestScaleInTask requestScaleInTask;
  private final RequestUpdateTask requestUpdateTask;

  /** Creates a new instance of the {@code WorkerScaleInHandler} class. */
  @Inject
  public WorkerScaleInRequestHandler() {
    Injector injector = Guice.createInjector(new WorkerScaleInModule());
    this.manageTerminatingWaitInstancesTask =
        injector.getInstance(ManageTerminatingWaitInstancesTask.class);
    this.requestScaleInTask = injector.getInstance(RequestScaleInTask.class);
    this.requestUpdateTask = injector.getInstance(RequestUpdateTask.class);
  }

  /** Constructor for testing. */
  public WorkerScaleInRequestHandler(
      ManageTerminatingWaitInstancesTask manageTerminatedInstanceTask,
      RequestScaleInTask requestScaleInTask,
      RequestUpdateTask requestUpdateTask) {
    this.manageTerminatingWaitInstancesTask = manageTerminatedInstanceTask;
    this.requestScaleInTask = requestScaleInTask;
    this.requestUpdateTask = requestUpdateTask;
  }

  @Override
  protected String toRequest(HttpRequest httpRequest) throws ServiceException {
    return "";
  }

  @Override
  protected String processRequest(String request) throws ServiceException {
    Map<String, List<GcpComputeInstance>> remainingInstances =
        manageTerminatingWaitInstancesTask.manageInstances();
    remainingInstances = requestUpdateTask.requestUpdate(remainingInstances);
    requestScaleInTask.requestScaleIn(remainingInstances);
    return "";
  }

  @Override
  protected void toCloudFunctionResponse(HttpResponse httpResponse, String response) {
    return;
  }
}
