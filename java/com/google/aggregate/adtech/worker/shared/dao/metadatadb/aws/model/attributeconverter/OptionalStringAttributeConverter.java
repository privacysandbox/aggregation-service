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

import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Dynamo converter to handle the Optional String. */
public final class OptionalStringAttributeConverter
    implements AttributeConverter<Optional<String>> {

  /** Creates a new instance of the {@code OptionalStringAttributeConverter} class. */
  public static OptionalStringAttributeConverter create() {
    return new OptionalStringAttributeConverter();
  }

  /** Convert Optional String values to Dynamo number type. */
  @Override
  public AttributeValue transformFrom(Optional<String> input) {
    if (input.isEmpty()) {
      return null;
    }
    return AttributeValue.builder().s(input.get()).build();
  }

  /** Convert Dynamo AttributeValue to Optional String. */
  @Override
  public Optional<String> transformTo(AttributeValue attributeValue) {
    return Optional.ofNullable(attributeValue.s());
  }

  /** Returns a DynamoDB enhanced type for the {@code Optional<String>} class. */
  @Override
  public EnhancedType<Optional<String>> type() {
    return EnhancedType.optionalOf(String.class);
  }

  /** Returns a DynamoDB attribute value type for the {@code Optional<String>} class. */
  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.S;
  }
}
