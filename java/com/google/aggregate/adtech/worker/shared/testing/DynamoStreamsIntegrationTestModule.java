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

package com.google.aggregate.adtech.worker.shared.testing;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.service.aws.DynamoStreamsJobMetadataHandler;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.adtech.worker.shared.injection.modules.BaseDataModule;
import java.net.URI;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/** Dependencies used for integration testing the {@link DynamoStreamsJobMetadataHandler} class. */
@AutoService(BaseDataModule.class)
public final class DynamoStreamsIntegrationTestModule extends AbstractModule {

  private static final Region DYNAMO_TABLE_REGION = Region.US_EAST_1;

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    install(new CommonTestModule());
    // Bind all classes needed for data related services
    Multibinder<JobMetadataChangeHandler> multibinder =
        Multibinder.newSetBinder(binder(), JobMetadataChangeHandler.class);
    multibinder.addBinding().to(CopyJobHandler.class);
    install(new DynamoModule("copyjobmetadatastreams", Copy.class));
    install(new DynamoModule("jobmetadatastreams", Source.class));

    DynamoDbClientBuilder dynamoClientBuilder =
        DynamoDbClient.builder().region(DYNAMO_TABLE_REGION);
    if (endpointOverride() != null) {
      dynamoClientBuilder.endpointOverride(URI.create(endpointOverride()));
    }
    bind(DynamoDbEnhancedClient.class)
        .toInstance(
            DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoClientBuilder.build()).build());
  }

  /** Gets the endpoint override URL. */
  public String endpointOverride() {
    return null;
  }
}
