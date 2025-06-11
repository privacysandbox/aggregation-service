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

/** Dynamo converter to handle the Optional Integer. */
public final class OptionalIntegerAttributeConverter
    implements AttributeConverter<Optional<Integer>> {

  /** Creates a new instance of the {@code OptionalIntegerAttributeConverter} class. */
  public static OptionalIntegerAttributeConverter create() {
    return new OptionalIntegerAttributeConverter();
  }

  /** Convert Optional Integer values to DynamoDB number type. */
  @Override
  public AttributeValue transformFrom(Optional<Integer> input) {
    if (input.isEmpty()) {
      return null;
    }
    return AttributeValue.builder().n(input.get().toString()).build();
  }

  /** Convert DynamoDB AttributeValue to Optional Integer. */
  @Override
  public Optional<Integer> transformTo(AttributeValue attributeValue) {
    return Optional.of(Integer.parseInt(attributeValue.n()));
  }

  /** Returns a DynamoDB enhanced type for the {@code Optional<Integer>} class. */
  @Override
  public EnhancedType<Optional<Integer>> type() {
    return EnhancedType.optionalOf(Integer.class);
  }

  /** Returns a DynamoDB attribute value type for the {@code Optional<Integer>} class. */
  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.N;
  }
}
