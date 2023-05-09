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

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import javax.inject.Inject;

/** Creates {@code JobResult} for the run job */
public final class JobResultHelper {

  public static final String RESULT_SUCCESS_MESSAGE = "Aggregation job successfully processed";

  public static final String RESULT_SUCCESS_WITH_ERRORS_MESSAGE =
      "Aggregation job successfully processed but some reports have errors.";

  private final boolean returnStackTraceInResponse;

  private final int maxDepthOfStackTrace;

  private final Clock clock;

  @Inject
  JobResultHelper(
      Clock clock,
      @EnableStackTraceInResponse boolean returnStackTraceInResponse,
      @MaxDepthOfStackTrace int maxDepthOfStackTrace) {
    this.clock = clock;
    this.returnStackTraceInResponse = returnStackTraceInResponse;
    this.maxDepthOfStackTrace = maxDepthOfStackTrace;
  }

  /**
   * Returns {@code JobResult} for completion if a job. The return code is SUCCESS or
   * SUCCESS_WITH_ERRORS depending on success or partial success with errors in reports.
   *
   * @param job Current job
   * @param errorSummary {@Code ErrorSummary} object of the errors in report
   * @param successCode Code to return when the ErrorSummary is empty, and job is otherwise
   *     successful
   */
  public JobResult createJobResultOnCompletion(
      Job job, ErrorSummary errorSummary, AggregationWorkerReturnCode successCode) {
    ResultInfo.Builder resultInfoBuilder =
        ResultInfo.newBuilder()
            .setErrorSummary(errorSummary)
            .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)));

    if (errorSummary.getErrorCountsList().isEmpty()) {
      resultInfoBuilder.setReturnCode(successCode.name()).setReturnMessage(RESULT_SUCCESS_MESSAGE);
    } else {
      resultInfoBuilder
          .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
          .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE);
    }

    return JobResult.builder()
        .setJobKey(job.jobKey())
        .setResultInfo(resultInfoBuilder.build())
        .build();
  }

  /**
   * Returns {@code JobResult} for completion if a job. The return code is SUCCESS or
   * SUCCESS_WITH_ERRORS depending on success or partial success with errors in reports.
   *
   * @param job Current job
   * @param errorSummary {@Code ErrorSummary} object of the errors in report
   */
  public JobResult createJobResultOnCompletion(Job job, ErrorSummary errorSummary) {
    return createJobResultOnCompletion(job, errorSummary, AggregationWorkerReturnCode.SUCCESS);
  }

  /**
   * Returns {@code JobResult} for a job that threw an Exception. Error stackTrace is attached to
   * the return message if enabled.
   *
   * @param job current job
   * @param exception the thrown AggregationJobProcessException instance.
   */
  public JobResult createJobResultOnException(Job job, AggregationJobProcessException exception) {
    return JobResult.builder()
        .setJobKey(job.jobKey())
        .setResultInfo(
            ResultInfo.newBuilder()
                .setReturnMessage(getDetailedExceptionMessage(exception))
                .setReturnCode(exception.getCode().name())
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                .build())
        .build();
  }

  /**
   * Returns a string containing the throwable.toString() value followed by stacktrace of throwable.
   * If returnStackTraceInResponse is not enabled, it returns the throwable.toString() value.
   */
  private String getDetailedExceptionMessage(Throwable throwable) {
    StringBuilder builder = new StringBuilder();
    builder.append(throwable);
    if (returnStackTraceInResponse) {
      StackTraceElement[] stackTrace = throwable.getStackTrace();
      if (stackTrace != null) {
        for (int i = 0; i < Integer.min(maxDepthOfStackTrace, stackTrace.length); i++) {
          builder.append(" \n ").append(stackTrace[i]);
        }
      }
    }
    return builder.toString();
  }
}
