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

package com.google.aggregate.adtech.worker.aggregation.concurrent;

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHENTICATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHORIZATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.SUCCESS;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.DontConsumeBudgetInDebugRunEnabled;
import com.google.aggregate.adtech.worker.Annotations.ReportErrorThresholdPercentage;
import com.google.aggregate.adtech.worker.Annotations.StreamingOutputDomainProcessing;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngineFactory;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.adtech.worker.model.AggregatableInputBudgetConsumptionInfo;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetUnit;
import com.google.aggregate.adtech.worker.util.DebugSupportHelper;
import com.google.aggregate.adtech.worker.util.JobResultHelper;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.adtech.worker.util.ReportingOriginUtils;
import com.google.aggregate.adtech.worker.util.ReportingOriginUtils.InvalidReportingOriginException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetingServiceBridgeException;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParamsFactory;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.model.AggregatedResults;
import com.google.aggregate.privacy.noise.model.SummaryReportAvro;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.privacysandbox.otel.OTelConfiguration;
import com.google.privacysandbox.otel.Timer;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Processor which uses simple in-memory aggregation. */
public final class ConcurrentAggregationProcessor implements JobProcessor {

  // Key for user provided debug epsilon value in the job params of the job request.
  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";
  // Key to indicate whether this is a debug job
  public static final String JOB_PARAM_DEBUG_RUN = "debug_run";
  // Key for user provided reporting site value in the job params of the job request.
  public static final String JOB_PARAM_REPORTING_SITE = "reporting_site";

  public static final String PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE =
      "Insufficient privacy budget for one or more aggregatable reports. No aggregatable report can"
          + " appear in more than one aggregation job. Information related to reports that do not"
          + " have budget can be found in the following file:\n"
          + " File path: %s \n"
          + " Filename: %s \n";

  public static final String PRIVACY_BUDGET_EXHAUSTED_DEBUGGING_INFO_FILENAME_PREFIX =
      "privacy_budget_exhausted_debugging_information_";
  private static final Logger logger =
      LoggerFactory.getLogger(ConcurrentAggregationProcessor.class);

  private final AggregationEngineFactory aggregationEngineFactory;
  private final OutputDomainProcessor outputDomainProcessor;
  private final NoisedAggregationRunner noisedAggregationRunner;
  private final ResultLogger resultLogger;
  private final JobResultHelper jobResultHelper;
  private final JobScopedPrivacyParamsFactory privacyParamsFactory;
  private final BlobStorageClient blobStorageClient;
  private final StopwatchRegistry stopwatches;
  private final PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge;
  private final OTelConfiguration oTelConfiguration;
  private final Boolean streamingOutputDomainProcessing;
  private final boolean dontConsumeBudgetInDebugRunEnabled;
  private final ReportAggregator reportAggregator;
  private final double defaultReportErrorThresholdPercentage;

  @Inject
  ConcurrentAggregationProcessor(
      AggregationEngineFactory aggregationEngineFactory,
      OutputDomainProcessor outputDomainProcessor,
      NoisedAggregationRunner noisedAggregationRunner,
      ResultLogger resultLogger,
      BlobStorageClient blobStorageClient,
      StopwatchRegistry stopwatches,
      PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge,
      OTelConfiguration oTelConfiguration,
      JobResultHelper jobResultHelper,
      JobScopedPrivacyParamsFactory privacyParamsFactory,
      @ReportErrorThresholdPercentage double defaultReportErrorThresholdPercentage,
      @StreamingOutputDomainProcessing Boolean streamingOutputDomainProcessing,
      @DontConsumeBudgetInDebugRunEnabled boolean dontConsumeBudgetInDebugRunEnabled,
      ReportAggregator reportAggregator) {
    this.aggregationEngineFactory = aggregationEngineFactory;
    this.outputDomainProcessor = outputDomainProcessor;
    this.noisedAggregationRunner = noisedAggregationRunner;
    this.resultLogger = resultLogger;
    this.blobStorageClient = blobStorageClient;
    this.stopwatches = stopwatches;
    this.privacyBudgetingServiceBridge = privacyBudgetingServiceBridge;
    this.jobResultHelper = jobResultHelper;
    this.privacyParamsFactory = privacyParamsFactory;
    this.oTelConfiguration = oTelConfiguration;
    this.defaultReportErrorThresholdPercentage = defaultReportErrorThresholdPercentage;
    this.streamingOutputDomainProcessing = streamingOutputDomainProcessing;
    this.dontConsumeBudgetInDebugRunEnabled = dontConsumeBudgetInDebugRunEnabled;
    this.reportAggregator = reportAggregator;
  }

