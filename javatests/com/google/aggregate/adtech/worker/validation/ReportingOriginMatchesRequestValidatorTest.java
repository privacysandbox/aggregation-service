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
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MALFORMED;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.REPORTING_SITE_MISMATCH;

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
            .setVersion(LATEST_VERSION)
            .setReportingOrigin("")
            .setDestination("")
            .setSourceRegistrationTime(Instant.now())
            .setScheduledReportTime(Instant.now());
    ctx = FakeJobGenerator.generateBuilder("").build();
  }

  private Job createTestJob(ImmutableMap<String, String> jobParameters) {
    return ctx.toBuilder()
        .setRequestInfo(
            ctx.requestInfo().toBuilder()
                .clearJobParameters()
                .putAllJobParameters(jobParameters)
                .build())
        .build();
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
    Job testCtx = createTestJob(ImmutableMap.of("attribution_report_to", "foo.com"));

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
    Job testCtx = createTestJob(ImmutableMap.of("attribution_report_to", "bar.com"));

    Optional<ErrorMessage> validationError = validator.validate(report, testCtx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(ATTRIBUTION_REPORT_TO_MISMATCH);
  }

  /**
   * Test that the validation passed when the report's reporting origin belongs to the site provided
   * in the aggregation request ({@code Job}).
   */
  @Test
  public void siteProvided_reportOriginBelongsToSite_success() {
    Report report1 =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("https://origin1.foo.com").build())
            .build();
    Report report2 =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("https://origin2.foo.com").build())
            .build();
    Job testCtx = createTestJob(ImmutableMap.of("reporting_site", "https://foo.com"));

    Optional<ErrorMessage> validationError1 = validator.validate(report1, testCtx);
    Optional<ErrorMessage> validationError2 = validator.validate(report2, testCtx);

    assertThat(validationError1).isEmpty();
    assertThat(validationError2).isEmpty();
  }

  /**
   * Test that the validation fails when the report's reporting origin belongs to a different site
   * than the one provided in the aggregation request ({@code Job}).
   */
  @Test
  public void siteProvided_reportOriginDoesNotBelongsToSite_failure() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("https://origin.foo.com").build())
            .build();
    Job testCtx = createTestJob(ImmutableMap.of("reporting_site", "https://foo1.com"));

    Optional<ErrorMessage> validationError = validator.validate(report, testCtx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REPORTING_SITE_MISMATCH);
  }

  /** Tests validation failure when the report's reporting origin is malformed. */
  @Test
  public void siteProvided_reportOriginInvalid_failure() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin("origin.foo.com").build())
            .build();
    Job testCtx = createTestJob(ImmutableMap.of("reporting_site", "https://foo1.com"));

    Optional<ErrorMessage> validationError = validator.validate(report, testCtx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(ATTRIBUTION_REPORT_TO_MALFORMED);
  }
}
