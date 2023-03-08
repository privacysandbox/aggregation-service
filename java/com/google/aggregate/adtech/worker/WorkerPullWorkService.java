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

package com.google.aggregate.adtech.worker;

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.validation.JobValidator;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.StopwatchExporter.StopwatchExportException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.jobclient.JobClient;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.metricclient.MetricClient;
import com.google.scp.operator.cpio.metricclient.MetricClient.MetricClientException;
import com.google.scp.operator.cpio.metricclient.model.CustomMetric;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Guava service for repeatedly pulling from the pubsub and processing the request */
public final class WorkerPullWorkService extends AbstractExecutionThreadService {

  private static final Logger logger = LoggerFactory.getLogger(WorkerPullWorkService.class);

  private final JobClient jobClient;
  private final JobProcessor jobProcessor;
  private final MetricClient metricClient;
  private final StopwatchRegistry stopwatchRegistry;
  private final StopwatchExporter stopwatchExporter;
  private final boolean benchmarkMode;
  private final Clock clock;

  private final ListeningExecutorService nonBlockingThreadPool;
  private final ListeningExecutorService blockingThreadPool;

  // Tracks whether the service should be pulling more jobs. Once the shutdown of the service
  // is initiated, this is switched to false.
  private volatile boolean moreNewRequests;

  private static final String METRIC_NAMESPACE = "scp/worker";
  private static final String JOB_ERROR_METRIC_NAME = "WorkerJobError";
  private static final String JOB_COMPLETION_METRIC_NAME = "WorkerJobCompletion";

  @Inject
  WorkerPullWorkService(
      JobClient jobClient,
      JobProcessor jobProcessor,
      MetricClient metricClient,
      Clock clock,
      StopwatchRegistry stopwatchRegistry,
      StopwatchExporter stopwatchExporter,
      @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @BenchmarkMode boolean benchmarkMode) {
    this.jobClient = jobClient;
    this.jobProcessor = jobProcessor;
    this.metricClient = metricClient;
    this.moreNewRequests = true;
    this.clock = clock;
    this.stopwatchRegistry = stopwatchRegistry;
    this.stopwatchExporter = stopwatchExporter;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.blockingThreadPool = blockingThreadPool;
    this.benchmarkMode = benchmarkMode;
  }

  // TODO(b/271323750) Implement unit tests
  @Override
  protected void run() {
    logger.info("Aggregation worker started");

    while (moreNewRequests) {
      Optional<Job> job = Optional.empty();
      try {
        job = jobClient.getJob();
        if (job.isEmpty()) {
          logger.info("No job pulled.");

          // If the jobhandler could not pull any new jobs, stop polling.
          // Note that jobhandler has an internal backoff mechanism.
          moreNewRequests = false;
          continue;
        }

        logger.info("Item pulled");

        boolean validated = JobValidator.validate(job);

        if (!validated) {
          JobResult jobErrorResult =
              JobResult.builder()
                  .setResultInfo(
                      ResultInfo.newBuilder()
                          .setReturnCode(INVALID_JOB.name())
                          .setReturnMessage(
                              String.format(
                                  "Job '%s' does not have an attribution_report_to field.",
                                  toJobKeyString(job.get().jobKey())))
                          .setErrorSummary(ErrorSummary.getDefaultInstance())
                          .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now()))
                          .build())
                  .build();
          jobClient.markJobCompleted(jobErrorResult);

          try {
            CustomMetric metric =
                CustomMetric.builder()
                    .setNameSpace(METRIC_NAMESPACE)
                    .setName("JobValidationFailure")
                    .setValue(1.0)
                    .setUnit("Count")
                    .addLabel("Validator", JobValidator.class.getSimpleName())
                    .build();
            metricClient.recordMetric(metric);
          } catch (MetricClientException e) {
            logger.warn(String.format("Could not record JobValidationFailure metric.\n%s", e));
          }

          continue;
        }

        JobResult jobResult = jobProcessor.process(job.get());

        jobClient.markJobCompleted(jobResult);
        recordWorkerJobMetric(JOB_COMPLETION_METRIC_NAME, "Success");
      } catch (AggregationJobProcessException e) {
        processAggregationJobProcessException(e, jobClient, job.get());
      } catch (Exception e) {
        processException(e, jobClient, job.orElse(null));
      }
      // Stopwatches only get exported once this loop exits. When run in benchmark mode (for perf
      // tests), we only expect one worker item.
      if (benchmarkMode) {
        break;
      }
    }

    try {
      stopwatchExporter.export(stopwatchRegistry);
    } catch (StopwatchExportException e) {
      throw new IllegalStateException("Stopwatches not exported", e);
    }

    nonBlockingThreadPool.shutdownNow();
    blockingThreadPool.shutdownNow();
  }

  @Override
  protected void triggerShutdown() {
    moreNewRequests = false;
  }

  private void recordWorkerJobMetric(String metricName, String type) {
    try {
      CustomMetric metric =
          CustomMetric.builder()
              .setNameSpace(METRIC_NAMESPACE)
              .setName(metricName)
              .setValue(1.0)
              .setUnit("Count")
              .addLabel("Type", type)
              .build();
      metricClient.recordMetric(metric);
    } catch (Exception e) {
      logger.warn(String.format("Could not record job metric %s.\n%s", metricName, e));
    }
  }

  private JobResult createErrorJobResult(Job job, String errorCode, String errorMessage) {
    return JobResult.builder()
        .setJobKey(job.jobKey())
        .setResultInfo(
            ResultInfo.newBuilder()
                .setReturnMessage(errorMessage)
                .setReturnCode(errorCode)
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                .build())
        .build();
  }

  private void processAggregationJobProcessException(
      AggregationJobProcessException e, JobClient jobClient, Job job) {
    logger.error("Exception while running job :", e);
    try {
      JobResult jobResult = createErrorJobResult(job, e.getCode().name(), e.getMessage());
      jobClient.markJobCompleted(jobResult);
      recordWorkerJobMetric(JOB_COMPLETION_METRIC_NAME, "Success");
    } catch (Exception ex) {
      logger.error("Exception while processing AggregationJobProcessException :", ex.getMessage());
      processException(ex, jobClient, job);
    }
  }

  /**
   * Called when an unexpected exception occurs during job processing. Adds the exception's message
   * to the job.
   *
   * @param e Unexpected exception whose message is to be saved
   * @param jobClient JobClient that processed the job
   * @param job Job that threw the exception when run
   */
  private void processException(Exception e, JobClient jobClient, Job job) {
    logger.error(
        String.format("%s caught in WorkerPullWorkService: ", e.getClass().getSimpleName()), e);
    try {
      recordWorkerJobMetric(JOB_ERROR_METRIC_NAME, "JobHandlingError");
      jobClient.appendJobErrorMessage(job.jobKey(), e.getMessage());
    } catch (Exception ex) {
      logger.error(
          String.format(
              "%s caught in WorkerPullWorkService when processing an exception: ",
              ex.getClass().getSimpleName()),
          ex);
    }
  }
}
