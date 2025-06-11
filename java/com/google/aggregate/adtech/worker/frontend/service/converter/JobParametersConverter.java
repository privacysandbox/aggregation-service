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

package com.google.aggregate.adtech.worker.frontend.service.converter;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;
import com.google.aggregate.protos.shared.backend.JobParametersProto;
import com.google.aggregate.adtech.worker.shared.utils.AttributeConverterUtils;
import com.google.aggregate.adtech.worker.shared.utils.ObjectConversionException;
import java.util.Map;

/**
 * Converter between {@link Map<String, String>} and {@link JobParametersProto.JobParameters}.
 *
 * <p>The map key is the field name in {@link JobParametersProto.JobParameters}.
 */
public final class JobParametersConverter
    extends Converter<Map<String, String>, JobParametersProto.JobParameters> {

  @Override
  protected JobParametersProto.JobParameters doForward(Map<String, String> jobParametersMap) {
    JobParametersProto.JobParameters.Builder builder =
        JobParametersProto.JobParameters.newBuilder();
    for (Map.Entry<String, String> entry : jobParametersMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      Descriptors.FieldDescriptor field =
          JobParametersProto.JobParameters.getDescriptor().findFieldByName(key);

      if (field == null) {
        throw new ObjectConversionException("Invalid key: " + key + " in job parameters.");
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

  @Override
  protected ImmutableMap<String, String> doBackward(
      JobParametersProto.JobParameters jobParameters) {
    return jobParameters.getAllFields().entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey().getName(), entry -> String.valueOf(entry.getValue())));
  }
}
