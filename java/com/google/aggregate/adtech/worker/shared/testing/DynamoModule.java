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

import com.google.inject.PrivateModule;
import com.google.aggregate.adtech.worker.frontend.service.aws.DynamoStreamsJobMetadataHandler;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoClient;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoTableName;
import java.lang.annotation.Annotation;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Dependencies for the DynamoDB tables used for integration testing the {@link
 * DynamoStreamsJobMetadataHandler} class.
 */
public final class DynamoModule extends PrivateModule {

  private static final Region DYNAMO_TABLE_REGION = Region.US_WEST_2;
  private final String dynamoTableName;
  private final Class<? extends Annotation> annotation;

  /** Creates a new instance of the {@code DynamoModule} class. */
  public DynamoModule(String dynamoTableName, Class<? extends Annotation> annotation) {
    this.dynamoTableName = dynamoTableName;
    this.annotation = annotation;
  }

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    bind(String.class).annotatedWith(MetadataDbDynamoTableName.class).toInstance(dynamoTableName);
    bind(DynamoDbEnhancedClient.class)
        .annotatedWith(MetadataDbDynamoClient.class)
        .toInstance(
            DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.builder().region(DYNAMO_TABLE_REGION).build())
                .build());
    bind(DynamoMetadataDb.class).annotatedWith(annotation).to(DynamoMetadataDb.class);
    expose(DynamoMetadataDb.class).annotatedWith(annotation);
  }
}
