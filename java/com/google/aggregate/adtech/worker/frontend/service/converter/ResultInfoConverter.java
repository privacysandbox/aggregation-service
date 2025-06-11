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
import com.google.inject.Inject;
import com.google.aggregate.protos.frontend.api.v1.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.frontend.api.v1.ResultInfoProto.ResultInfo;

/**
 * Converts between the {@link
 * com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo} and {@link
 * ResultInfo}.
 */
public final class ResultInfoConverter
    extends Converter<
        com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo, ResultInfo> {

  private final Converter<
          com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
          ErrorSummary>
      errorySummaryConverter;

  /** Creates a new instance of the {@code ResultInfoConverter} class. */
  @Inject
  public ResultInfoConverter(
      Converter<
              com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary,
              ErrorSummary>
          errorySummaryConverter) {

    this.errorySummaryConverter = errorySummaryConverter;
  }

  /** Converts the shared model into the frontend model. */
  @Override
  protected ResultInfo doForward(
      com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo resultInfo) {
    return ResultInfo.newBuilder()
        .setReturnCode(resultInfo.getReturnCode())
        .setReturnMessage(resultInfo.getReturnMessage())
        .setFinishedAt(resultInfo.getFinishedAt())
        .setErrorSummary(errorySummaryConverter.convert(resultInfo.getErrorSummary()))
        .build();
  }

  /** Converts the frontend model into the shared model. */
  @Override
  protected com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo doBackward(
      ResultInfo resultInfo) {
    return com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo.newBuilder()
        .setReturnCode(resultInfo.getReturnCode())
        .setReturnMessage(resultInfo.getReturnMessage())
        .setFinishedAt(resultInfo.getFinishedAt())
        .setErrorSummary(errorySummaryConverter.reverse().convert(resultInfo.getErrorSummary()))
        .build();
  }
}
