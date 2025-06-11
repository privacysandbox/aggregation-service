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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.converter;

import com.google.common.base.Converter;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.DynamoMetadataTable;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts from the map containing {@link AttributeValue}s to the JobMetadata. Intended to be used
 * for processing dynamo stream events.
 */
public final class AttributeValueMapToJobMetadataConverter
    extends Converter<Map<String, AttributeValue>, JobMetadata> {

  private static final TableSchema<JobMetadata> JOB_METADATA_TABLE_SCHEMA =
      DynamoMetadataTable.getDynamoDbTableSchema();

  /** Converts the attribute value map into an instance of the {@code JobMetadata} class. */
  @Override
  protected JobMetadata doForward(Map<String, AttributeValue> attributeValueMap) {
    return JOB_METADATA_TABLE_SCHEMA.mapToItem(attributeValueMap);
  }

  /** Converts an instance of the {@code JobMetadata} class into an attribute value map. */
  @Override
  protected Map<String, AttributeValue> doBackward(JobMetadata unused) {
    throw new UnsupportedOperationException();
  }
}
