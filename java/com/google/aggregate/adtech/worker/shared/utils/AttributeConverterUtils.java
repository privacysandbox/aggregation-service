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

package com.google.aggregate.adtech.worker.shared.utils;

import com.google.protobuf.Descriptors;
import com.google.aggregate.protos.shared.backend.JobParametersProto;

/**
 * Utility class for converting attributes to their corresponding field values in the JobParameters
 * proto.
 */
public class AttributeConverterUtils {

  public static void setFieldValue(
      Descriptors.FieldDescriptor field,
      JobParametersProto.JobParameters.Builder builder,
      String value) {
    switch (field.getJavaType()) {
      case STRING:
        builder.setField(field, value);
        break;
      case LONG:
        // This represents int64 which translates to a Long data type in Java.
        builder.setField(field, Long.parseLong(value));
        break;
      case DOUBLE:
        builder.setField(field, Double.parseDouble(value));
        break;
      case BOOLEAN:
        builder.setField(field, Boolean.parseBoolean(value));
        break;
      default:
        throw new ObjectConversionException("Unsupported field type: " + value);
    }
  }
}
