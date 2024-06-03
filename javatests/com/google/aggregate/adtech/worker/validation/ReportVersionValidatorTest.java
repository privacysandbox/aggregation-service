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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportVersionValidatorTest {

  // Under test
  private ReportVersionValidator validator;

  private Report.Builder reportBuilder;

  private Job ctx;

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  @Before
  public void setUp() {
    validator = new ReportVersionValidator();
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    ctx = FakeJobGenerator.generate("");
  }

  @Test
  public void attributionReporting_v01Reports_validationSucceeds() {
    Report reportVersion01 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion(VERSION_0_1)
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion01 = validator.validate(reportVersion01, ctx);

    assertThat(validationErrorVersion01).isEmpty();
  }

  @Test
  public void attributionReporting_v03Reports_validationSucceed() {
    Report reportVersion03 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("0.3")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion03 = validator.validate(reportVersion03, ctx);

    // v0.3 has same major version as LATEST_VERSION 0.1. Validation should pass.
    assertThat(validationErrorVersion03).isEmpty();
  }

  @Test
  public void attributionReporting_UnsupportedFutureMajorVersionReports_throwsException() {
    Report unsupportedReport =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("5.0")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    ValidationException exception =
        assertThrows(ValidationException.class, () -> validator.validate(unsupportedReport, ctx));
    assertThat(exception.getCode()).isEqualTo(UNSUPPORTED_SHAREDINFO_VERSION);
    assertThat(exception)
        .hasMessageThat()
        .contains(
            "Current Aggregation Service deployment does not support Aggregatable reports with"
                + " shared_info.version");
  }

  @Test
  public void attributionReporting_v00Reports_validationFails() {
    Report reportVersion00 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("0.0")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion00 = validator.validate(reportVersion00, ctx);

    assertThat(validationErrorVersion00.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_v0Reports_validationFails() {
    Report reportVersion0 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("0")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion0 = validator.validate(reportVersion0, ctx);

    assertThat(validationErrorVersion0.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_v1Reports_validationFails() {
    Report reportVersion1 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("1")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion1 = validator.validate(reportVersion1, ctx);

    assertThat(validationErrorVersion1.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_emptyVersionReports_validationFails() {
    Report reportVersionEmpty =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionEmpty =
        validator.validate(reportVersionEmpty, ctx);

    assertThat(validationErrorVersionEmpty.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_v123Reports_validationFails() {
    Report reportVersion123 =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("1.2.3")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersion123 = validator.validate(reportVersion123, ctx);

    assertThat(validationErrorVersion123.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_invalidVersionReports_validationFails() {
    Report reportVersionInvalid =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("invalid.version")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionInvalid =
        validator.validate(reportVersionInvalid, ctx);

    assertThat(validationErrorVersionInvalid.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_invalidVersion2Reports_validationFails() {
    Report reportVersionInvalid =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("...")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionInvalid =
        validator.validate(reportVersionInvalid, ctx);

    assertThat(validationErrorVersionInvalid.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_invalidVersion3Reports_validationFails() {
    Report reportVersionInvalid =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("0.1s")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionInvalid =
        validator.validate(reportVersionInvalid, ctx);

    assertThat(validationErrorVersionInvalid.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_invalidVersion4Reports_validationFails() {
    Report reportVersionInvalid =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("p0.1")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionInvalid =
        validator.validate(reportVersionInvalid, ctx);

    assertThat(validationErrorVersionInvalid.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_negativeVersionReports_validationFails() {
    Report reportVersionNegative =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("-0.1")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionNegative =
        validator.validate(reportVersionNegative, ctx);

    assertThat(validationErrorVersionNegative.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }

  @Test
  public void attributionReporting_invalidPositiveVersionReports_validationFails() {
    Report reportVersionInvalidPositive =
        reportBuilder
            .setSharedInfo(
                SharedInfo.builder()
                    .setApi(ATTRIBUTION_REPORTING_API)
                    .setVersion("+0.+2")
                    .setReportId(RANDOM_UUID)
                    .setReportingOrigin(REPORTING_ORIGIN)
                    .setScheduledReportTime(FIXED_TIME)
                    .setSourceRegistrationTime(FIXED_TIME)
                    .setDestination(DESTINATION)
                    .build())
            .build();

    Optional<ErrorMessage> validationErrorVersionInvalidPositive =
        validator.validate(reportVersionInvalidPositive, ctx);

    assertThat(validationErrorVersionInvalidPositive.get().category())
        .isEqualTo(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION);
  }
}
