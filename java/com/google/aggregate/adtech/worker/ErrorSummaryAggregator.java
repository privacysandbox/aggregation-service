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
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;

/**
 * Aggregates {@code DecryptionValidationResults} that have errors present so they can be summarized
 * and provided in the output as debugging information. This allows requesters to see how many
 * reports were excluded from aggregation and what errors were present.
 */
public final class ErrorSummaryAggregator {

  /** Creates an {@code ErrorSummary} from a list of {@code DecryptionValidationResult} */
  public static ErrorSummary createErrorSummary(ImmutableList<DecryptionValidationResult> results) {

    ImmutableList<DecryptionValidationResult> onlyResultsWithErrors =
        results.stream()
            .filter(
                decryptionValidationResult -> !decryptionValidationResult.errorMessages().isEmpty())
            .collect(toImmutableList());

    // Iterate over all of the {@code ErrorMessages} and count the occurrence of each category
    ImmutableMap<ErrorCounter, Long> errorCounts =
        onlyResultsWithErrors.stream()
            .map(DecryptionValidationResult::errorMessages)
            .flatMap(ImmutableList::stream)
            .collect(toImmutableMap(ErrorMessage::category, (unused) -> 1L, Math::addExact));
    if (onlyResultsWithErrors.size() > 0) {
      errorCounts =
          ImmutableMap.<ErrorCounter, Long>builder()
              .putAll(errorCounts)
              .put(NUM_REPORTS_WITH_ERRORS, Long.valueOf(onlyResultsWithErrors.size()))
              .build();
    }

    return ErrorSummary.newBuilder()
        .addAllErrorCounts(
            errorCounts.entrySet().stream()
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
