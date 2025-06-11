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
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.aggregate.protos.frontend.api.v1.ErrorCountProto.ErrorCount;
import com.google.aggregate.protos.frontend.api.v1.ErrorSummaryProto.ErrorSummary;
import java.util.stream.Collectors;

/**
 * Converts between the {@link
 * com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary} and {@link
 * ErrorSummary}. *
 */
public final class ErrorSummaryConverter
    extends Converter<
        com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
        ErrorSummary> {

  private final Converter<
          com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount, ErrorCount>
      errorCountConverter;

  /** Creates a new instance of the {@code ErrorSummaryConverter} class. */
  @Inject
  public ErrorSummaryConverter(
      Converter<
              com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount, ErrorCount>
          errorCountConverter) {

    this.errorCountConverter = errorCountConverter;
  }

  /** Converts the shared model into the frontend model. */
  @Override
  protected ErrorSummary doForward(
      com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary errorSummary) {
    ImmutableList.Builder<ErrorCount> errorCounts =
        ImmutableList.<ErrorCount>builder()
            .addAll(
                ImmutableList.copyOf(
                    errorSummary.getErrorCountsList().stream()
                        .map(errorCountConverter::convert)
                        .collect(Collectors.toUnmodifiableList())));

    long numReportsWithErrors = errorSummary.getNumReportsWithErrors();
    if (numReportsWithErrors != 0) {
      if (numReportsWithErrors > Integer.MAX_VALUE) {
        throw new IllegalStateException(
            "Storage model 'ErrorSummary.numReportsWithErrors' cannot be safely downcasted to"
                + " 32-bits");
      }
      errorCounts.add(
          ErrorCount.newBuilder()
              .setCategory("NUM_REPORTS_WITH_ERRORS")
              // Downcasting Storage 64-bit error count to API 32-bit error count.
              .setCount((int) numReportsWithErrors)
              .build());
    }

    return ErrorSummary.newBuilder()
        .addAllErrorCounts(errorCounts.build())
        .addAllErrorMessages(errorSummary.getErrorMessagesList())
        .build();
  }

  /** Converts the frontend model into the shared model. */
  @Override
  protected com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary doBackward(
      ErrorSummary errorSummary) {
    return com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary.newBuilder()
        .addAllErrorCounts(
            ImmutableList.copyOf(
                errorSummary.getErrorCountsList().stream()
                    .map(i -> errorCountConverter.reverse().convert(i))
                    .collect(Collectors.toUnmodifiableList())))
        .addAllErrorMessages(errorSummary.getErrorMessagesList())
        .build();
  }
}
