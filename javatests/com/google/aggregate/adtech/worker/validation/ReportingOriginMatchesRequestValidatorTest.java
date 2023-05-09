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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableMap;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportingOriginMatchesRequestValidatorTest {

  // Under test
  private ReportingOriginMatchesRequestValidator validator;

  private Report.Builder reportBuilder;
  private SharedInfo.Builder sharedInfoBuilder;
  private Job ctx;

  @Before
  public void setUp() {
    validator = new ReportingOriginMatchesRequestValidator();
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    sharedInfoBuilder =
        SharedInfo.builder()
            .setReportingOrigin("")
            .setPrivacyBudgetKey("")
            .setScheduledReportTime(Instant.now());
    ctx = FakeJobGenerator.generateBuilder("").build();
  }

  /**
   * Test that the validation passed when the report and the aggregation request ({@code Job}) have
   * matching attributionReportTo values.
   */
  @Test
  public void testMatchingPasses() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("foo.com").build())
            .build();
    Job testCtx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .clearJobParameters()
                    .putAllJobParameters(ImmutableMap.of("attribution_report_to", "foo.com"))
                    .build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, testCtx);

    assertThat(validationError).isEmpty();
  }

  /**
   * Test that validation fails when the report and the aggregation request ({@code Job}) have
   * different attributionReportTo values.
   */
  @Test
  public void testMismatchingFails() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("foo.com").build())
            .build();
    Job testCtx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .clearJobParameters()
                    .putAllJobParameters(ImmutableMap.of("attribution_report_to", "bar.com"))
                    .build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, testCtx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(ATTRIBUTION_REPORT_TO_MISMATCH);
  }
}
