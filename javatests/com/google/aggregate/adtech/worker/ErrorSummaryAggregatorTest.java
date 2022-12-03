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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.NUM_REPORTS_WITH_ERRORS;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.ORIGINAL_REPORT_TIME_MISMATCH;
import static com.google.common.truth.Truth.assertThat;
import static com.google.scp.operator.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory.DECRYPTION_ERROR;

import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ErrorSummaryAggregatorTest {

  /**
   * Test that {@code DecryptionValidationResult}s with errors are aggregated to a summary correctly
   * and that {@code DecryptionValidationResult}s without errors are ignored.
   */
  @Test
  public void shouldAggregateResultsWithErrors() {
    ImmutableList<DecryptionValidationResult> decryptionValidationResults =
        ImmutableList.of(
            DecryptionValidationResult.builder()
                .addErrorMessage(
                    ErrorMessage.builder()
                        .setCategory(DECRYPTION_ERROR.name())
                        .setDetailedErrorMessage("foo")
                        .build())
                .build(),
            DecryptionValidationResult.builder()
                .addErrorMessage(
                    ErrorMessage.builder()
                        .setCategory(ATTRIBUTION_REPORT_TO_MISMATCH.name())
                        .setDetailedErrorMessage("bar")
                        .build())
                .addErrorMessage(
                    ErrorMessage.builder()
                        .setCategory(ORIGINAL_REPORT_TIME_MISMATCH.name())
                        .setDetailedErrorMessage("fizz")
                        .build())
                .build(),
            DecryptionValidationResult.builder()
                .setReport(FakeReportGenerator.generateWithParam(0, /* reportVersion */ ""))
                .build(),
            DecryptionValidationResult.builder()
                .addErrorMessage(
                    ErrorMessage.builder()
                        .setCategory(ATTRIBUTION_REPORT_TO_MISMATCH.name())
                        .setDetailedErrorMessage("buzz")
                        .build())
                .build());
    ErrorSummary expectedErrorSummary =
        ErrorSummary.newBuilder()
            .addAllErrorCounts(
                ImmutableList.of(
                    ErrorCount.newBuilder()
                        .setCategory(DECRYPTION_ERROR.name())
                        .setCount(1L)
                        .build(),
                    ErrorCount.newBuilder()
                        .setCategory(ATTRIBUTION_REPORT_TO_MISMATCH.name())
                        .setCount(2L)
                        .build(),
                    ErrorCount.newBuilder()
                        .setCategory(ORIGINAL_REPORT_TIME_MISMATCH.name())
                        .setCount(1L)
                        .build(),
                    ErrorCount.newBuilder()
                        .setCategory(NUM_REPORTS_WITH_ERRORS.name())
                        .setCount(3L)
                        .build()))
            .build();

    ErrorSummary errorSummary =
        ErrorSummaryAggregator.createErrorSummary(decryptionValidationResults);

    assertThat(errorSummary).isEqualTo(expectedErrorSummary);
  }
}
