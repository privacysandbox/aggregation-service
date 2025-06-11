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

import com.google.aggregate.adtech.worker.jobclient.JobClient;
import com.google.aggregate.adtech.worker.jobclient.JobClientImpl;
import com.google.aggregate.adtech.worker.jobclient.JobHandlerModule;
import com.google.aggregate.adtech.worker.jobclient.JobPullBackoff;
import com.google.aggregate.adtech.worker.jobclient.JobPullBackoffImpl;
import com.google.aggregate.adtech.worker.jobclient.JobValidatorModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.OptionalBinder;
import com.google.scp.operator.cpio.notificationclient.NotificationClient;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueueConfig;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueueModule;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbConfig;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbModule;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpProjectId;
import com.google.scp.shared.clients.configclient.model.WorkerParameter;

/** Guice module for binding the GCP job client functionality */
public final class GcpJobHandlerModule extends JobHandlerModule {

  /** Caller is expected to bind {@link GcpJobHandlerConfig}. */
  public GcpJobHandlerModule() {}

  @Override
  public Class<? extends JobClient> getJobClientImpl() {
    return JobClientImpl.class;
  }

  @Override
  public void customConfigure() {
    install(new PubSubJobQueueModule());
    install(new SpannerMetadataDbModule());
    bind(JobPullBackoff.class).to(JobPullBackoffImpl.class);
    install(new JobValidatorModule());
    OptionalBinder.newOptionalBinder(binder(), Key.get(NotificationClient.class));
  }

  /** Provider for the GCP job queue configuration. */
  @Provides
  PubSubJobQueueConfig providePubSubJobQueueConfig(
      GcpJobHandlerConfig config, ParameterClient parameterClient, @GcpProjectId String projectId)
      throws ParameterClientException {
    String topicId =
        parameterClient
            .getParameter(WorkerParameter.JOB_PUBSUB_TOPIC_ID.name())
            .orElse(config.pubSubTopicId());
    String subscriptionId =
        parameterClient
            .getParameter(WorkerParameter.JOB_PUBSUB_SUBSCRIPTION_ID.name())
            .orElse(config.pubSubSubscriptionId());
    String messageLeaseSeconds =
        parameterClient
            .getParameter(WorkerParameter.MAX_JOB_PROCESSING_TIME_SECONDS.name())
            .orElse(Integer.toString(config.pubSubMessageLeaseSeconds()));

    return PubSubJobQueueConfig.builder()
        .setGcpProjectId(projectId)
        .setPubSubTopicId(topicId)
        .setPubSubSubscriptionId(subscriptionId)
        .setPubSubMaxMessageSizeBytes(1000)
        .setPubSubMessageLeaseSeconds(Integer.parseInt(messageLeaseSeconds))
        .setEndpointUrl(config.pubSubEndpoint())
        .build();
  }

  /** Provider for the GCP metadata DB configuration. */
  @Provides
  SpannerMetadataDbConfig provideSpannerMetadataDbConfig(
      GcpJobHandlerConfig config, ParameterClient parameterClient, @GcpProjectId String projectId)
      throws ParameterClientException {
    String spannerInstanceId =
        parameterClient
            .getParameter(WorkerParameter.JOB_SPANNER_INSTANCE_ID.name())
            .orElse(config.spannerInstanceId());
    String spannerDbName =
        parameterClient
            .getParameter(WorkerParameter.JOB_SPANNER_DB_NAME.name())
            .orElse(config.spannerDbName());

    return SpannerMetadataDbConfig.builder()
        .setGcpProjectId(projectId)
        .setSpannerInstanceId(spannerInstanceId)
        .setSpannerDbName(spannerDbName)
        .setEndpointUrl(config.spannerEndpoint())
        .build();
  }

  /** Provider for the maximum number of attempts allowed for a job. */
  @Provides
  @JobClientJobMaxNumAttemptsBinding
  int provideMaxNumAttempts(GcpJobHandlerConfig config, ParameterClient parameterClient)
      throws ParameterClientException {
    String maxNumAttempts =
        parameterClient
            .getParameter(WorkerParameter.MAX_JOB_NUM_ATTEMPTS.name())
            .orElse(Integer.toString(config.maxNumAttempts()));
    return Integer.parseInt(maxNumAttempts);
  }
}
