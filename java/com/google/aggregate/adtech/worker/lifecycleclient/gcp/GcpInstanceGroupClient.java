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

package com.google.aggregate.adtech.worker.lifecycleclient.gcp;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.Operation;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersDeleteInstancesRequest;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.Annotations.GcpInstanceUrl;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.Annotations.WorkerManagedInstanceGroupName;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpProjectId;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpZone;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/** Helper class for GCP managed instance group APIs for lifecycle client. */
public class GcpInstanceGroupClient {
  private final RegionInstanceGroupManagersClient instanceGroupManagerClient;
  private String projectId;
  private String managedInstanceGroupName;
  private String zone;
  private String instanceUrl;

  @Inject
  public GcpInstanceGroupClient(
      RegionInstanceGroupManagersClient instanceGroupManagersClient,
      @GcpProjectId String projectId,
      @WorkerManagedInstanceGroupName String managedInstanceGroupName,
      @GcpZone String zone,
      @GcpInstanceUrl String instanceUrl) {
    this.instanceGroupManagerClient = instanceGroupManagersClient;
    this.projectId = projectId;
    this.managedInstanceGroupName = managedInstanceGroupName;
    this.zone = zone;
    this.instanceUrl = instanceUrl;
  }

  /** Deletes the current worker instance from the worker managed instance group. */
  public void deleteInstance() throws InterruptedException, ExecutionException {
    String region = zone.substring(0, zone.lastIndexOf("-"));
    RegionInstanceGroupManagersDeleteInstancesRequest deleteRegionInstanceGroupManagerRequest =
        RegionInstanceGroupManagersDeleteInstancesRequest.newBuilder()
            .addAllInstances(Set.of(instanceUrl))
            .build();
    OperationFuture<Operation, Operation> future =
        instanceGroupManagerClient.deleteInstancesAsync(
            projectId, region, managedInstanceGroupName, deleteRegionInstanceGroupManagerRequest);

    int maxRetries = 6;
    for (int retryCount = 0; retryCount <= maxRetries; retryCount++) {
      if (future.isDone()) {
        Thread.sleep(5000L);
        break;
      } else if (retryCount < maxRetries) {
        Thread.sleep((long) (1000L * Math.pow(2, retryCount + 1)));
      } else {
        throw new RuntimeException("Instance deletion did not complete");
      }
    }
  }
}
