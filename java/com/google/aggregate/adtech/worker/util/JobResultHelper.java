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
import java.util.Optional;
import javax.inject.Inject;

/** Creates {@code JobResult} for the run job */
public final class JobResultHelper {

  public static final String RESULT_SUCCESS_MESSAGE = "Aggregation job successfully processed";

  public static final String RESULT_SUCCESS_WITH_ERRORS_MESSAGE =
      "Aggregation job successfully processed but some reports have errors.";

  public static final String RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR_MESSAGE =
      "Aggregation would have failed in non-debug mode due to a privacy budget error.";

  public static final String RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED_MESSAGE =
      "Aggregation would have failed in non-debug mode due to privacy budget exhaustion";

  public static final String RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE =
      "Aggregation job failed early because the number of reports excluded from aggregation"
          + " exceeded threshold.";

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
   * Returns {@code JobResult} for a job that threw an AggregationJobProcessException. Error
   * stackTrace is attached to the return message if enabled.
   *
   * @param job current job
   * @param exception the thrown AggregationJobProcessException instance.
   */
  public JobResult createJobResultOnException(Job job, AggregationJobProcessException exception) {
    return buildJobResult(
        job,
        ErrorSummary.getDefaultInstance(),
        exception.getCode().name(),
        getDetailedExceptionMessage(exception));
  }

  /**
   * Returns JobResult with SUCCESS return code and default message for success. If errors are
   * present within ErrorSummary, it will return SUCCESS_WITH_ERRORS and its corresponding message
   * instead.
   *
   * @param job current job
   * @param errorSummary error summary of the job
   */
  public JobResult createJobResult(Job job, ErrorSummary errorSummary) {
    return createJobResult(
        job,
        errorSummary,
        /* code= */ AggregationWorkerReturnCode.SUCCESS,
        /* message= */ Optional.empty());
  }

  /**
   * Returns {@code JobResult} upon job completion. Determines the correct type of result based on
   * parameters. Message parameter can only be specified if code parameter is as well.
   *
   * @param job current job
   * @param errorSummary error summary of the job
   * @param code return code to set in the JobResult
   * @param message Optional message to override the default one that corresponds to the return
   *     code.
   */
  public JobResult createJobResult(
      Job job,
      ErrorSummary errorSummary,
      AggregationWorkerReturnCode code,
      Optional<String> message) {

    if (code.equals(AggregationWorkerReturnCode.SUCCESS)
        && !errorSummary.getErrorCountsList().isEmpty()) {
      code = AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS;
    }

    AggregationWorkerReturnCode finalCode = code;
    String returnMessage = message.orElseGet(() -> getCorrespondingMessage(finalCode));

    return buildJobResult(job, errorSummary, code.name(), returnMessage);
  }

  private String getCorrespondingMessage(AggregationWorkerReturnCode code) {
    switch (code) {
      case SUCCESS_WITH_ERRORS:
        return RESULT_SUCCESS_WITH_ERRORS_MESSAGE;
      case DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR:
        return RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR_MESSAGE;
      case DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED:
        return RESULT_DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED_MESSAGE;
      case SUCCESS:
      default:
        return RESULT_SUCCESS_MESSAGE;
    }
  }

  /**
   * Returns {@code JobResult} for completion of a job. The return returnCode is set based on the
   * parameter, due to different possible codes based on the run parameters and result of the
   * aggregation.
   *
   * @param job Current job
   * @param errorSummary {@code ErrorSummary} object of the errors in report
   * @param returnCode {@code AggregationWorkerReturnCode} to set as the return code
   * @param returnMessage String to set as the return message
   */
  private JobResult buildJobResult(
      Job job, ErrorSummary errorSummary, String returnCode, String returnMessage) {
    ResultInfo resultInfo =
        ResultInfo.newBuilder()
            .setReturnCode(returnCode)
            .setReturnMessage(returnMessage)
            .setErrorSummary(errorSummary)
            .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
            .build();

    return JobResult.builder().setJobKey(job.jobKey()).setResultInfo(resultInfo).build();
  }

  /**
   * Returns a string containing the throwable.toString() value followed by stacktrace of throwable.
   * If returnStackTraceInResponse is not enabled, it returns the throwable.toString() value.
   */
  public String getDetailedExceptionMessage(Throwable throwable) {
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
