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

package com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/** Holds configuration parameters for {@code PubSubJobQueueModule}. */
@AutoValue
public abstract class PubSubJobQueueConfig {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new AutoValue_PubSubJobQueueConfig.Builder();
  }

  /** Set the topic ID of the job queue. */
  public abstract String pubSubTopicId();

  /** Set the subscription ID of the job queue. */
  public abstract String pubSubSubscriptionId();

  /** Set the maximum size of the job queue messages. */
  public abstract int pubSubMaxMessageSizeBytes();

  /** Set the message lease time-out of the job queue. */
  public abstract int pubSubMessageLeaseSeconds();

  /** Set the GCP project ID. */
  public abstract String gcpProjectId();

  /** Get the pubsub endpoint url. */
  public abstract Optional<String> endpointUrl();

  /** Builder class for the {@code PubSubJobQueueConfig} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Get the topic ID of the job queue. */
    public abstract Builder setPubSubTopicId(String topicId);

    /** Get the subscription ID of the job queue. */
    public abstract Builder setPubSubSubscriptionId(String subscriptionId);

    /** Get the maximum size of the job queue messages. */
    public abstract Builder setPubSubMaxMessageSizeBytes(int maxMessageSizeBytes);

    /** Get the message lease time-out of the job queue. */
    public abstract Builder setPubSubMessageLeaseSeconds(int MessageLeaseSeconds);

    /** Get the GCP project ID. */
    public abstract Builder setGcpProjectId(String projectId);

    /** Set the pubsub endpoint url. */
    public abstract Builder setEndpointUrl(Optional<String> endpointUrl);

    /** Creates a new instance of the {@code PubSubJobQueueConfig} class from the builder. */
    public abstract PubSubJobQueueConfig build();
  }
}
