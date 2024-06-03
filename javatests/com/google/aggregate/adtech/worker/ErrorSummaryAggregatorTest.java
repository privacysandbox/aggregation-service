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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test that {@code DecryptionValidationResult}s with errors are aggregated to a summary correctly
 * and that {@code DecryptionValidationResult}s without errors are ignored.
 */
@RunWith(JUnit4.class)
public class ErrorSummaryAggregatorTest {

  private static final DecryptionValidationResult NO_ERROR_RESULTS =
      DecryptionValidationResult.builder()
          .setReport(
              FakeReportGenerator.generateWithParam(
                  0, /* reportVersion */ LATEST_VERSION, "https://foo.com"))
          .build();

  private static final ImmutableList<DecryptionValidationResult> DECRYPTION_VALIDATION_RESULTS =
      ImmutableList.of(
          generateResult(ErrorCounter.DECRYPTION_ERROR),
          generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
          NO_ERROR_RESULTS,
          generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH));

  private static final ErrorSummary EXPECTED_ERROR_SUMMARY =
      ErrorSummary.newBuilder()
          .addAllErrorCounts(
              ImmutableList.of(
                  generateExpectedResult(ErrorCounter.DECRYPTION_ERROR, 1L),
                  generateExpectedResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH, 2L),
                  generateExpectedResult(ErrorCounter.NUM_REPORTS_WITH_ERRORS, 3L)))
          .build();

  @Test
  public void shouldAggregateResultsWithErrorsSingleThread() {
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.of(4L), 100);
    DECRYPTION_VALIDATION_RESULTS.forEach(aggregator::add);
    ErrorSummary errorSummary = aggregator.createErrorSummary();
    // The order is not necessary to be the same.
    assertThat(errorSummary.getErrorCountsList())
        .containsExactlyElementsIn(EXPECTED_ERROR_SUMMARY.getErrorCountsList());
    // The last entry should be NUM_REPORTS_WITH_ERRORS.
    assertThat(
            errorSummary
                .getErrorCountsList()
                .get(errorSummary.getErrorCountsCount() - 1)
                .getCategory())
        .isEqualTo(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name());
  }

  @Test
  public void shouldAggregateResultsWithErrorsMultiThreads_lessThanNumOfThreads()
      throws InterruptedException, ExecutionException {
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.of(4L), 100);

    processErrors(DECRYPTION_VALIDATION_RESULTS, aggregator);
    ErrorSummary errorSummary = aggregator.createErrorSummary();

    // The order is not necessary to be the same.
    assertThat(errorSummary.getErrorCountsList())
        .containsExactlyElementsIn(EXPECTED_ERROR_SUMMARY.getErrorCountsList());
    // The last entry should be NUM_REPORTS_WITH_ERRORS.
    assertThat(
            errorSummary
                .getErrorCountsList()
                .get(errorSummary.getErrorCountsCount() - 1)
                .getCategory())
        .isEqualTo(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name());
  }

  @Test
  public void shouldAggregateResultsWithErrorsMultiThreads_largerThanNumOfThreads()
      throws InterruptedException, ExecutionException {
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.of(40L), 100);
    List result = DECRYPTION_VALIDATION_RESULTS.asList();
    List<DecryptionValidationResult> largeDecryptionValidationResults = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      largeDecryptionValidationResults.addAll(result);
    }
    ErrorSummary largeExpectedErrorSummary =
        ErrorSummary.newBuilder()
            .addAllErrorCounts(
                ImmutableList.of(
                    generateExpectedResult(ErrorCounter.DECRYPTION_ERROR, 10L),
                    generateExpectedResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH, 20L),
                    generateExpectedResult(ErrorCounter.NUM_REPORTS_WITH_ERRORS, 30L)))
            .build();

    processErrors(largeDecryptionValidationResults, aggregator);

    ErrorSummary errorSummary = aggregator.createErrorSummary();
    // The order is not necessary to be the same.
    assertThat(errorSummary.getErrorCountsList())
        .containsExactlyElementsIn(largeExpectedErrorSummary.getErrorCountsList());
    // The last entry should be NUM_REPORTS_WITH_ERRORS.
    assertThat(
            errorSummary
                .getErrorCountsList()
                .get(errorSummary.getErrorCountsCount() - 1)
                .getCategory())
        .isEqualTo(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name());
  }

  @Test
  public void countsAboveThreshold_withCountProvided_exceedsThreshold() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            generateResult(ErrorCounter.DECRYPTION_ERROR),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS);
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.of(6L), 10);
    decryptionValidationResults.forEach(aggregator::add);

    assertThat(aggregator.countsAboveThreshold()).isTrue();
  }

  @Test
  public void countsAboveThreshold_withCountProvided_withinThreshold() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            generateResult(ErrorCounter.DECRYPTION_ERROR),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS);
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.of(6L), 50);
    decryptionValidationResults.forEach(aggregator::add);

    assertThat(aggregator.countsAboveThreshold()).isFalse();
  }

  @Test
  public void countsAboveThreshold_withNoCountProvided_withThresholdZero_exceedsThreshold() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            generateResult(ErrorCounter.DECRYPTION_ERROR),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS);
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.empty(), 0);
    decryptionValidationResults.forEach(aggregator::add);

    assertThat(aggregator.countsAboveThreshold()).isTrue();
  }

  @Test
  public void countsAboveThreshold_withNoCountProvided_exceedsThreshold() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            generateResult(ErrorCounter.DECRYPTION_ERROR),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS);
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.empty(), 0);
    decryptionValidationResults.forEach(aggregator::add);

    assertThat(aggregator.countsAboveThreshold(6)).isTrue();
  }

  @Test
  public void countsAboveThreshold_withNoCountProvided_withinThreshold() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            generateResult(ErrorCounter.DECRYPTION_ERROR),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            generateResult(ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH),
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS,
            NO_ERROR_RESULTS);
    ErrorSummaryAggregator aggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(Optional.empty(), 50);
    decryptionValidationResults.forEach(aggregator::add);

    assertThat(aggregator.countsAboveThreshold(6)).isFalse();
  }

  private static ErrorCount generateExpectedResult(ErrorCounter error, Long count) {
    return ErrorCount.newBuilder()
        .setCategory(error.name())
        .setDescription(error.getDescription())
        .setCount(count)
        .build();
  }

  private static DecryptionValidationResult generateResult(ErrorCounter error) {
    return DecryptionValidationResult.builder()
        .addErrorMessage(ErrorMessage.builder().setCategory(error).build())
        .build();
  }

  private static void processErrors(
      List<DecryptionValidationResult> decryptionValidationResults,
      ErrorSummaryAggregator aggregator)
      throws ExecutionException, InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    ImmutableList<Future> resultFutures =
        decryptionValidationResults.stream()
            .map(result -> executor.submit(() -> aggregator.add(result)))
            .collect(toImmutableList());

    for (Future future : resultFutures) {
      future.get();
    }
    executor.shutdown();
  }
}
