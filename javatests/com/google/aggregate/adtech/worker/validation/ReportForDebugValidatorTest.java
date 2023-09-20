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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.DEBUG_NOT_ENABLED;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableMap;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportForDebugValidatorTest {

  private ReportForDebugValidator validator;
  private Job.Builder jobBuilder;
  private Report.Builder reportBuilder;
  private RequestInfo.Builder requestInfoBuilder;
  private SharedInfo.Builder sharedInfoBuilder;

  @Before
  public void setUp() {
    validator = new ReportForDebugValidator();
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    jobBuilder = FakeJobGenerator.generateBuilder("");
    requestInfoBuilder =
        RequestInfo.newBuilder()
            .setJobRequestId("123")
            .setInputDataBlobPrefix("foo")
            .setInputDataBucketName("foo")
            .setOutputDataBlobPrefix("foo")
            .setOutputDataBucketName("foo");
    sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setReportingOrigin("")
            .setDestination("")
            .setSourceRegistrationTime(Instant.now())
            .setScheduledReportTime(Instant.now())
            .setScheduledReportTime(Instant.now());
  }

  /** Test in the debug run, the report's debug mode is enabled and passes validation */
  @Test
  public void testIsDebugRunDebugModeEnabledPasses() {
    Report report =
        reportBuilder.setSharedInfo(sharedInfoBuilder.setReportDebugMode(true).build()).build();
    ImmutableMap jobParams = ImmutableMap.of("debug_run", "true");
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, job);

    assertThat(validationError).isEmpty();
  }

  /** Test in the debug run, the report's debug mode is not enabled and fails validation */
  @Test
  public void testIsDebugRunDebugModeNotEnabledFails() {
    Report report =
        reportBuilder.setSharedInfo(sharedInfoBuilder.setReportDebugMode(false).build()).build();
    ImmutableMap jobParams = ImmutableMap.of("debug_run", "true");
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, job);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(DEBUG_NOT_ENABLED);
  }

  /** Test not in the debug run, the report's debug mode is not enabled and passes validation */
  @Test
  public void testNotDebugRunDebugModeNotEnabledPasses() {
    Report report =
        reportBuilder.setSharedInfo(sharedInfoBuilder.setReportDebugMode(false).build()).build();
    ImmutableMap jobParams = ImmutableMap.of("debug_run", "false");
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, job);

    assertThat(validationError).isEmpty();
  }

  /** Test no debug run params, the report's debug mode is not enabled and passes validation */
  @Test
  public void testNoDebugRunParamsDebugModeNotEnabledPasses() {
    Report report =
        reportBuilder.setSharedInfo(sharedInfoBuilder.setReportDebugMode(false).build()).build();
    ImmutableMap jobParams = ImmutableMap.of();
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, job);

    assertThat(validationError).isEmpty();
  }
}
