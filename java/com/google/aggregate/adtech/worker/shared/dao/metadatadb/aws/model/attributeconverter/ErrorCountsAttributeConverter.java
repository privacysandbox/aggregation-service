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

import com.google.common.collect.ImmutableList;
import com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.aggregate.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Dynamo converter to handle the {@link
 * com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary}'s errorCounts map.
 */
public final class ErrorCountsAttributeConverter implements AttributeConverter<List<ErrorCount>> {

  private static final String DESCRIPTION_COLUMN_NAME = "Description";

  /** Creates a new instance of the {@code ErrorCountsAttributeConverter} class. */
  public static ErrorCountsAttributeConverter create() {
    return new ErrorCountsAttributeConverter();
  }

  /**
   * Convert the map keys from {@link JobErrorCategory} values to strings and the values to the
   * Dynamo number type.
   */
  @Override
  public AttributeValue transformFrom(List<ErrorCount> errorCounts) {
    ImmutableList<AttributeValue> convertedList =
        errorCounts.stream()
            .map(
                entry ->
                    AttributeValue.builder()
                        .m(
                            Map.of(
                                "Count",
                                AttributeValue.builder().n(Long.toString(entry.getCount())).build(),
                                "Category",
                                AttributeValue.builder().s(entry.getCategory().toString()).build(),
                                DESCRIPTION_COLUMN_NAME,
                                AttributeValue.builder()
                                    .s(entry.getDescription().toString())
                                    .build()))
                        .build())
            .collect(ImmutableList.toImmutableList());
    return AttributeValue.builder().l(convertedList).build();
  }

  /**
   * Convert the map keys from Strings corresponding to {@link JobErrorCategory} values to {@link
   * JobErrorCategory} value and the values from the Dynamo number type to Longs.
   *
   * <p>If the {@link AttributeValue} from Dynamo is null, this returns an empty map.
   */
  @Override
  public List<ErrorCount> transformTo(AttributeValue attributeValue) {
    return Optional.ofNullable(attributeValue.l())
        .map(
            dynamoList ->
                dynamoList.stream()
                    .map(
                        entry ->
                            ErrorCount.newBuilder()
                                .setCategory(entry.m().get("Category").s())
                                .setCount(Long.parseLong(entry.m().get("Count").n()))
                                .setDescription(
                                    entry
                                        .m()
                                        .getOrDefault(
                                            DESCRIPTION_COLUMN_NAME, AttributeValue.fromS(""))
                                        .s())
                                .build())
                    .collect(ImmutableList.toImmutableList()))
        .orElse(ImmutableList.of());
  }

  /** Returns a DynamoDB enhanced type for the {@code ErrorCount} class. */
  @Override
  public EnhancedType<List<ErrorCount>> type() {
    return EnhancedType.listOf(ErrorCount.class);
  }

  /** Returns a DynamoDB attribute value type for the {@code ErrorCount} class. */
  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.L;
  }
}
