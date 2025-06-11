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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/** Holds configuration parameters for {@code SpannerMetadataDbModule}. */
// Consider consolidating with SpannerKeyDbConfig
@AutoValue
public abstract class SpannerMetadataDbConfig {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new AutoValue_SpannerMetadataDbConfig.Builder();
  }

  /** Get the instance ID of the job metadata DB. */
  public abstract String spannerInstanceId();

  /** Get the job metadata DB name. */
  public abstract String spannerDbName();

  /** Get the GCP project ID. */
  public abstract String gcpProjectId();

  /**
   * Overrides Spanner endpoint URL. Values that do not start with https:// are assumed to be
   * emulators for testing.
   */
  public abstract Optional<String> endpointUrl();

  /** Builder class for the {@code SpannerMetadataDbConfig} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the instance ID of the job metadata DB. */
    public abstract Builder setSpannerInstanceId(String instanceId);

    /** Set the job metadata DB name. */
    public abstract Builder setSpannerDbName(String dbName);

    /** Set the GCP project ID. */
    public abstract Builder setGcpProjectId(String projectId);

    /**
     * Set the spanner endpoint URL. Values that do not start with https:// are assumed to be
     * emulators for testing.
     */
    public abstract Builder setEndpointUrl(Optional<String> endpointUrl);

    /** Creates a new instance of the {@code SpannerMetadataDbConfig} class from the builder. */
    public abstract SpannerMetadataDbConfig build();
  }
}
