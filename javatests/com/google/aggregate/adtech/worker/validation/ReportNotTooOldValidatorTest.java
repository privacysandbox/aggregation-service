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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ORIGINAL_REPORT_TIME_TOO_OLD;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static java.time.temporal.ChronoUnit.DAYS;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportNotTooOldValidatorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Used for fixing the clock used by the validator
  static Instant now = Instant.now();

  // Under test
  @Inject private ReportNotTooOldValidator validator;

  private Report.Builder reportBuilder;
  private SharedInfo.Builder sharedInfoBuilder;
  private Job ctx;

  @Before
  public void setUp() {
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setReportingOrigin("")
            .setDestination("")
            .setSourceRegistrationTime(Instant.now())
            .setScheduledReportTime(Instant.now());
    ctx = FakeJobGenerator.generate("");
  }

  /** Test that a report that is younger 90 days old passes validation */
  @Test
  public void testYoungerThan90DaysPasses() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setScheduledReportTime(now.minus(45, DAYS)).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isEmpty();
  }

  /** Test that a report that is older than 90 days old fails validation */
  @Test
  public void testOlderThan90DaysFails() {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setScheduledReportTime(now.minus(91, DAYS)).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(ORIGINAL_REPORT_TIME_TOO_OLD);
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(Clock.class).toInstance(Clock.fixed(now, ZoneId.systemDefault()));
    }
  }
}
