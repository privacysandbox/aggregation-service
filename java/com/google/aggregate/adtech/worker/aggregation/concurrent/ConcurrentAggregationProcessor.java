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
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INTERNAL_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PERMISSION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHENTICATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHORIZATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.RESULT_WRITE_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.SUCCESS;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.UNSUPPORTED_REPORT_VERSION;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.ReportErrorThresholdPercentage;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.ReportDecrypterAndValidator;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.ConcurrentShardReadException;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.adtech.worker.exceptions.InternalServerException;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.util.DebugSupportHelper;
import com.google.aggregate.adtech.worker.util.JobResultHelper;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.validation.ValidationException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetingServiceBridgeException;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.privacysandbox.otel.OTelConfiguration;
import com.google.privacysandbox.otel.Timer;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.avro.AvroRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Processor which uses simple in-memory aggregation. */
public final class ConcurrentAggregationProcessor implements JobProcessor {

  // Key for user provided debug epsilon value in the job params of the job request.
  public static final String JOB_PARAM_DEBUG_PRIVACY_EPSILON = "debug_privacy_epsilon";
  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";
  // Key to indicate whether this is a debug job
  public static final String JOB_PARAM_DEBUG_RUN = "debug_run";

  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();
  // In aggregation service, reading is much faster than decryption, and most of the time, it waits
  // for decryption to complete to continue reading. Therefore, set the number of read concurrent
  // thread to NUM_CPUS / 4.
  private static final int NUM_READ_THREADS = (int) Math.ceil((double) NUM_CPUS / 4);
  // Decryption is a CPU-bound operation so put more CPU resources here.
  private static final int NUM_PROCESS_THREADS = NUM_CPUS;

  // Buffer size for reading data on the same thread
  // TODO(b/279061816): Research on optimal value for MAX_REPORTS_READ_BUFFER_SIZE and
  // MAX_REPORTS_PROCESS_BUFFER_SIZE
  private final int MAX_REPORTS_READ_BUFFER_SIZE = 1000;
  // Buffer size for decrypting and aggregating data on the same thread
  private final int MAX_REPORTS_PROCESS_BUFFER_SIZE = 1000;

  public static final String PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE =
      "Insufficient privacy budget for one or more aggregatable reports. No aggregatable report can"
          + " appear in more than one aggregation job.";
  private static final Logger logger =
      LoggerFactory.getLogger(ConcurrentAggregationProcessor.class);

  private final ReportDecrypterAndValidator reportDecrypterAndValidator;
  private final Provider<AggregationEngine> engineProvider;
  private final OutputDomainProcessor outputDomainProcessor;
  private final NoisedAggregationRunner noisedAggregationRunner;
  private final ResultLogger resultLogger;
  private final JobResultHelper jobResultHelper;
  private final BlobStorageClient blobStorageClient;
  private final AvroReportsReaderFactory readerFactory;
  private final AvroRecordEncryptedReportConverter encryptedReportConverter;
  private final StopwatchRegistry stopwatches;
  private final PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge;
  private final ListeningExecutorService blockingThreadPool;
  private final ListeningExecutorService nonBlockingThreadPool;
  private final boolean domainOptional;
  private final boolean enableThresholding;
  private final OTelConfiguration oTelConfiguration;
  private final double defaultReportErrorThresholdPercentage;

  @Inject
  ConcurrentAggregationProcessor(
      ReportDecrypterAndValidator reportDecrypterAndValidator,
      Provider<AggregationEngine> engineProvider,
      OutputDomainProcessor outputDomainProcessor,
      NoisedAggregationRunner noisedAggregationRunner,
      ResultLogger resultLogger,
      BlobStorageClient blobStorageClient,
      AvroReportsReaderFactory readerFactory,
      AvroRecordEncryptedReportConverter encryptedReportConverter,
      StopwatchRegistry stopwatches,
      PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge,
      OTelConfiguration oTelConfiguration,
      JobResultHelper jobResultHelper,
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      @DomainOptional Boolean domainOptional,
      @EnableThresholding Boolean enableThresholding,
      @ReportErrorThresholdPercentage double defaultReportErrorThresholdPercentage) {
    this.reportDecrypterAndValidator = reportDecrypterAndValidator;
    this.engineProvider = engineProvider;
    this.outputDomainProcessor = outputDomainProcessor;
    this.noisedAggregationRunner = noisedAggregationRunner;
    this.resultLogger = resultLogger;
    this.blobStorageClient = blobStorageClient;
    this.readerFactory = readerFactory;
    this.encryptedReportConverter = encryptedReportConverter;
    this.stopwatches = stopwatches;
    this.privacyBudgetingServiceBridge = privacyBudgetingServiceBridge;
    this.jobResultHelper = jobResultHelper;
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.domainOptional = domainOptional;
    this.oTelConfiguration = oTelConfiguration;
    this.enableThresholding = enableThresholding;
    this.defaultReportErrorThresholdPercentage = defaultReportErrorThresholdPercentage;
  }

