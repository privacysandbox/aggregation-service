/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.shared.testutils.gcp;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import org.testcontainers.utility.DockerImageName;

public class PubSubEmulatorContainerTestModule extends AbstractModule {
  private final String projectId;
  private final String topicId;
  private final String subscriptionId;

  public PubSubEmulatorContainerTestModule(
      String projectId, String topicId, String subscriptionId) {
    this.projectId = projectId;
    this.topicId = topicId;
    this.subscriptionId = subscriptionId;
  }

  @Provides
  @Singleton
  public PubSubEmulatorContainer providePubSubEmulator() {
    return new PubSubEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators"));
  }

  @Provides
  @Singleton
  public TransportChannelProvider provideChannelProvider(PubSubEmulatorContainer emulator) {
    String hostport = emulator.getEmulatorEndpoint();
    ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
    return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
  }

  @Provides
  @Singleton
  public Publisher providePublisher(TransportChannelProvider channelProvider) throws IOException {
    NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();

    createTopic(topicId, channelProvider, credentialsProvider);
    createSubscription(subscriptionId, topicId, channelProvider, credentialsProvider);

    return Publisher.newBuilder(TopicName.of(projectId, topicId))
        .setChannelProvider(channelProvider)
        .setCredentialsProvider(credentialsProvider)
        .build();
  }

  @Provides
  SubscriberStub provideSubscriber(TransportChannelProvider channelProvider) throws IOException {

    NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();

    SubscriberStubSettings subscriberStubSettings =
        SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();

    return GrpcSubscriberStub.create(subscriberStubSettings);
  }

  private void createTopic(
      String topicId,
      TransportChannelProvider channelProvider,
      NoCredentialsProvider credentialsProvider)
      throws IOException {
    TopicAdminSettings topicAdminSettings =
        TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      TopicName topicName = TopicName.of(projectId, topicId);
      topicAdminClient.createTopic(topicName);
    }
  }

  private void createSubscription(
      String subscriptionId,
      String topicId,
      TransportChannelProvider channelProvider,
      NoCredentialsProvider credentialsProvider)
      throws IOException {
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(subscriptionAdminSettings);
    ProjectSubscriptionName subscriptionName =
        ProjectSubscriptionName.of(projectId, subscriptionId);
    subscriptionAdminClient.createSubscription(
        subscriptionName, TopicName.of(projectId, topicId), PushConfig.getDefaultInstance(), 10);
  }
}
