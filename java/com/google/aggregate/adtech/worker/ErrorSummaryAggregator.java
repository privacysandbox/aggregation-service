/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.adtech.worker;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.NUM_REPORTS_WITH_ERRORS;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.common.collect.ImmutableMap;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates {@code DecryptionValidationResults} that have errors present so they can be summarized
 * and provided in the output as debugging information. This allows requesters to see how many
 * reports were excluded from aggregation and what errors were present.
 *
 * <p>This implementation is thread-safe using ConcurrentHashMap and AtomicInteger. It will keep add
 * error count to ConcurrentMap. And after each report is processed, use createErrorSummary to
 * generate a error summary.
 */
public final class ErrorSummaryAggregator {

  private final ConcurrentMap<ErrorCounter, AtomicLong> errorMap = new ConcurrentHashMap<>();
  private final AtomicLong totalErrorCounts = new AtomicLong(0L);
  private final Optional<Long> errorThresholdValue;
  private final double errorThresholdPercentage;

  /**
   * Creates an ErrorSummaryAggregator object calculating the errorThresholdValue.
   *
   * @param totalReportCountsOptional total reports counts from which the threshold value will be
   *     calculated
   * @param errorThresholdPercentage percentage value for error threshold
   */
  public static ErrorSummaryAggregator createErrorSummaryAggregator(
      Optional<Long> totalReportCountsOptional, double errorThresholdPercentage) {
    Optional<Long> errorThresholdValue =
        totalReportCountsOptional.map(
            totalReportCounts -> Math.round(totalReportCounts * errorThresholdPercentage / 100));
    if (errorThresholdValue.isEmpty() && errorThresholdPercentage == 0) {
      errorThresholdValue = Optional.of(0L);
    }
    return new ErrorSummaryAggregator(errorThresholdValue, errorThresholdPercentage);
  }

  private ErrorSummaryAggregator(
      Optional<Long> errorThresholdValue, double errorThresholdPercentage) {
    this.errorThresholdValue = errorThresholdValue;
    this.errorThresholdPercentage = errorThresholdPercentage;
  }

  /** Add DecryptionValidationResult to errorMap and also add 1 to totalCounts. */
  public void add(DecryptionValidationResult result) {
    if (result.errorMessages().isEmpty()) {
      return;
    }
    result.errorMessages().stream()
        .map(ErrorMessage::category)
        .forEach(
            errorCategory -> {
              errorMap.computeIfAbsent(errorCategory, p -> new AtomicLong(0L)).getAndAdd(1L);
            });
    totalErrorCounts.getAndAdd(1L);
  }

  /** Finds if the error counts have exceeded the set threshold. */
  public boolean countsAboveThreshold() {
    return errorThresholdValue.isPresent() && (totalErrorCounts.get() > errorThresholdValue.get());
  }

  @Deprecated(
  /* This will be removed in favor of giving the totalReportCounts in the beginning to enable failing early b/218924983.*/ )
  public boolean countsAboveThreshold(long totalReportCount) {
    return totalErrorCounts.get() > ((totalReportCount * errorThresholdPercentage) / 100);
  }

  /** Creates an {@code ErrorSummary} from the errorMap. */
  public ErrorSummary createErrorSummary() {

    // Because NUM_REPORTS_WITH_ERRORS is designed to be the last entry of the map, it can't be
    // added in the `add` function and also need to use ImmutableMap to ensure
    // NUM_REPORTS_WITH_ERRORS is last entry.
    ImmutableMap<ErrorCounter, Long> immutableErrorMap = ImmutableMap.of();

    if (errorMap.size() > 0) {
      ImmutableMap.Builder<ErrorCounter, Long> immutableErrorMapBuilder = ImmutableMap.builder();
      errorMap.forEach(
          (errorName, errorCount) -> immutableErrorMapBuilder.put(errorName, errorCount.get()));
      immutableErrorMapBuilder.put(NUM_REPORTS_WITH_ERRORS, totalErrorCounts.get());
      immutableErrorMap = immutableErrorMapBuilder.build();
    }

    return ErrorSummary.newBuilder()
        .addAllErrorCounts(
            immutableErrorMap.entrySet().stream()
                .map(
                    i ->
                        ErrorCount.newBuilder()
                            .setCount(i.getValue())
                            .setCategory(i.getKey().name())
                            .setDescription(i.getKey().getDescription())
                            .build())
                .collect(toImmutableList()))
        .build();
  }
}
