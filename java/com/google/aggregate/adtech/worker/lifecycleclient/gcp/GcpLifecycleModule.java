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

import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleModule;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.Annotations.GcpInstanceUrl;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.Annotations.WorkerManagedInstanceGroupName;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpInstanceName;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpProjectId;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpZone;

import static com.google.scp.shared.clients.configclient.model.WorkerParameter.WORKER_MANAGED_INSTANCE_GROUP_NAME;

/** Defines dependencies to be used in the GCP Lifecycle Client. */
public final class GcpLifecycleModule extends LifecycleModule {

  /**
   * Gets the GCP implementation of {@code LifecycleClient}.
   *
   * @return GcpLifecycleClient
   */
  @Override
  public Class<? extends LifecycleClient> getLifecycleClientImpl() {
    return GcpLifecycleClient.class;
  }

  /** Provides the region instance group managers client. */
  @Provides
  @Singleton
  public RegionInstanceGroupManagersClient getRegionInstanceGroupManagersClient() throws Exception {
    return RegionInstanceGroupManagersClient.create();
  }

  /** Provides the worker managed instance group name. */
  @Provides
  @WorkerManagedInstanceGroupName
  public String provideManagedInstanceGroupName(ParameterClient parameterClient)
      throws ParameterClientException {
    return parameterClient.getParameter(WORKER_MANAGED_INSTANCE_GROUP_NAME.name()).orElse("");
  }

  /** Provides the GCP Instance URL. */
  @Provides
  @GcpInstanceUrl
  public String provideInstanceUrl(
      @GcpInstanceName String instanceName, @GcpZone String zone, @GcpProjectId String projectId) {
    return String.format(
        "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
        projectId, zone, instanceName);
  }
}
