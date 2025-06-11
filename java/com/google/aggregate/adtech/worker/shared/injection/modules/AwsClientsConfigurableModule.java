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

import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * Provides an implementation for AWS clients using the default credentials provider while enabling
 * a configurable region and override endpoint URL.
 */
public abstract class AwsClientsConfigurableModule extends BaseAwsClientsModule {

  @Override
  protected void configureModule() {}

  /** Gets the AWS region. */
  protected abstract Region getRegion();

  /** Gets the override endpoint URL. */
  protected Optional<String> getOverrideEndpointUrl() {
    return Optional.empty();
  }

  /** Gets the credentials provider. */
  protected AwsCredentialsProvider getCredentialsProvider() {
    return DefaultCredentialsProvider.create();
  }

  /**
   * Initializes the actual client based on the Client/BuilderType, ie. ClientType = S3Client,
   * BuilderType = S3Client.builder(). Uses the overridden overrideEndpointUrl, region.
   * AwsSyncClientBuilder.httpClient, AwsClientBuilder.region, AwsSyncClientBuilder.endpointOverride
   */
  @Override
  protected <
          ClientType,
          BuilderType extends
              AwsSyncClientBuilder<BuilderType, ClientType>
                  & AwsClientBuilder<BuilderType, ClientType>>
      ClientType initializeClient(BuilderType builder) {
    builder
        .httpClient(UrlConnectionHttpClient.builder().build())
        .region(getRegion())
        .credentialsProvider(getCredentialsProvider());

    // Override the endpoint using a user specified endpoint if overridden
    getOverrideEndpointUrl().ifPresent(s -> builder.endpointOverride(URI.create(s)));
    return builder.build();
  }
}
