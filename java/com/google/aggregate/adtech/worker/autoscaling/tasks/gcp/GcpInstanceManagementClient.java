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

package com.google.aggregate.adtech.worker.autoscaling.tasks.gcp;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.Autoscaler;
import com.google.cloud.compute.v1.GetRegionInstanceGroupManagerRequest;
import com.google.cloud.compute.v1.InstanceGroupManager;
import com.google.cloud.compute.v1.ListManagedInstancesRegionInstanceGroupManagersRequest;
import com.google.cloud.compute.v1.ManagedInstance.CurrentAction;
import com.google.cloud.compute.v1.Operation;
import com.google.cloud.compute.v1.RegionAutoscalersClient;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient.ListManagedInstancesPagedResponse;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersDeleteInstancesRequest;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/** A helper class to wrap calls to GCP instance group management and autoscaler APIs. */
public class GcpInstanceManagementClient {

  private final RegionInstanceGroupManagersClient instanceGroupManagerClient;
  private final RegionAutoscalersClient autoscalerClient;
  private final String projectId;
  private final String managedInstanceGroupId;
  private final String region;

  /** Creates a new instance of the {@code GcpInstanceManagementClient} class. */
  @Inject
  public GcpInstanceManagementClient(
      RegionInstanceGroupManagersClient instanceGroupManagerClient,
      RegionAutoscalersClient autoscalerClient,
      GcpInstanceManagementConfig instanceManagementConfig) {
    this.instanceGroupManagerClient = instanceGroupManagerClient;
    this.autoscalerClient = autoscalerClient;
    this.projectId = instanceManagementConfig.projectId();
    this.managedInstanceGroupId = instanceManagementConfig.managedInstanceGroupName();
    this.region = instanceManagementConfig.region();
  }

  /**
   * Returns the list of instances in the worker managed instance group that are not in the process
   * of deleting. An empty list is returned if there are none.
   */
  public List<GcpComputeInstance> listActiveInstanceGroupInstances() {
    ListManagedInstancesRegionInstanceGroupManagersRequest listManagedInstanceRequest =
        ListManagedInstancesRegionInstanceGroupManagersRequest.newBuilder()
            .setProject(projectId)
            .setRegion(region)
            .setInstanceGroupManager(managedInstanceGroupId)
            .setFilter("currentAction != " + CurrentAction.DELETING)
            .build();
    ListManagedInstancesPagedResponse listInstanceResponse =
        instanceGroupManagerClient.listManagedInstances(listManagedInstanceRequest);
    List<GcpComputeInstance> activeInstances = new ArrayList<>();
    listInstanceResponse
        .iterateAll()
        .forEach(
            e -> {
              activeInstances.add(
                  GcpComputeInstance.builder()
                      .setInstanceId(e.getInstance())
                      .setInstanceTemplate(e.getVersion().getInstanceTemplate())
                      .build());
            });
    return activeInstances;
  }

  /** Deletes the list of instances provided from the worker managed instance group. */
  public void deleteInstances(Set<String> instanceIdsToTerminate)
      throws InterruptedException, ExecutionException {
    RegionInstanceGroupManagersDeleteInstancesRequest deleteRegionInstanceGroupManagerRequest =
        RegionInstanceGroupManagersDeleteInstancesRequest.newBuilder()
            .addAllInstances(instanceIdsToTerminate)
            .build();
    OperationFuture<Operation, Operation> future =
        instanceGroupManagerClient.deleteInstancesAsync(
            projectId, region, managedInstanceGroupId, deleteRegionInstanceGroupManagerRequest);

    int maxRetries = 6;
    for (int retryCount = 0; retryCount <= maxRetries; retryCount++) {
      if (future.isDone()) {
        break;
      } else if (retryCount < maxRetries) {
        Thread.sleep((long) (1000L * Math.pow(2, retryCount + 1)));
      } else {
        throw new RuntimeException("Instance deletion did not complete");
      }
    }
  }

  /**
   * Returns the autoscaler for the worker managed instance group. An empty Optional is returned if
   * there is not an autoscaler associated with the instance group.
   */
  public Optional<Autoscaler> getAutoscaler() {
    InstanceGroupManager manager =
        instanceGroupManagerClient.get(projectId, region, managedInstanceGroupId);
    if (manager.hasStatus() && manager.getStatus().hasAutoscaler()) {
      String autoscalerUri = manager.getStatus().getAutoscaler();
      return Optional.of(
          autoscalerClient.get(
              projectId, region, autoscalerUri.substring(autoscalerUri.lastIndexOf("/") + 1)));
    }
    return Optional.empty();
  }

  /** Returns the current instance template for the managed instance group. */
  public String getCurrentInstanceTemplate() {
    GetRegionInstanceGroupManagerRequest getManagedInstanceRequest =
        GetRegionInstanceGroupManagerRequest.newBuilder()
            .setInstanceGroupManager(managedInstanceGroupId)
            .setProject(projectId)
            .setRegion(region)
            .build();
    return instanceGroupManagerClient.get(getManagedInstanceRequest).getInstanceTemplate();
  }
}
