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

package com.google.aggregate.adtech.worker.frontend.service.gcp;

import static com.google.aggregate.adtech.worker.frontend.service.gcp.FrontendServiceHttpFunctionBase.JOB_V1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Converter;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.TypeLiteral;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.adtech.worker.frontend.service.FrontendServiceImpl;
import com.google.aggregate.adtech.worker.frontend.service.converter.CreateJobRequestToRequestInfoConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ErrorCountConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ErrorSummaryConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.GetJobResponseConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.JobStatusConverter;
import com.google.aggregate.adtech.worker.frontend.service.converter.ResultInfoConverter;
import com.google.aggregate.adtech.worker.frontend.tasks.gcp.GcpTasksModule;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.ErrorCountProto;
import com.google.aggregate.protos.frontend.api.v1.ErrorSummaryProto;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto;
import com.google.aggregate.protos.frontend.api.v1.JobStatusProto;
import com.google.aggregate.protos.frontend.api.v1.ResultInfoProto;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueueConfig;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp.PubSubJobQueueModule;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDb.MetadataDbSpannerTtlDays;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbConfig;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDbModule;
import com.google.scp.shared.mapper.TimeObjectMapper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/** Defines dependencies for GCP implementation of FrontendService. */
public final class GcpFrontendServiceModule extends AbstractModule {

  private static final String PROJECT_ID_ENV_VAR = "PROJECT_ID";
  private static final String INSTANCE_ID_ENV_VAR = "INSTANCE_ID";
  private static final String DATABASE_ID_ENV_VAR = "DATABASE_ID";
  private static final String PUBSUB_TOPIC_ID_ENV_VAR = "PUBSUB_TOPIC_ID";
  private static final String PUBSUB_SUBSCRIPTION_ID_ENV_VAR = "PUBSUB_SUBSCRIPTION_ID";
  private static final String JOB_METADATA_TTL_ENV_VAR = "JOB_METADATA_TTL";
  private static final String JOB_VERSION_ENV_VAR = "JOB_VERSION";
  private static final String DEFAULT_JOB_VERSION = String.valueOf(JOB_V1);

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    Map<String, String> env = System.getenv();
    String projectId = env.getOrDefault(PROJECT_ID_ENV_VAR, "adh-cloud-adtech1");
    String instanceId = env.getOrDefault(INSTANCE_ID_ENV_VAR, "jobmetadatainstance");
    String databaseId = env.getOrDefault(DATABASE_ID_ENV_VAR, "jobmetadatadb");
    String pubsubTopicId = env.getOrDefault(PUBSUB_TOPIC_ID_ENV_VAR, "aggregate-service-jobqueue");
    String pubsubSubscriptionId = env.getOrDefault(PUBSUB_SUBSCRIPTION_ID_ENV_VAR, "");
    Integer metadataTtlDays = Integer.parseInt(env.getOrDefault(JOB_METADATA_TTL_ENV_VAR, "365"));

    // Service layer bindings
    bind(FrontendService.class).to(FrontendServiceImpl.class);
    bind(new TypeLiteral<Converter<JobMetadata, GetJobResponseProto.GetJobResponse>>() {})
        .to(GetJobResponseConverter.class);
    bind(new TypeLiteral<Converter<CreateJobRequest, RequestInfo>>() {})
        .to(CreateJobRequestToRequestInfoConverter.class);
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo,
                ResultInfoProto.ResultInfo>>() {})
        .to(ResultInfoConverter.class);
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
                ErrorSummaryProto.ErrorSummary>>() {})
        .to(ErrorSummaryConverter.class);
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
                JobStatusProto.JobStatus>>() {})
        .to(JobStatusConverter.class);
    bind(new TypeLiteral<
            Converter<
                com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount,
                ErrorCountProto.ErrorCount>>() {})
        .to(ErrorCountConverter.class);
    bind(ObjectMapper.class).to(TimeObjectMapper.class);

    // Business layer bindings
    install(new GcpTasksModule());

    // Data layer bindings
    bind(SpannerMetadataDbConfig.class)
        .toInstance(
            SpannerMetadataDbConfig.builder()
                .setGcpProjectId(projectId)
                .setSpannerInstanceId(instanceId)
                .setSpannerDbName(databaseId)
                .build());
    install(new SpannerMetadataDbModule());
    bind(PubSubJobQueueConfig.class)
        .toInstance(
            PubSubJobQueueConfig.builder()
                .setGcpProjectId(projectId)
                .setPubSubTopicId(pubsubTopicId)
                .setPubSubSubscriptionId(pubsubSubscriptionId)
                .setPubSubMaxMessageSizeBytes(1000)
                .setPubSubMessageLeaseSeconds(600)
                .build());
    install(new PubSubJobQueueModule());
    bind(Integer.class).annotatedWith(MetadataDbSpannerTtlDays.class).toInstance(metadataTtlDays);
    bind(Integer.class)
        .annotatedWith(FrontendServiceVersionBinding.class)
        .toInstance(
            Integer.parseInt(
                System.getenv().getOrDefault(JOB_VERSION_ENV_VAR, DEFAULT_JOB_VERSION)));
  }

  /** The version of the gcp frontend service. */
  @BindingAnnotation
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FrontendServiceVersionBinding {}
}
