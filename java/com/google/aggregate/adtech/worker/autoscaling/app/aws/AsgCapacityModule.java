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

package com.google.aggregate.adtech.worker.autoscaling.app.aws;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.Annotations.AsgName;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.Annotations.ScalingRatio;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsQueueUrl;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;

/**
 * Defines dependencies to be used with AsgCapacityHandler for setting the capacity for the worker
 * Auto Scaling Group.
 */
public final class AsgCapacityModule extends AbstractModule {
  /** Name of environment variable for the SQS job queue */
  private static final String AWS_SQS_URL_ENV_VAR = "AWS_SQS_URL";
  /** Environment variable for the Autoscaling group name */
  private static final String ASG_NAME_ENV_VAR = "ASG_NAME";
  /** Environment variable for the Autoscaling scaling ratio */
  private static final String SCALING_RATIO_ENV_VAR = "SCALING_RATIO";

  private static final Map<String, String> env = System.getenv();

  /** Returns name of the SQS job queue. */
  public String getSqsUrl() {
    return env.getOrDefault(AWS_SQS_URL_ENV_VAR, "unspecified_queue");
  }

  /** Returns name of autoscaling group. */
  public String getAsgName() {
    return env.getOrDefault(ASG_NAME_ENV_VAR, "unspecified_asg");
  }

  /** Returns the Autoscaling scaling ratio. */
  public Double getScalingRatio() {
    return Double.parseDouble(env.getOrDefault(SCALING_RATIO_ENV_VAR, "1"));
  }

  @Provides
  @Singleton
  public SqsClient getSqsClient(
      SdkHttpClient httpClient, AwsCredentialsProvider credentialsProvider) {
    return SqsClient.builder()
        .credentialsProvider(credentialsProvider)
        .httpClient(httpClient)
        .build();
  }

  @Provides
  @Singleton
  public AutoScalingClient provideAutoScalingClient(
      SdkHttpClient httpClient, AwsCredentialsProvider credentialsProvider) {
    return AutoScalingClient.builder()
        .credentialsProvider(credentialsProvider)
        .httpClient(httpClient)
        .build();
  }

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(JobQueueSqsQueueUrl.class).toInstance(getSqsUrl());
    bind(String.class).annotatedWith(AsgName.class).toInstance(getAsgName());
    bind(Double.class).annotatedWith(ScalingRatio.class).toInstance(getScalingRatio());

    bind(SdkHttpClient.class).toInstance(UrlConnectionHttpClient.builder().build());
    bind(AwsCredentialsProvider.class).toInstance(EnvironmentVariableCredentialsProvider.create());
  }
}