  /** Processor responsible for performing aggregation. */
  @Override
  public JobResult process(Job job)
      throws ExecutionException, InterruptedException, AggregationJobProcessException {
    Stopwatch processingStopwatch =
        stopwatches.createStopwatch("concurrent-" + toJobKeyString(job.jobKey()));
    processingStopwatch.start();

    final Boolean debugRun = DebugSupportHelper.isDebugRun(job);
    JobScopedPrivacyParams privacyParams = privacyParamsFactory.fromRequestInfo(job.requestInfo());
    final String jobKey = toJobKeyString(job.jobKey());

    Optional<DataLocation> outputDomainLocation = Optional.empty();
    Map<String, String> jobParams = job.requestInfo().getJobParametersMap();

    if (jobParams.containsKey(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME)
        && jobParams.containsKey(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX)
        && (!jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME).isEmpty()
            || !jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX).isEmpty())) {
      outputDomainLocation =
          Optional.of(
              BlobStorageClient.getDataLocation(
                  jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME),
                  jobParams.get(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX)));
    }

    try {
      // Reading the output domain early before report processing so that we can fail early if there
      // is a problem with output domains.
      ImmutableList<DataLocation> outputDomainShards =
          outputDomainLocation.map(outputDomainProcessor::listShards).orElse(ImmutableList.of());

      if (outputDomainLocation.isPresent() && outputDomainShards.isEmpty()) {
        throw new DomainReadException(
            new IllegalArgumentException(
                "No output domain shards found for location: " + outputDomainLocation));
      }
      ImmutableSet<UnsignedLong> filteringIds = JobUtils.getFilteringIdsFromJobOrDefault(job);

      AggregationEngine aggregationEngine =
          aggregationEngineFactory.createKeyAggregationEngine(filteringIds);
      double reportErrorThresholdPercentage =
          JobUtils.getReportErrorThresholdPercentage(
              jobParams, defaultReportErrorThresholdPercentage);
      ErrorSummaryAggregator errorAggregator =
          ErrorSummaryAggregator.createErrorSummaryAggregator(
              JobUtils.getInputReportCountFromJobParams(jobParams), reportErrorThresholdPercentage);

      AtomicLong totalReportCount = new AtomicLong(0);
      try (Timer reportsProcessTimer =
          oTelConfiguration.createDebugTimerStarted("reports_process_time", jobKey)) {
        // This function would add reports to aggregationEngine or errorAggregator.
        reportAggregator.processReports(totalReportCount, job, aggregationEngine, errorAggregator);
      }

      ErrorSummary errorSummary = errorAggregator.createErrorSummary();

      if (errorAggregator.countsAboveThreshold(totalReportCount.get())) {
        processingStopwatch.stop();
        return jobResultHelper.createJobResult(
            job,
            errorSummary,
            AggregationWorkerReturnCode.REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD,
            Optional.of(RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE));
      }

      AggregatedResults aggregatedResults;
      try {
        aggregatedResults =
            conflateWithDomainAndAddNoiseStreaming(
                outputDomainLocation,
                outputDomainShards,
                aggregationEngine,
                privacyParams,
                debugRun);
      } catch (DomainReadException e) {
        throw new AggregationJobProcessException(
            INPUT_DATA_READ_FAILED, "Exception while reading domain input data.", e.getCause());
      }

      processingStopwatch.stop();

      AggregationWorkerReturnCode jobCode = SUCCESS;
      if (debugRun) {
        if (!dontConsumeBudgetInDebugRunEnabled) {
          try {
            consumePrivacyBudgetUnits(aggregationEngine, job);
          } catch (AggregationJobProcessException e) {
            jobCode = AggregationWorkerReturnCode.getDebugEquivalent(e.getCode());
          }
        }

        logResults(aggregatedResults, job, /* isDebugRun= */ true);
      } else {
        consumePrivacyBudgetUnits(aggregationEngine, job);
      }

      // Log summary results
      try (Timer t = oTelConfiguration.createDebugTimerStarted("summary_write_time", jobKey)) {
        logResults(aggregatedResults, job, /* isDebugRun= */ false);
      }

      return jobResultHelper.createJobResult(
          job, errorSummary, jobCode, /* message= */ Optional.empty());
    } catch (RuntimeException e) {
      throw AggregationJobProcessException.createFromRuntimeException(e);
    }
  }

  private void logResults(AggregatedResults aggregatedResults, Job ctx, boolean isDebugRun) {
    // Only one of noisedAggregationResultSet (partial RxJava-based stream domain processing) or
    // summaryReportAvroSet(full RxJava-based stream domain processing) will be present, with the
    // full stream processing enabled by default.
    aggregatedResults
        .noisedAggregatedResultSet()
        .ifPresent(
            noisedAggregatedResultSet -> {
              ImmutableList<AggregatedFact> noisedFacts =
                  isDebugRun
                      ? noisedAggregatedResultSet.noisedDebugResult().get().noisedAggregatedFacts()
                      : noisedAggregatedResultSet.noisedResult().noisedAggregatedFacts();

              resultLogger.logResults(noisedFacts, ctx, isDebugRun);
            });

    aggregatedResults
        .summaryReportAvroSet()
        .ifPresent(
            summaryReportAvroSet -> {
              ImmutableList<SummaryReportAvro> summaryReportAvros =
                  isDebugRun
                      ? summaryReportAvroSet.debugSummaryReport().get()
                      : summaryReportAvroSet.summaryReports();

              resultLogger.logResultsAvros(summaryReportAvros, ctx, isDebugRun);
            });
  }

  private AggregatedResults conflateWithDomainAndAddNoiseStreaming(
      Optional<DataLocation> outputDomainLocation,
      ImmutableList<DataLocation> outputDomainShards,
      AggregationEngine engine,
      JobScopedPrivacyParams privacyParams,
      Boolean debugRun)
      throws DomainReadException {
    if (streamingOutputDomainProcessing) {
      return outputDomainProcessor.adjustAggregationWithDomainAndNoiseStreaming(
          engine,
          outputDomainLocation,
          outputDomainShards,
          noisedAggregationRunner,
          privacyParams,
          debugRun);
    }

    return outputDomainProcessor.adjustAggregationWithDomainAndNoise(
        engine,
        outputDomainLocation,
        outputDomainShards,
        noisedAggregationRunner,
        privacyParams,
        debugRun);
  }

  private void consumePrivacyBudgetUnits(AggregationEngine aggregationEngine, Job job)
      throws AggregationJobProcessException {
    ImmutableList<PrivacyBudgetUnit> budgetsToConsume = aggregationEngine.getPrivacyBudgetUnits();

    // Only send request to PBS if there are units to consume budget for; the list of units
    // can be empty if all reports failed decryption.
    if (budgetsToConsume.isEmpty()) {
      return;
    }

    String claimedIdentity;
    // Validations ensure that at least one of the parameters will always exist.
    if (job.requestInfo().getJobParametersMap().containsKey(JOB_PARAM_REPORTING_SITE)) {
      claimedIdentity = job.requestInfo().getJobParametersMap().get(JOB_PARAM_REPORTING_SITE);
    } else {
      try {
        claimedIdentity =
            ReportingOriginUtils.convertReportingOriginToSite(
                job.requestInfo().getJobParametersMap().get(JOB_PARAM_ATTRIBUTION_REPORT_TO));
      } catch (InvalidReportingOriginException e) {
        // This should never happen due to validations ensuring that the reporting origin is always
        // valid.
        throw new IllegalStateException(
            "Invalid reporting origin found while consuming budget, this should not happen as job"
                + " validations ensure the reporting origin is always valid.",
            e);
      }
    }

    ImmutableList<PrivacyBudgetUnit> missingPrivacyBudgetUnits;
    try {
      try (Timer t =
          oTelConfiguration.createDebugTimerStarted("pbs_latency", toJobKeyString(job.jobKey()))) {
        missingPrivacyBudgetUnits =
            privacyBudgetingServiceBridge.consumePrivacyBudget(budgetsToConsume, claimedIdentity);
      }
    } catch (PrivacyBudgetingServiceBridgeException e) {
      if (e.getStatusCode() != null) {
        switch (e.getStatusCode()) {
          case PRIVACY_BUDGET_CLIENT_UNAUTHENTICATED:
            throw new AggregationJobProcessException(
                PRIVACY_BUDGET_AUTHENTICATION_ERROR,
                "Aggregation service is not authenticated to call privacy budget service. This"
                    + " could happen due to a misconfiguration during enrollment. Please contact"
                    + " support for resolution.",
                e);
          case PRIVACY_BUDGET_CLIENT_UNAUTHORIZED:
            throw new AggregationJobProcessException(
                PRIVACY_BUDGET_AUTHORIZATION_ERROR,
                "Aggregation service is not authorized to call privacy budget service. This could"
                    + " happen if the createJob API job_paramaters.attribution_report_to does not"
                    + " match the one registered at enrollment. Please verify and contact support"
                    + " if needed.",
                e);
          default:
            break;
        }
      }

      String nestedMessage = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
      throw new AggregationJobProcessException(
          PRIVACY_BUDGET_ERROR,
          String.format(
              "Exception while consuming privacy budget. Exception message: %s", nestedMessage),
          e);
    }

    if (!missingPrivacyBudgetUnits.isEmpty()) {
      ImmutableList<PrivacyBudgetKeyInput> exhaustedPrivacyBudgetKeyInputs =
          aggregationEngine.getPrivacyBudgetKeyInputsFromPrivacyBudgetUnits(
              missingPrivacyBudgetUnits);
      ImmutableSet<AggregatableInputBudgetConsumptionInfo>
          aggregatableInputBudgetConsumptionInfoSet =
              exhaustedPrivacyBudgetKeyInputs.stream()
                  .map(
                      input ->
                          AggregatableInputBudgetConsumptionInfo.builder()
                              .setPrivacyBudgetKeyInput(input)
                              .build())
                  .collect(ImmutableSet.toImmutableSet());
      PrivacyBudgetExhaustedInfo privacyBudgetExhaustedDebuggingInfo =
          PrivacyBudgetExhaustedInfo.builder()
              .setAggregatableInputBudgetConsumptionInfos(aggregatableInputBudgetConsumptionInfoSet)
              .build();

      String privacyBudgetExhaustedDebugInfoFilename =
          PRIVACY_BUDGET_EXHAUSTED_DEBUGGING_INFO_FILENAME_PREFIX + job.createTime() + ".json";
      String privacyBudgetExhaustedDebugInfoFileBucketAndPrefix =
          resultLogger.writePrivacyBudgetExhaustedDebuggingInformation(
              privacyBudgetExhaustedDebuggingInfo, job, privacyBudgetExhaustedDebugInfoFilename);
      String privacyBudgetExhaustedDebugInfoFilePath =
          Paths.get(privacyBudgetExhaustedDebugInfoFileBucketAndPrefix).getParent().toString();

      throw new AggregationJobProcessException(
          PRIVACY_BUDGET_EXHAUSTED,
          String.format(
              PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE,
              privacyBudgetExhaustedDebugInfoFilePath,
              privacyBudgetExhaustedDebugInfoFilename));
    }
  }
}
