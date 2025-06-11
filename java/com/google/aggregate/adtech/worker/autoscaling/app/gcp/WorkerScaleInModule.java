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

import com.google.cloud.compute.v1.RegionAutoscalersClient;
import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.Annotations.TerminationWaitTimeout;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.GcpInstanceManagementConfig;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerAsgInstancesDao.AsgInstancesDbSpannerTtlDays;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbConfig;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbModule;

import java.time.Clock;
import java.util.Map;

/**
 * Defines dependencies to be used with WorkerScaleInHandler for managing scale in of the worker
 * instance group.
 */
public final class WorkerScaleInModule extends AbstractModule {
  private static final String PROJECT_ID_ENV_VAR = "PROJECT_ID";
  private static final String SPANNER_INSTANCE_ID_ENV_VAR = "SPANNER_INSTANCE_ID";
  private static final String SPANNER_DB_ID_ENV_VAR = "SPANNER_DATABASE_ID";
  private static final String MANAGED_INSTANCE_GROUP_NAME = "MANAGED_INSTANCE_GROUP_NAME";
  private static final String REGION = "REGION";
  private static final String TERMINATION_WAIT_TIMEOUT = "TERMINATION_WAIT_TIMEOUT";
  private static final String ASG_INSTANCES_TTL_ENV_VAR = "ASG_INSTANCES_TTL";

  Map<String, String> env = System.getenv();

  private String getProjectId() {
    return env.get(PROJECT_ID_ENV_VAR);
  }

  private String getRegion() {
    return env.get(REGION);
  }

  private String getManagedInstanceGroupName() {
    return env.get(MANAGED_INSTANCE_GROUP_NAME);
  }

  /** Termination wait timeout in seconds. */
  @Provides
  @TerminationWaitTimeout
  public Integer getTerminationWaitTimeout() {
    return Integer.parseInt(env.get(TERMINATION_WAIT_TIMEOUT));
  }

  /** Client to call the Regional Instance Group Managers APIs. */
  @Provides
  @Singleton
  public RegionInstanceGroupManagersClient getRegionInstanceGroupManagersClient() throws Exception {
    return RegionInstanceGroupManagersClient.create();
  }

  /** Client to call the Regional Autoscaler APIs. */
  @Provides
  @Singleton
  public RegionAutoscalersClient getRegionAutoscalersClient() throws Exception {
    return RegionAutoscalersClient.create();
  }

  /** TTL in days for AsgInstances table records. */
  @Provides
  @AsgInstancesDbSpannerTtlDays
  public Integer getAsgInstancesTtl() {
    return Integer.parseInt(env.getOrDefault(ASG_INSTANCES_TTL_ENV_VAR, "365"));
  }

  /** Provides an instance of the {@code Clock} class. */
  @Provides
  @Singleton
  public Clock provideClock() {
    return Clock.systemUTC();
  }

  @Override
  protected void configure() {
    bind(SpannerMetadataDbConfig.class)
        .toInstance(
            SpannerMetadataDbConfig.builder()
                .setGcpProjectId(getProjectId())
                .setSpannerInstanceId(env.get(SPANNER_INSTANCE_ID_ENV_VAR))
                .setSpannerDbName(env.get(SPANNER_DB_ID_ENV_VAR))
                .build());
    install(new SpannerMetadataDbModule());
    bind(GcpInstanceManagementConfig.class)
        .toInstance(
            GcpInstanceManagementConfig.builder()
                .setRegion(getRegion())
                .setProjectId(getProjectId())
                .setManagedInstanceGroupName(getManagedInstanceGroupName())
                .build());
  }
}
