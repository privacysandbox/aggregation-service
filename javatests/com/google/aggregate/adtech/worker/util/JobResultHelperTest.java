/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.adtech.worker.util;

import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_SUCCESS_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_SUCCESS_WITH_ERRORS_MESSAGE;
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.Timestamp;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JobResultHelperTest {
  private static final Job job = FakeJobGenerator.generate("job1");

  private static final int NUMBER_OF_STACK_TRACE_LINES = 3;

  private static final Instant INSTANT = Instant.parse("1926-12-31T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(INSTANT, ZoneId.systemDefault());

  private static final Timestamp TIMESTAMP =
      ProtoUtil.toProtoTimestamp(Instant.parse("1926-12-31T00:00:00Z"));

  private static final AggregationJobProcessException pbsError =
      new AggregationJobProcessException(
          AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR,
          "Exception while consuming privacy budget.");

  private static final AggregationJobProcessException pbsExhausted =
      new AggregationJobProcessException(
          AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED, "Privacy Budget exhausted.");

  @Inject private JobResultHelper jobResultHelper;

  Injector injector =
      Guice.createInjector(
          new AbstractModule() {
            @Override
            protected void configure() {
              bind(Boolean.class).annotatedWith(EnableStackTraceInResponse.class).toInstance(true);
              bind(Integer.class)
                  .annotatedWith(MaxDepthOfStackTrace.class)
                  .toInstance(NUMBER_OF_STACK_TRACE_LINES);
              bind(Clock.class).toInstance(CLOCK);
            }
          });

  @Before
  public void setup() {
    injector.injectMembers(this);
  }

  @Test
  public void getJobResult_withAllSuccessfulReports_returnsSuccessCode() {
    // Arrange
    ErrorSummary errorSummary = ErrorSummary.getDefaultInstance();

    // Act
    JobResult actualJobResult = jobResultHelper.createJobResult(job, errorSummary);

    // Assert
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(errorSummary)
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS.name())
                    .setReturnMessage(RESULT_SUCCESS_MESSAGE)
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void getJobResult_withSomeFailedReports_returnsSuccessWithErrorCode() {
    // Arrange
    ErrorSummary errorSummary =
        ErrorSummary.newBuilder()
            .addAllErrorCounts(
                ImmutableList.of(
                    ErrorCount.newBuilder()
                        .setCategory(ErrorCounter.ORIGINAL_REPORT_TIME_TOO_OLD.name())
                        .setCount(4L)
                        .build(),
                    ErrorCount.newBuilder()
                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                        .setCount(4L)
                        .build()))
            .build();

    // Act
    JobResult actualJobResult = jobResultHelper.createJobResult(job, errorSummary);

    // Assert
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(errorSummary)
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                    .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE)
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void createErrorJobResult_returnsJobResultWithStackTrace() {
    // Arrange
    AggregationJobProcessException aggregationJobProcessException =
        new AggregationJobProcessException(
            AggregationWorkerReturnCode.INTERNAL_ERROR, "Resistance is futile!");

    // Act
    JobResult actualJobResult =
        jobResultHelper.createJobResultOnException(job, aggregationJobProcessException);

    // Assert
    StackTraceElement[] stackTrace = aggregationJobProcessException.getStackTrace();
    StringBuilder expectedReturnMessage =
        new StringBuilder(aggregationJobProcessException.toString()).append(" \n ");
    expectedReturnMessage.append(
        String.join(
            " \n ",
            Arrays.stream(stackTrace)
                .limit(NUMBER_OF_STACK_TRACE_LINES)
                .map(element -> element.toString())
                .collect(ImmutableList.toImmutableList())));
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(ErrorSummary.getDefaultInstance())
                    .setReturnCode(AggregationWorkerReturnCode.INTERNAL_ERROR.name())
                    .setReturnMessage(expectedReturnMessage.toString())
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void getJobResult_withErrorCodeDebugSuccessWithPrivacyBudgetError() {
    // Arrange
    ErrorSummary errorSummary = ErrorSummary.getDefaultInstance();

    // Act
    JobResult actualJobResult =
        jobResultHelper.createJobResult(
            job,
            errorSummary,
            AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR,
            Optional.empty());

    // Assert
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(errorSummary)
                    .setReturnCode(
                        AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR.name())
                    .setReturnMessage(RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR_MESSAGE)
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void getJobResult_withErrorCodeDebugSuccessWithPrivacyBudgetExhausted() {
    // Arrange
    ErrorSummary errorSummary = ErrorSummary.getDefaultInstance();

    // Act
    JobResult actualJobResult =
        jobResultHelper.createJobResult(
            job,
            errorSummary,
            AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED,
            Optional.empty());

    // Assert
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(errorSummary)
                    .setReturnCode(
                        AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED
                            .name())
                    .setReturnMessage(RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED_MESSAGE)
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void getJobResult_withUnsupportedReturnCode() {
    // Arrange
    ErrorSummary errorSummary = ErrorSummary.getDefaultInstance();

    // Act
    JobResult actualJobResult =
        jobResultHelper.createJobResult(
            job, errorSummary, AggregationWorkerReturnCode.INVALID_JOB, Optional.empty());

    // Assert
    JobResult expectedJobResult =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setErrorSummary(errorSummary)
                    .setReturnCode(AggregationWorkerReturnCode.INVALID_JOB.name())
                    .setReturnMessage(RESULT_SUCCESS_MESSAGE)
                    .setFinishedAt(TIMESTAMP)
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }
}
