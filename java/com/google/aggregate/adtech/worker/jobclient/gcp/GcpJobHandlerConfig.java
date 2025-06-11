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

package com.google.aggregate.adtech.worker.jobclient.gcp;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/** Holds configuration parameters for {@code GcpJobHandlerModule}. */
@AutoValue
public abstract class GcpJobHandlerConfig {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new AutoValue_GcpJobHandlerConfig.Builder();
  }

  /** Topic ID of the job queue. */
  public abstract String pubSubTopicId();

  /** Subscription ID of the job queue. */
  public abstract String pubSubSubscriptionId();

  /** Maximum size of the job queue messages. */
  public abstract int pubSubMaxMessageSizeBytes();

  /** Message lease time-out of the job queue. */
  public abstract int pubSubMessageLeaseSeconds();

  /** Instance ID of the job metadata DB. */
  public abstract String spannerInstanceId();

  /** Job metadata DB name. */
  public abstract String spannerDbName();

  /** GCP project ID. */
  public abstract String gcpProjectId();

  /** Maximum number of attempts allowed for the job. */
  public abstract int maxNumAttempts();

  /**
   * Overrides Spanner endpoint URL. Values that do not start with https:// are assumed to be
   * emulators for testing.
   */
  public abstract Optional<String> spannerEndpoint();

  /** Overrides the pubsub endpoint URL. */
  public abstract Optional<String> pubSubEndpoint();

  /** Builder for the {@code GcpJobHandlerConfig} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the topic ID of the job queue. */
    public abstract Builder setPubSubTopicId(String topicId);

    /** Set the subscription ID of the job queue. */
    public abstract Builder setPubSubSubscriptionId(String subscriptionId);

    /** Set the maximum size of the job queue messages. */
    public abstract Builder setPubSubMaxMessageSizeBytes(int maxMessageSizeBytes);

    /** Set the message lease time-out of the job queue. */
    public abstract Builder setPubSubMessageLeaseSeconds(int MessageLeaseSeconds);

    /** Set the instance ID of the job metadata DB. */
    public abstract Builder setSpannerInstanceId(String instanceId);

    /** Set the job metadata DB name. */
    public abstract Builder setSpannerDbName(String dbName);

    /** Set the GCP project ID. */
    public abstract Builder setGcpProjectId(String projectId);

    /** Set the maximum number of attempts allowed for the job. */
    public abstract Builder setMaxNumAttempts(int maxNumAttempts);

    /**
     * Set the spanner endpoint URL. Values that do not start with https:// are assumed to be
     * emulators for testing.
     */
    public abstract Builder setSpannerEndpoint(Optional<String> spannerEndpoint);

    /** Set the endpoint for the pubsub. */
    public abstract Builder setPubSubEndpoint(Optional<String> pubSubEndpoint);

    /** Return a new instance of the {@code GcpJobHandlerConfig} class from the builder. */
    public abstract GcpJobHandlerConfig build();
  }
}
