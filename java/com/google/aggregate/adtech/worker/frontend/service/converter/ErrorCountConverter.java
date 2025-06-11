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
import com.google.aggregate.protos.frontend.api.v1.ErrorCountProto.ErrorCount;

/**
 * Converts between the {@link
 * com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount} and {@link
 * ErrorCount}. *
 */
public final class ErrorCountConverter
    extends Converter<
        com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount, ErrorCount> {

  /** Converts the shared model into the frontend model. */
  @Override
  protected ErrorCount doForward(
      com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount errorCount) {
    if (errorCount.getCount() > Integer.MAX_VALUE) {
      throw new IllegalStateException(
          "Storage model 'ErrorCount.count' cannot be safely downcasted to 32-bits");
    }

    return ErrorCount.newBuilder()
        // Downcasting Storage 64-bit error count to API 32-bit error count.
        .setCount((int) errorCount.getCount())
        .setCategory(errorCount.getCategory())
        .setDescription(errorCount.getDescription())
        .build();
  }

  /** Converts the frontend model into the shared model. */
  @Override
  protected com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount doBackward(
      ErrorCount errorCount) {
    return com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount.newBuilder()
        // Upcasting API 32-bit error count to Storage 64-bit error count.
        .setCount((long) errorCount.getCount())
        .setCategory(errorCount.getCategory())
        .setDescription(errorCount.getDescription())
        .build();
  }
}
