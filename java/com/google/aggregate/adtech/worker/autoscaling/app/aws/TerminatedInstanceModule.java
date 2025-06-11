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
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb.AsgInstancesDbDynamoClient;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb.AsgInstancesDbDynamoTableName;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb.AsgInstancesDbDynamoTtlDays;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Clock;
import java.util.Map;

/**
 * Defines dependencies to be used with TerminatedInstanceHandler for initial filtering of
 * terminating instances for the worker Auto Scaling Group.
 */
public final class TerminatedInstanceModule extends AbstractModule {

  private static final Map<String, String> env = System.getenv();
  private static final String ASG_INSTANCES_DYNAMO_TABLE_NAME_ENV_VAR =
      "ASG_INSTANCES_DYNAMO_TABLE_NAME";
  private static final String ASG_INSTANCES_DYNAMO_TTL_DAYS_ENV_VAR =
      "ASG_INSTANCES_DYNAMO_TTL_DAYS";

  private static String getAsgInstancesDynamoTableName() {
    return env.getOrDefault(ASG_INSTANCES_DYNAMO_TABLE_NAME_ENV_VAR, "");
  }

  private static Integer getAsgInstancesDynamoTtlDays() {
    return Integer.parseInt(env.getOrDefault(ASG_INSTANCES_DYNAMO_TTL_DAYS_ENV_VAR, "7"));
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

  @Provides
  @Singleton
  @AsgInstancesDbDynamoClient
  public DynamoDbEnhancedClient provideAsgInstancesDynamoClient(
      SdkHttpClient httpClient, AwsCredentialsProvider credentialsProvider) {
    DynamoDbClient ddbClient =
        DynamoDbClient.builder()
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient)
            .build();
    return DynamoDbEnhancedClient.builder().dynamoDbClient(ddbClient).build();
  }

  /** Provides an instance of the {@code Clock} class. */
  @Provides
  @Singleton
  public Clock provideClock() {
    return Clock.systemUTC();
  }

  @Override
  protected void configure() {
    bind(String.class)
        .annotatedWith(AsgInstancesDbDynamoTableName.class)
        .toInstance(getAsgInstancesDynamoTableName());
    bind(Integer.class)
        .annotatedWith(AsgInstancesDbDynamoTtlDays.class)
        .toInstance(getAsgInstancesDynamoTtlDays());

    bind(SdkHttpClient.class).toInstance(UrlConnectionHttpClient.builder().build());
    bind(AwsCredentialsProvider.class).toInstance(EnvironmentVariableCredentialsProvider.create());
  }
}
