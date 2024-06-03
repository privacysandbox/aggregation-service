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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.UNSUPPORTED_REPORT_API_TYPE;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.Annotations.SupportedApis;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SupportedReportApiTypeValidatorTest {
  @Rule
  public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject private SupportedReportApiTypeValidator validator;

  private Report.Builder reportBuilder;

  private Job ctx;

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  @Before
  public void setUp() {
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    ctx = FakeJobGenerator.generate("");
  }

  @Test
  public void attributionReportingReports_validationSucceeds() {
    SharedInfo.Builder sharedInfoVersion01Builder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setDestination(DESTINATION);
    Report reportVersion01 =
        reportBuilder.setSharedInfo(sharedInfoVersion01Builder.build()).build();

    Optional<ErrorMessage> validationErrorVersion01 = validator.validate(reportVersion01, ctx);

    assertThat(validationErrorVersion01).isEmpty();
  }

  @Test
  public void attributionReportingDebugReports_validationSucceeds() {
    SharedInfo sharedInfoVersion01 =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .build();
    Report reportVersion01 = reportBuilder.setSharedInfo(sharedInfoVersion01).build();

    Optional<ErrorMessage> validationErrorVersion01 = validator.validate(reportVersion01, ctx);

    assertThat(validationErrorVersion01).isEmpty();
  }

  @Test
  public void protectedAudienceReports_validationSucceeds() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(PROTECTED_AUDIENCE_API)
            .setVersion("0.1")
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void sharedStorageReports_validationSucceeds() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(SHARED_STORAGE_API)
            .setVersion("0.1")
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void invalidApiTypeReports_validationFails() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setApi("invalid-api")
            .setVersion("0.1")
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    Report report = reportBuilder.setSharedInfo(sharedInfoBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(UNSUPPORTED_REPORT_API_TYPE);
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(new TypeLiteral<ImmutableSet<String>>() {})
          .annotatedWith(SupportedApis.class)
          .toInstance(
              ImmutableSet.of(
                  ATTRIBUTION_REPORTING_API,
                  ATTRIBUTION_REPORTING_DEBUG_API,
                  PROTECTED_AUDIENCE_API,
                  SHARED_STORAGE_API));
    }
  }
}
