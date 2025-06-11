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

import com.google.auto.value.AutoValue;

/** Configurations for {@code GcpInstanceManagementModule}. */
@AutoValue
public abstract class GcpInstanceManagementConfig {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new AutoValue_GcpInstanceManagementConfig.Builder();
  }

  /** GCP project id. */
  public abstract String projectId();

  /** GCP region. */
  public abstract String region();

  /** The name of the worker managed instance group. */
  public abstract String managedInstanceGroupName();

  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the GCP project id. */
    public abstract Builder setProjectId(String projectId);

    /** Set the GCP region. */
    public abstract Builder setRegion(String region);

    /** Set the name of the worker managed instance group. */
    public abstract Builder setManagedInstanceGroupName(String managedInstanceInstanceGroupName);

    /** Return a new instance of the {@code GcpInstanceManagementConfig} class from the builder. */
    public abstract GcpInstanceManagementConfig build();
  }
}
