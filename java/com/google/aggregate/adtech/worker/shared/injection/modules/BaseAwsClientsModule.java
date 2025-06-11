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

package com.google.aggregate.adtech.worker.shared.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Provider of AWS SDK clients. */
public abstract class BaseAwsClientsModule extends AbstractModule {

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  protected void configureModule() {}

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    configureModule();
  }

  /** Provides a singleton instance of the {@code S3Client} class. */
  @Provides
  @Singleton
  public S3Client provideS3Client() {
    return getS3Client();
  }

  /**
   * Allows subclasses to override the {@code S3Client} since overriding @Provides methods is not
   * allowed.
   */
  protected S3Client getS3Client() {
    return initializeClient(S3Client.builder());
  }

  /** Provides a singleton instance of the {@code DynamoDbClient} class. */
  @Provides
  @Singleton
  public DynamoDbClient provideDynamoDbClient() {
    return getDynamoDbClient();
  }

  /**
   * Allows subclasses to override the {@code DynamoDbClient} since overriding @Provides methods is
   * not allowed.
   */
  protected DynamoDbClient getDynamoDbClient() {
    return initializeClient(DynamoDbClient.builder());
  }

  /** Provides a singleton instance of the {@code DynamoDbEnhancedClient} class. */
  @Provides
  @Singleton
  public DynamoDbEnhancedClient provideDynamoDbEnhancedClient() {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
  }

  /** Provides a singleton instance of the {@code DynamoDbStreamsClient} class. */
  @Provides
  @Singleton
  public DynamoDbStreamsClient provideDynamoDbStreamsClient() {
    return getDynamoDbStreamsClient();
  }

  /**
   * Allows subclasses to override the {@code DynamoDbStreamsClient} since overriding @Provides
   * methods is not allowed.
   */
  protected DynamoDbStreamsClient getDynamoDbStreamsClient() {
    return initializeClient(DynamoDbStreamsClient.builder());
  }

  /** Provides a singleton instance of the {@code SqsClient} class. */
  @Provides
  @Singleton
  public SqsClient provideSqsClient() {
    return initializeClient(SqsClient.builder());
  }

  /**
   * Allows subclasses to override the {@code SqsClient} since overriding @Provides methods is not
   * allowed.
   */
  protected SqsClient getSqsClient() {
    return initializeClient(SqsClient.builder());
  }

  /** Provides a singleton instance of the {@code KmsClient} class. */
  @Provides
  @Singleton
  public KmsClient provideKmsClient() {
    return getKmsClient();
  }

  /**
   * Allows subclasses to override the {@code KmsClient} since overriding @Provides methods is not
   * allowed.
   */
  protected KmsClient getKmsClient() {
    return initializeClient(KmsClient.builder());
  }

  // TODO:(b\206012357) Remove @MetadataDbDynamoClient and use unannotated client
  /**
   * Provides a singleton instance of the {@code DynamoDbEnhancedClient} class, annotated with
   * {@code @MetadataDbDynamoClient}.
   */
  @Provides
  @Singleton
  @MetadataDbDynamoClient
  public DynamoDbEnhancedClient provideMetadataDbDynamoClient() {
    return provideDynamoDbEnhancedClient();
  }

  @Provides
  @Singleton
  public PricingClient providesPricingClient() {
    return initializeClient(PricingClient.builder());
  }
  /**
   * Initializes the actual client based on the Client/BuilderType, ie. ClientType = S3Client,
   * BuilderType = S3Client.builder()
   */
  protected abstract <
          ClientType,
          BuilderType extends
              AwsSyncClientBuilder<BuilderType, ClientType>
                  & AwsClientBuilder<BuilderType, ClientType>>
      ClientType initializeClient(BuilderType builder);
}