  /**
   * Processor responsible for performing aggregation. TODO: evaluate throwing unchecked exceptions
   * here.
   *
   * @param job
   * @return
   * @throws AggregationJobProcessException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @Override
  public JobResult process(Job job)
      throws ExecutionException, InterruptedException, AggregationJobProcessException {
    Stopwatch processingStopwatch =
        stopwatches.createStopwatch("concurrent-" + toJobKeyString(job.jobKey()));
    processingStopwatch.start();

    final Boolean debugRun = DebugSupportHelper.isDebugRun(job);
    final Optional<Double> debugPrivacyEpsilon = getPrivacyEpsilonForJob(job);
    final String jobKey = toJobKeyString(job.jobKey());

    if (debugPrivacyEpsilon.isPresent()) {
      Double privacyEpsilonForJob = debugPrivacyEpsilon.get();
      if (!(privacyEpsilonForJob > 0d && privacyEpsilonForJob <= 64d)) {
        throw new AggregationJobProcessException(
            INVALID_JOB,
            String.format("Failed Parsing Job parameters for %s", JOB_PARAM_DEBUG_PRIVACY_EPSILON));
      }
    }

    ListenableFuture<ImmutableSet<BigInteger>> outputDomainFuture;
    Optional<DataLocation> outputDomainLocation = Optional.empty();
    Map<String, String> jobParams = job.requestInfo().getJobParameters();

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

    ImmutableList<DataLocation> dataShards;

    try {
      DataLocation reportsLocation =
          BlobStorageClient.getDataLocation(
              job.requestInfo().getInputDataBucketName(),
              job.requestInfo().getInputDataBlobPrefix());
      dataShards = findShards(reportsLocation);

      if (dataShards.isEmpty()) {
        throw new AggregationJobProcessException(
            INPUT_DATA_READ_FAILED, "No report shards found for location: " + reportsLocation);
      }

      outputDomainFuture =
          outputDomainLocation
              .map(outputDomainProcessor::readAndDedupDomain)
              .orElse(immediateFuture(ImmutableSet.of()));
    } catch (ConcurrentShardReadException e) {
      throw new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "Exception while reading reports input data.", e);
    } catch (DomainReadException e) {
      throw new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "Exception while reading domain input data.", e);
    }

    try {
      double reportErrorThresholdPercentage = getReportErrorThresholdPercentage(jobParams);
      AggregationEngine aggregationEngine = engineProvider.get();
      // TODO(b/218924983) Estimate report counts to enable failing early on report errors reaching
      // threshold.
      ErrorSummaryAggregator errorAggregator =
          ErrorSummaryAggregator.createErrorSummaryAggregator(
              Optional.empty(), reportErrorThresholdPercentage);
      AtomicLong totalReportCount = new AtomicLong(0);

      try (Timer reportsProcessTimer =
          oTelConfiguration.createDebugTimerStarted("reports_process_time", jobKey)) {
        // This function would add reports to aggregationEngine or errorAggregator.
        processReports(dataShards, totalReportCount, job, aggregationEngine, errorAggregator);
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

      ImmutableMap<BigInteger, AggregatedFact> reportsAggregatedMap =
          aggregationEngine.makeAggregation();

      NoisedAggregatedResultSet noisedResultSet;
      ListenableFuture<NoisedAggregatedResultSet> aggregationFinalFuture =
          Futures.transform(
              outputDomainFuture,
              outputDomain ->
                  adjustAggregationWithDomainAndNoise(
                      outputDomain, reportsAggregatedMap, debugPrivacyEpsilon, debugRun),
              nonBlockingThreadPool);

      try {
        noisedResultSet = aggregationFinalFuture.get();
      } catch (ExecutionException e) {
        if ((e.getCause() instanceof DomainReadException)) {
          throw new AggregationJobProcessException(
              INPUT_DATA_READ_FAILED, "Exception while reading domain input data.", e.getCause());
        }
        throw new AggregationJobProcessException(
            INTERNAL_ERROR, "Exception in processing domain.", e);
      }

      processingStopwatch.stop();

      AggregationWorkerReturnCode jobCode = SUCCESS;
      if (debugRun) {
        try {
          consumePrivacyBudgetUnits(aggregationEngine.getPrivacyBudgetUnits(), job);
        } catch (AggregationJobProcessException e) {
          jobCode = AggregationWorkerReturnCode.getDebugEquivalent(e.getCode());
        }

        NoisedAggregationResult noisedDebugResult = noisedResultSet.noisedDebugResult().get();
        resultLogger.logResults(
            noisedDebugResult.noisedAggregatedFacts(), job, /* isDebugRun= */ true);
      } else {
        consumePrivacyBudgetUnits(aggregationEngine.getPrivacyBudgetUnits(), job);
      }

      // Log summary results
      try (Timer t = oTelConfiguration.createDebugTimerStarted("summary_write_time", jobKey)) {
        resultLogger.logResults(
            noisedResultSet.noisedResult().noisedAggregatedFacts(), job, /* isDebugRun= */ false);
      }

      return jobResultHelper.createJobResult(
          job, errorSummary, jobCode, /* message= */ Optional.empty());
    } catch (ResultLogException e) {
      throw new AggregationJobProcessException(
          RESULT_WRITE_ERROR, "Exception occured while writing result.", e);
    } catch (AccessControlException e) {
      throw new AggregationJobProcessException(
          PERMISSION_ERROR, "Exception because of missing permission.", e);
    } catch (InternalServerException e) {
      throw new AggregationJobProcessException(
          INTERNAL_ERROR, "Internal Service Exception when processing reports.", e);
    } catch (ConcurrentShardReadException e) {
      // TODO(b/197999001) report exception in some monitoring counter
      throw new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "Exception while reading reports input data.");
    } catch (ValidationException e) {
      if (e.getCode().equals(UNSUPPORTED_SHAREDINFO_VERSION)) {
        throw new AggregationJobProcessException(UNSUPPORTED_REPORT_VERSION, e.getMessage());
      } else {
        throw new AggregationJobProcessException(
            INVALID_JOB, "Error due to validation exception.", e);
      }
    }
  }

  private double getReportErrorThresholdPercentage(Map<String, String> jobParams) {
    String jobParamsReportErrorThresholdPercentage =
        jobParams.getOrDefault(JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, null);
    if (jobParamsReportErrorThresholdPercentage != null) {
      return NumericConversions.getPercentageValue(jobParamsReportErrorThresholdPercentage);
    }
    logger.info(
        String.format(
            "Job parameters didn't have a report error threshold configured. Taking the default"
                + " percentage value %f",
            defaultReportErrorThresholdPercentage));
    return defaultReportErrorThresholdPercentage;
  }

  private void consumePrivacyBudgetUnits(ImmutableList<PrivacyBudgetUnit> budgetsToConsume, Job job)
      throws AggregationJobProcessException {
    if (budgetsToConsume.isEmpty()) {
      return;
    }

    ImmutableList<PrivacyBudgetUnit> missingPrivacyBudgetUnits;

    try {
      // Only send request to PBS if there are units to consume budget for, the list of units
      // can be empty if all reports failed decryption
      try (Timer t =
          oTelConfiguration.createDebugTimerStarted("pbs_latency", toJobKeyString(job.jobKey()))) {
        missingPrivacyBudgetUnits =
            privacyBudgetingServiceBridge.consumePrivacyBudget(
                budgetsToConsume, /* budgetsToConsume */
                job.requestInfo() /* attributionReportTo */
                    .getJobParameters()
                    .get(JOB_PARAM_ATTRIBUTION_REPORT_TO));
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
      throw new AggregationJobProcessException(
          PRIVACY_BUDGET_EXHAUSTED, PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE);
    }
  }

  private ImmutableList<DataLocation> findShards(DataLocation reportsLocation) {
    try {
      ImmutableList<String> shardBlobs = blobStorageClient.listBlobs(reportsLocation);

      logger.info("Reports shards detected by blob storage client: " + shardBlobs);

      BlobStoreDataLocation blobsPrefixLocation = reportsLocation.blobStoreDataLocation();

      ImmutableList<DataLocation> shards =
          shardBlobs.stream()
              .map(shard -> BlobStoreDataLocation.create(blobsPrefixLocation.bucket(), shard))
              .map(DataLocation::ofBlobStoreDataLocation)
              .collect(toImmutableList());

      logger.info("Reports shards to be used: " + shards);

      return shards;
    } catch (BlobStorageClientException e) {
      throw new ConcurrentShardReadException(e);
    }
  }

  private NoisedAggregatedResultSet adjustAggregationWithDomainAndNoise(
      ImmutableSet<BigInteger> outputDomain,
      ImmutableMap<BigInteger, AggregatedFact> reportsAggregatedMap,
      Optional<Double> debugPrivacyEpsilon,
      Boolean debugRun) {
    // This pseudo-aggregation has all zeroes for the output domain. If a key is present in the
    // output domain, but not in the aggregation itself, a zero is inserted which will later be
    // noised to some value.
    ImmutableMap<BigInteger, AggregatedFact> outputDomainPseudoAggregation =
        outputDomain.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(), key -> AggregatedFact.create(key, /* metric= */ 0)));

    // Difference by key is computed so that the output can be adjusted for the output domain.
    // Keys that are in the aggregation data, but not in the output domain, are subject to both
    // noising and thresholding.
    // Otherwise, the data is subject to noising only.
    MapDifference<BigInteger, AggregatedFact> pseudoDiff =
        Maps.difference(reportsAggregatedMap, outputDomainPseudoAggregation);

    // The values for common keys should in theory be differing, since the pseudo aggregation will
    // have all zeroes, while the 'real' aggregation will have non-zeroes, but just in case to
    // cover overlapping zeroes, matching keys are also processed.
    // `overlappingZeroes` includes all the keys present in both domain and reports but
    // the values are 0.
    Iterable<AggregatedFact> overlappingZeroes = pseudoDiff.entriesInCommon().values();
    // `overlappingNonZeroes` includes all the keys present in both domain and reports, and the
    // value is non-zero in reports.
    Iterable<AggregatedFact> overlappingNonZeroes =
        Maps.transformValues(pseudoDiff.entriesDiffering(), ValueDifference::leftValue).values();
    // `domainOutputOnlyZeroes` only includes keys in domain.
    Iterable<AggregatedFact> domainOutputOnlyZeroes = pseudoDiff.entriesOnlyOnRight().values();

    NoisedAggregationResult noisedOverlappingNoThreshold =
        noisedAggregationRunner.noise(
            Iterables.concat(overlappingZeroes, overlappingNonZeroes),
            /* doThreshold= */ false,
            debugPrivacyEpsilon);

    NoisedAggregationResult noisedDomainOnlyNoThreshold =
        noisedAggregationRunner.noise(
            domainOutputOnlyZeroes, /* doThreshold= */ false, debugPrivacyEpsilon);

    NoisedAggregationResult noisedDomainNoThreshold =
        NoisedAggregationResult.merge(noisedOverlappingNoThreshold, noisedDomainOnlyNoThreshold);

    NoisedAggregatedResultSet.Builder noisedResultSetBuilder = NoisedAggregatedResultSet.builder();

    if (debugRun) {
      // Noise values for keys that are only in reports (not in domain) without thresholding
      NoisedAggregationResult noisedReportsOnlyNoThreshold =
          noisedAggregationRunner.noise(
              pseudoDiff.entriesOnlyOnLeft().values(),
              /* doThreshold= */ false,
              debugPrivacyEpsilon);

      NoisedAggregationResult noisedReportsOnlyNoThresholdWithAnno =
          NoisedAggregationResult.addDebugAnnotations(
              noisedReportsOnlyNoThreshold, List.of(DebugBucketAnnotation.IN_REPORTS));
      NoisedAggregationResult noisedDomainOnlyNoThresholdWithAnno =
          NoisedAggregationResult.addDebugAnnotations(
              noisedDomainOnlyNoThreshold, List.of(DebugBucketAnnotation.IN_DOMAIN));
      NoisedAggregationResult noisedOverlappingNoThresholdWithAnno =
          NoisedAggregationResult.addDebugAnnotations(
              noisedOverlappingNoThreshold,
              List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN));

      noisedResultSetBuilder.setNoisedDebugResult(
          NoisedAggregationResult.merge(
              noisedOverlappingNoThresholdWithAnno,
              NoisedAggregationResult.merge(
                  noisedReportsOnlyNoThresholdWithAnno, noisedDomainOnlyNoThresholdWithAnno)));
    }

    if (domainOptional) {
      NoisedAggregationResult noisedReportsDomainOptional =
          noisedAggregationRunner.noise(
              pseudoDiff.entriesOnlyOnLeft().values(), enableThresholding, debugPrivacyEpsilon);

      return noisedResultSetBuilder
          .setNoisedResult(
              NoisedAggregationResult.merge(noisedDomainNoThreshold, noisedReportsDomainOptional))
          .build();
    } else {
      return noisedResultSetBuilder.setNoisedResult(noisedDomainNoThreshold).build();
    }
  }

  private Flowable<EncryptedReport> readData(DataLocation shard) {
    return Flowable.using(
        () -> {
          try {
            return blobStorageClient.getBlob(shard);
          } catch (BlobStorageClientException e) {
            throw new ConcurrentShardReadException(e);
          }
        },
        inputStream -> Flowable.fromStream(readInputStream(inputStream)),
        InputStream::close);
  }

  private Stream<EncryptedReport> readInputStream(InputStream shardInputStream) {
    try {
      return readerFactory.create(shardInputStream).streamRecords().map(encryptedReportConverter);
    } catch (IOException | AvroRuntimeException e) {
      throw new ConcurrentShardReadException(e);
    }
  }

  private void processReports(
      ImmutableList<DataLocation> dataShards,
      AtomicLong totalReportCount,
      Job job,
      AggregationEngine aggregationEngine,
      ErrorSummaryAggregator errorAggregator) {
    Flowable.fromStream(dataShards.stream())
        // This would open connections with data and max concurrency is NUM_READ_THREADS.
        .flatMap(
            dataLocation -> readData(dataLocation).subscribeOn(Schedulers.from(blockingThreadPool)),
            false,
            NUM_READ_THREADS,
            MAX_REPORTS_READ_BUFFER_SIZE)
        // Specify the number of reports are grouped into a list.
        .buffer(MAX_REPORTS_PROCESS_BUFFER_SIZE)
        .doOnNext(encryptedReports -> totalReportCount.addAndGet(encryptedReports.size()))
        .flatMap(
            encryptedReportList ->
                Flowable.just(encryptedReportList)
                    .subscribeOn(Schedulers.from(nonBlockingThreadPool))
                    .map(
                        encryptedReports ->
                            decryptAndAggregateReports(
                                encryptedReports, job, aggregationEngine, errorAggregator)),
            NUM_PROCESS_THREADS)
        .blockingSubscribe();
  }

  private Observable decryptAndAggregateReports(
      List<EncryptedReport> reports,
      Job job,
      AggregationEngine aggregationEngine,
      ErrorSummaryAggregator errorAggregator) {
    reports.forEach(
        report -> {
          DecryptionValidationResult result;
          try (Timer t =
              oTelConfiguration.createDebugTimerStarted(
                  "decryption_time_per_report", toJobKeyString(job.jobKey()))) {
            result = reportDecrypterAndValidator.decryptAndValidate(report, job);
          }
          if (result.report().isPresent()) {
            aggregationEngine.accept(result.report().get());
          } else {
            errorAggregator.add(result);
          }
        });
    return Observable.empty();
  }

  /** Retrieve epsilon from nested optional fields */
  private Optional<Double> getPrivacyEpsilonForJob(Job job) {
    Optional<Double> epsilonValueFromJobReq = Optional.empty();
    try {
      if (job.requestInfo().getJobParameters().containsKey(JOB_PARAM_DEBUG_PRIVACY_EPSILON)) {
        epsilonValueFromJobReq =
            Optional.of(
                Double.parseDouble(
                    job.requestInfo().getJobParameters().get(JOB_PARAM_DEBUG_PRIVACY_EPSILON)));
      }
    } catch (NumberFormatException e) {
      logger.error(
          String.format("Failed Parsing Job parameters for %s", JOB_PARAM_DEBUG_PRIVACY_EPSILON),
          e);
    }
    return epsilonValueFromJobReq;
  }
}
