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

/** A helper class to wrap GCP instance IDs together with their instance template. */
@AutoValue
public abstract class GcpComputeInstance {

  /** Returns a new instance of the {@code GcpComputeInstance.Builder} class. */
  static Builder builder() {
    return new AutoValue_GcpComputeInstance.Builder();
  }

  /** Returns the instance's ID. */
  public abstract String getInstanceId();

  /** Returns the instance's template. */
  public abstract String getInstanceTemplate();

  /** Builder class for the {@code GcpComputeInstance} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the instance ID. */
    public abstract Builder setInstanceId(String instanceId);

    /** Set the instance template. */
    public abstract Builder setInstanceTemplate(String instanceTemplate);

    /** Returns a new instance of the {@code GcpComputeInstance} class from the builder. */
    public abstract GcpComputeInstance build();
  }
}
