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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;
import com.google.aggregate.protos.shared.backend.JobParametersProto.JobParameters;
import com.google.aggregate.adtech.worker.shared.utils.AttributeConverterUtils;
import com.google.aggregate.adtech.worker.shared.utils.ObjectConversionException;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Dynamo converter to handle the conversion between Attribute String and {@link
 * JobParameters}.
 */
public class JobParametersAttributeConverter implements AttributeConverter<JobParameters> {

  /** Creates a new instance of the {@code JobParametersAttributeConverter} class. */
  public static JobParametersAttributeConverter create() {
    return new JobParametersAttributeConverter();
  }

  /** Converts a {@code JobParameters} object to an Attribute Map. */
  @Override
  public AttributeValue transformFrom(JobParameters input) {
    ImmutableMap<String, AttributeValue> attributeValueMap =
        input.getAllFields().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    entry -> entry.getKey().getName(),
                    entry -> AttributeValue.builder().s(entry.getValue().toString()).build()));

    return AttributeValue.builder().m(attributeValueMap).build();
  }

  /** Converts the Attribute Map value to a JobParameters. */
  @Override
  public JobParameters transformTo(AttributeValue input) {
    JobParameters.Builder builder =
        JobParameters.newBuilder();
    for (Map.Entry<String, AttributeValue> entry : input.m().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().s();
      Descriptors.FieldDescriptor field = JobParameters.getDescriptor().findFieldByName(key);

      // This takes care of legacy data where fields that don't exist anymore are
      //   effectively ignored.
      if (field == null) {
        continue;
      }
      try {
        AttributeConverterUtils.setFieldValue(field, builder, value);
      } catch (NumberFormatException e) {
        throw new ObjectConversionException("Invalid value for field " + key + ": " + value);
      } catch (Exception e) {
        throw new ObjectConversionException(e.getMessage(), e);
      }
    }
    return builder.build();
  }

  /** Returns a DynamoDB enhanced type for the {@code JobParameters} class. */
  @Override
  public EnhancedType<JobParameters> type() {
    return EnhancedType.of(JobParameters.class);
  }

  /** Returns the DynamoDB attribute value type for the {@code JobParameters} class. */
  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.M;
  }
}
