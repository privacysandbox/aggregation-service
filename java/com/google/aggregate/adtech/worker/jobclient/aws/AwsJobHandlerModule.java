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

package com.google.aggregate.adtech.worker.jobclient.aws;

import com.google.aggregate.adtech.worker.jobclient.JobClient;
import com.google.aggregate.adtech.worker.jobclient.JobClientImpl;
import com.google.aggregate.adtech.worker.jobclient.JobHandlerModule;
import com.google.aggregate.adtech.worker.jobclient.JobPullBackoff;
import com.google.aggregate.adtech.worker.jobclient.JobPullBackoffImpl;
import com.google.aggregate.adtech.worker.jobclient.JobValidatorModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.OptionalBinder;
import com.google.scp.operator.cpio.notificationclient.NotificationClient;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsMaxWaitTimeSeconds;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsQueueUrl;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueMessageLeaseSeconds;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoClient;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoTableName;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoTtlDays;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.scp.shared.clients.configclient.Annotations.ApplicationRegionBinding;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.clients.configclient.model.ErrorReason;
import com.google.scp.shared.clients.configclient.model.WorkerParameter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;

/** Guice module for binding the AWS job client functionality */
public final class AwsJobHandlerModule extends JobHandlerModule {

  @Override
  public Class<? extends JobClient> getJobClientImpl() {
    return JobClientImpl.class;
  }

  @Override
  public void customConfigure() {
    bind(Integer.class).annotatedWith(JobQueueSqsMaxWaitTimeSeconds.class).toInstance(5);
    bind(JobQueue.class).to(SqsJobQueue.class);
    bind(JobMetadataDb.class).to(DynamoMetadataDb.class);
    bind(JobPullBackoff.class).to(JobPullBackoffImpl.class);
    bind(Integer.class).annotatedWith(MetadataDbDynamoTtlDays.class).toInstance(365);
    OptionalBinder.newOptionalBinder(binder(), Key.get(NotificationClient.class));
    install(new JobValidatorModule());
  }

  /** Provider for an instance of the {@code SqsClient} class. */
  @Provides
  SqsClient provideSqsClient(
      AwsCredentialsProvider credentials,
      RetryPolicy retryPolicy,
      @SqsEndpointOverrideBinding URI endpointOverride,
      @ApplicationRegionBinding String region) {
    SqsClientBuilder clientBuilder =
        SqsClient.builder()
            .credentialsProvider(credentials)
            .httpClient(ApacheHttpClient.builder().build())
            .overrideConfiguration(
                ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build())
            .region(Region.of(region));

    if (!endpointOverride.toString().isEmpty()) {
      clientBuilder.endpointOverride(endpointOverride);
    }

    return clientBuilder.build();
  }

  /** Provider for a DynamoDB client. */
  @Provides
  @MetadataDbDynamoClient
  DynamoDbEnhancedClient provideDdbClient(
      AwsCredentialsProvider credentials,
      RetryPolicy retryPolicy,
      @DdbEndpointOverrideBinding URI endpointOverride,
      @ApplicationRegionBinding String region) {
    DynamoDbClientBuilder clientBuilder =
        DynamoDbClient.builder()
            .credentialsProvider(credentials)
            .httpClient(ApacheHttpClient.builder().build())
            .overrideConfiguration(
                ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build())
            .region(Region.of(region));

    if (!endpointOverride.toString().isEmpty()) {
      clientBuilder.endpointOverride(endpointOverride);
    }

    return DynamoDbEnhancedClient.builder().dynamoDbClient(clientBuilder.build()).build();
  }

  /** Provider for the URL of the SQS job queue. */
  @Provides
  @JobQueueSqsQueueUrl
  String provideQueueUrl(ParameterClient paramClient) throws ParameterClientException {
    return paramClient
        .getParameter(WorkerParameter.JOB_QUEUE.name())
        .orElseThrow(
            () ->
                new ParameterClientException(
                    "Could not get job queue url from the parameter client.",
                    ErrorReason.MISSING_REQUIRED_PARAMETER));
  }

  /** Provider for the maximum number of job attempts. */
  @Provides
  @JobClientJobMaxNumAttemptsBinding
  int provideJobMaxNumAttempts(ParameterClient paramClient) throws ParameterClientException {
    String param =
        paramClient
            .getParameter(WorkerParameter.MAX_JOB_NUM_ATTEMPTS.name())
            .orElseThrow(
                () ->
                    new ParameterClientException(
                        "Could not get job max number of attempts from the parameter client.",
                        ErrorReason.MISSING_REQUIRED_PARAMETER));
    try {
      return Integer.parseInt(param);
    } catch (NumberFormatException e) {
      throw new ParameterClientException(
          String.format(
              "Expected job max number of attempts parameter to be integer, but received %s.",
              param),
          ErrorReason.INVALID_PARAMETER_VALUE);
    }
  }

  /** Provider for the job lease time. */
  @Provides
  @JobQueueMessageLeaseSeconds
  int provideJobQueueMessageLeaseSeconds(ParameterClient paramClient)
      throws ParameterClientException {
    String param =
        paramClient
            .getParameter(WorkerParameter.MAX_JOB_PROCESSING_TIME_SECONDS.name())
            .orElseThrow(
                () ->
                    new ParameterClientException(
                        "Could not get job lease time (seconds) from the parameter client.",
                        ErrorReason.MISSING_REQUIRED_PARAMETER));
    try {
      return Integer.parseInt(param);
    } catch (NumberFormatException e) {
      throw new ParameterClientException(
          String.format(
              "Expected job queue message lease time parameter to be integer, but received %s.",
              param),
          ErrorReason.INVALID_PARAMETER_VALUE);
    }
  }

  /** Provider for the metadata DB table name. */
  @Provides
  @MetadataDbDynamoTableName
  String provideDdbTable(ParameterClient paramClient) throws ParameterClientException {
    return paramClient
        .getParameter(WorkerParameter.JOB_METADATA_DB.name())
        .orElseThrow(
            () ->
                new ParameterClientException(
                    "Could not get job metadata db table name from the parameter client.",
                    ErrorReason.MISSING_REQUIRED_PARAMETER));
  }

  /** Annotation for binding the SQS endpoint override. */
  @BindingAnnotation
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SqsEndpointOverrideBinding {}

  /** Annotation for binding DynamoDB endpoint override. */
  @BindingAnnotation
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DdbEndpointOverrideBinding {}
}
