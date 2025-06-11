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

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.TopicName;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueMessageLeaseSeconds;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueue.JobQueuePubSubMaxMessageSizeBytes;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueue.JobQueuePubSubSubscriptionName;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueue.JobQueuePubSubTopicId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;

/** Module for Pub/Sub job queue. */
public final class PubSubJobQueueModule extends AbstractModule {

  /**
   * Creates a new instance of the {@code PubSubJobQueueModule} class.
   *
   * <p>Caller is expected to bind {@link PubSubJobQueueConfig}.
   */
  public PubSubJobQueueModule() {}

  /**
   * Configures injected dependencies for this module. Includes a binding for the {@code JobQueue}
   * class.
   */
  @Override
  protected void configure() {
    bind(JobQueue.class).to(PubSubJobQueue.class);
  }

  /** Provides an instance of the {@code SubscriberStub} class. */
  @Provides
  SubscriberStub provideSubscriber(
      @JobQueuePubSubMaxMessageSizeBytes int maxMessageSizeBytes, PubSubJobQueueConfig config)
      throws IOException {
    SubscriberStubSettings.Builder settingsBuilder = SubscriberStubSettings.newBuilder();
    config
        .endpointUrl()
        .ifPresentOrElse(
            endpoint -> {
              settingsBuilder.setTransportChannelProvider(getTransportChannelProvider(endpoint));
              if (isEmulatorEndpoint(endpoint)) {
                settingsBuilder.setCredentialsProvider(NoCredentialsProvider.create());
              }
            },
            () ->
                settingsBuilder.setTransportChannelProvider(
                    SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
                        .setMaxInboundMessageSize(maxMessageSizeBytes)
                        .build()));
    return GrpcSubscriberStub.create(settingsBuilder.build());
  }

  /** Provides an instance of the {@code Publisher} class. */
  @Provides
  Publisher providePublisher(PubSubJobQueueConfig config) throws IOException {
    Publisher.Builder publisherBuilder =
        Publisher.newBuilder(TopicName.of(config.gcpProjectId(), config.pubSubTopicId()));
    config
        .endpointUrl()
        .ifPresent(
            endpoint -> {
              publisherBuilder.setChannelProvider(getTransportChannelProvider(endpoint));
              if (isEmulatorEndpoint(endpoint)) {
                publisherBuilder.setCredentialsProvider(NoCredentialsProvider.create());
              }
            });

    return publisherBuilder.build();
  }

  private static TransportChannelProvider getTransportChannelProvider(String endpointUrl) {
    ManagedChannel channel = ManagedChannelBuilder.forTarget(endpointUrl).usePlaintext().build();
    return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
  }

  /** Provides a String representing the Pub/Sub topic ID. */
  @Provides
  @JobQueuePubSubTopicId
  String providePubSubTopicId(PubSubJobQueueConfig config) {
    return config.pubSubTopicId();
  }

  /** Provides a String representing the Pub/Sub subscription name. */
  @Provides
  @JobQueuePubSubSubscriptionName
  String providePubSubSubscriptionName(PubSubJobQueueConfig config) {
    return ProjectSubscriptionName.format(config.gcpProjectId(), config.pubSubSubscriptionId());
  }

  /** Provides an int representing the maximum Pub/Sub message size in bytes. */
  @Provides
  @JobQueuePubSubMaxMessageSizeBytes
  int providePubSubMaxMessageSizeBytes(PubSubJobQueueConfig config) {
    return config.pubSubMaxMessageSizeBytes();
  }

  /** Provides an int representing the message lease time in seconds. */
  @Provides
  @JobQueueMessageLeaseSeconds
  int provideMessageLeaseSeconds(PubSubJobQueueConfig config) {
    return config.pubSubMessageLeaseSeconds();
  }

  private static boolean isEmulatorEndpoint(String endpointUrl) {
    return !endpointUrl.toLowerCase().startsWith("https://");
  }
}
