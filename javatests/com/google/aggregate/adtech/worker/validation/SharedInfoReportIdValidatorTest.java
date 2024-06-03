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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.INVALID_REPORT_ID;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;

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
public class SharedInfoReportIdValidatorTest {

  // Under test
  private SharedInfoReportIdValidator validator;

  private Report.Builder reportBuilder;

  private Job ctx;

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  private static final String EMPTY_STRING = "";

  @Before
  public void setUp() {
    validator = new SharedInfoReportIdValidator();
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    ctx = FakeJobGenerator.generate("");
  }

  @Test
  public void sharedInfo_missingReportId_validationFails() {
    SharedInfo sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(LATEST_VERSION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .build();
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(INVALID_REPORT_ID);
  }

  @Test
  public void sharedInfo_emptyReportId_validationFails() {
    SharedInfo sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(LATEST_VERSION)
            .setReportId(EMPTY_STRING)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .build();
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(INVALID_REPORT_ID);
  }

  @Test
  public void sharedInfo_validReportId_validationSucceeds() {
    SharedInfo sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(LATEST_VERSION)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .build();
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isEmpty();
  }
}
