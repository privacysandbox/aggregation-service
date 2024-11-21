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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.REQUIRED_SHAREDINFO_FIELD_INVALID;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportPrivacyBudgetKeyFieldsValidatorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private Report.Builder reportBuilder;

  private Job ctx;

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  private static final String EMPTY_STRING = "";

  private static final String INVALID_API = "invalid-api";

  // Under test.
  @Inject private ReportPrivacyBudgetKeyValidator validator;

  @Before
  public void setUp() {
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    ctx = FakeJobGenerator.generate("");
  }

  @Test
  public void sharedInfo_withoutAPI_validationFails() {
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedInfo_emptyAPI_validationFails() {
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(EMPTY_STRING)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedInfo_invalidAPI_validationFails() {
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(INVALID_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedInfo_validAPI_validationSucceeds() {
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void sharedInfo_version_0_0_zeroFilteringId_validationFails() {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JobUtils.JOB_PARAM_FILTERING_IDS, "0");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion("0.0")
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedInfo_version_0_1_zeroFilteringId_validationSucceeds() {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JobUtils.JOB_PARAM_FILTERING_IDS, "0");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void sharedInfo_version_0_1_nonZeroFilteringId_validationSucceeds() {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JobUtils.JOB_PARAM_FILTERING_IDS, "5");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void sharedInfo_version_1_0_nonZeroFilteringId_validationSucceeds() {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JobUtils.JOB_PARAM_FILTERING_IDS, "2");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    SharedInfo.Builder sharedInfoVersionBuilder =
        SharedInfo.builder()
            .setApi(ATTRIBUTION_REPORTING_API)
            .setVersion(VERSION_1_0)
            .setReportId(RANDOM_UUID)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME);
    Report reportVersion = reportBuilder.setSharedInfo(sharedInfoVersionBuilder.build()).build();

    Optional<ErrorMessage> validationError = validator.validate(reportVersion, ctx);

    assertThat(validationError).isEmpty();
  }

  private ImmutableMap<String, String> combineJobParams(
      Map<String, String> currentJobParams, Map<String, String> additionalJobParams) {
    ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
    return map.putAll(currentJobParams).putAll(additionalJobParams).buildOrThrow();
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
