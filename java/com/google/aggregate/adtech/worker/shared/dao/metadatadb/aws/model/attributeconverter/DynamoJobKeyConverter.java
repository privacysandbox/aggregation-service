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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.attributeconverter;

import static com.google.aggregate.adtech.worker.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Dynamo converter to handle the conversion between Attribute String and {@link
 * JobKey}.
 */
public final class DynamoJobKeyConverter implements AttributeConverter<JobKey> {

  /** Creates a new instance of the {@code DynamoJobKeyConverter} class. */
  public static DynamoJobKeyConverter create() {
    return new DynamoJobKeyConverter();
  }

  /** Converts a {@code JobKey} object to an Attribute String. */
  @Override
  public AttributeValue transformFrom(JobKey input) {
    return AttributeValue.builder().s(toJobKeyString(input)).build();
  }

  /** Converts the Attribute String value to a JobKey. */
  @Override
  public JobKey transformTo(AttributeValue attributeValue) {
    String attributeStringValue = attributeValue.s();
    return JobKey.newBuilder().setJobRequestId(attributeStringValue).build();
  }

  /** Returns a DynamoDB enhanced type for the {@code JobKey} class. */
  @Override
  public EnhancedType<JobKey> type() {
    return EnhancedType.of(JobKey.class);
  }

  /** Returns the DynamoDB attribute value type for the {@code JobKey} class. */
  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.S;
  }
}
