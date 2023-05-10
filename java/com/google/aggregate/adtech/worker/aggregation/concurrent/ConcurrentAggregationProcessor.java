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
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.RESULT_WRITE_ERROR;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.whenAllSucceed;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.ReportDecrypterAndValidator;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.ConcurrentShardReadException;
import com.google.aggregate.adtech.worker.exceptions.DomainProcessException;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.util.DebugSupportHelper;
import com.google.aggregate.adtech.worker.util.JobResultHelper;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetingServiceBridgeException;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.protocol.avro.AvroReportsReader;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
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

  // Limit on how many invalid reports are collected; if a batch has a lot of invalid records, not
  // all of them will necessarily be captured.
  private static final int MAX_INVALID_REPORTS_COLLECTED = 1000;

  public static final String PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE =
      "Insufficient privacy budget for one or more aggregatable reports. No aggregatable report can"
          + " appear in more than one batch or contribute to more than one summary report.";
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
  // Provider<Boolean> used so the value can be dynamically changed in tests

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
      JobResultHelper jobResultHelper,
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      @DomainOptional Boolean domainOptional,
      @EnableThresholding Boolean enableThresholding) {
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
    this.enableThresholding = enableThresholding;
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

    if (debugPrivacyEpsilon.isPresent()) {
      Double privacyEpsilonForJob = debugPrivacyEpsilon.get();
      if (!(privacyEpsilonForJob > 0d && privacyEpsilonForJob <= 64d)) {
        throw new AggregationJobProcessException(
            INVALID_JOB,
            String.format("Failed Parsing Job parameters for %s", JOB_PARAM_DEBUG_PRIVACY_EPSILON));
      }
    }

    ListenableFuture<ImmutableSet<BigInteger>> outputDomain;
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

      outputDomain =
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
      // List of futures, one for each data shard; a shard is read into a list of encrypted reports.
      ImmutableList<ListenableFuture<ImmutableList<EncryptedReport>>> shardReads =
          Streams.mapWithIndex(
                  dataShards.stream(),
                  (shard, shardIndex) -> readShardAsync(job, shard, shardIndex))
              .collect(toImmutableList());

      ImmutableList<ListenableFuture<ImmutableList<DecryptionValidationResult>>> decryptedShards =
          Streams.mapWithIndex(
                  shardReads.stream(),
                  (shard, shardIndex) -> decryptShardAsync(job, shard, shardIndex))
              .collect(toImmutableList());

      ImmutableList<ListenableFuture<ImmutableList<DecryptionValidationResult>>>
          invalidReportsPerShards =
              decryptedShards.stream()
                  .map(this::collectInvalidReportsAsync)
                  .collect(toImmutableList());

      ListenableFuture<List<ImmutableList<DecryptionValidationResult>>>
          invalidReportsPerShardUnified = Futures.successfulAsList(invalidReportsPerShards);

      // TODO(b/199208370): Aggregate errors if there are too many.
      ListenableFuture<ImmutableList<DecryptionValidationResult>> invalidReportsFuture =
          Futures.transform(
              invalidReportsPerShardUnified,
              invalidReportPerShardList ->
                  invalidReportPerShardList.stream()
                      .flatMap(ImmutableList::stream)
                      .limit(MAX_INVALID_REPORTS_COLLECTED)
                      .collect(toImmutableList()),
              nonBlockingThreadPool);

      AggregationEngine aggregationEngine = engineProvider.get();

      // List of futures for each decrypted shard: each future finishes when all the reports from
      // the
      // shard have been consumed by the aggregation engine.
      ImmutableList<ListenableFuture<Void>> shardAggregatedFutures =
          Streams.mapWithIndex(
                  decryptedShards.stream(),
                  (shard, shardIndex) -> aggregateShardAsync(aggregationEngine, shard, shardIndex))
              .collect(toImmutableList());

      // Combines the futures above to produce a future that completes when all reports from all
      // shards have been run through the aggregation engine.
      ListenableFuture<Void> aggregationCompletion =
          whenAllSucceed(shardAggregatedFutures).call(() -> null, directExecutor());

      ListenableFuture<ImmutableMap<BigInteger, AggregatedFact>> aggregationFuture =
          Futures.transform(
              aggregationCompletion,
              unused -> aggregationEngine.makeAggregation(),
              nonBlockingThreadPool);

      ListenableFuture<NoisedAggregatedResultSet> aggregationFinalFuture =
          Futures.whenAllSucceed(outputDomain, aggregationFuture)
              .call(
                  () ->
                      adjustAggregationWithDomainAndNoise(
                          outputDomain, aggregationFuture, debugPrivacyEpsilon, debugRun),
                  nonBlockingThreadPool);

      NoisedAggregatedResultSet noisedResultSet = aggregationFinalFuture.get();

      ImmutableList<DecryptionValidationResult> invalidReports = invalidReportsFuture.get();
      processingStopwatch.stop();

      // Create error summary from the list of errors from decryption/validation
      ErrorSummary errorSummary = ErrorSummaryAggregator.createErrorSummary(invalidReports);

      AggregationWorkerReturnCode defaultCode = AggregationWorkerReturnCode.SUCCESS;
      try {
        consumePrivacyBudgetUnits(aggregationEngine.getPrivacyBudgetUnits(), job);
      } catch (AggregationJobProcessException e) {
        if (debugRun) {
          defaultCode = AggregationWorkerReturnCode.getDebugEquivalent(e.getCode());
        } else {
          throw e;
        }
      }

      // Log debug results if it is debug-run job
      if (debugRun) {
        NoisedAggregationResult noisedDebugResult = noisedResultSet.noisedDebugResult().get();
        DataLocation debugLocation =
            resultLogger.logDebugResults(noisedDebugResult.noisedAggregatedFacts().stream(), job);
      }

      // Log summary results
      DataLocation dataLocation =
          resultLogger.logResults(
              noisedResultSet.noisedResult().noisedAggregatedFacts().stream(), job);

      return jobResultHelper.createJobResultOnCompletion(job, errorSummary, defaultCode);
    } catch (ResultLogException e) {
      throw new AggregationJobProcessException(
          RESULT_WRITE_ERROR, "Exception occured while writing result.", e);
    } catch (DomainProcessException e) {
      throw new AggregationJobProcessException(
          INTERNAL_ERROR, "Exception in processing domain.", e);
    } catch (AccessControlException e) {
      throw new AggregationJobProcessException(
          PERMISSION_ERROR, "Exception because of missing permission.", e);

    } catch (ExecutionException e) {
      // Error occurred in data read
      // TODO(b/197999001) report exception in some monitoring counter
      if ((e.getCause() instanceof ConcurrentShardReadException)) {
        throw new AggregationJobProcessException(
            INPUT_DATA_READ_FAILED, "Exception while reading reports input data.", e.getCause());
      }
      if ((e.getCause() instanceof DomainReadException)) {
        throw new AggregationJobProcessException(
            INPUT_DATA_READ_FAILED, "Exception while reading domain input data.", e.getCause());
      }

      if (e.getCause() instanceof AccessControlException) {
        throw new AggregationJobProcessException(
            PERMISSION_ERROR, "Exception because of missing permission.", e.getCause());
      }

      throw e;
    }
  }

  private void consumePrivacyBudgetUnits(ImmutableList<PrivacyBudgetUnit> budgetsToConsume, Job job)
      throws AggregationJobProcessException {
    ImmutableList<PrivacyBudgetUnit> missingPrivacyBudgetUnits = ImmutableList.of();

    try {
      // Only send request to PBS if there are units to consume budget for, the list of units
      // can be empty if all reports failed decryption
      if (!budgetsToConsume.isEmpty()) {
        missingPrivacyBudgetUnits =
            privacyBudgetingServiceBridge.consumePrivacyBudget(
                /* budgetsToConsume= */ budgetsToConsume,
                /* attributionReportTo= */ job.requestInfo()
                    .getJobParameters()
                    .get(JOB_PARAM_ATTRIBUTION_REPORT_TO));
      }
    } catch (PrivacyBudgetingServiceBridgeException e) {
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
      ListenableFuture<ImmutableSet<BigInteger>> outputDomainFuture,
      ListenableFuture<ImmutableMap<BigInteger, AggregatedFact>> aggregationFuture,
      Optional<Double> debugPrivacyEpsilon,
      Boolean debugRun) {
    try {
      ImmutableSet<BigInteger> outputDomain = Futures.getDone(outputDomainFuture);
      ImmutableMap<BigInteger, AggregatedFact> aggregation = Futures.getDone(aggregationFuture);

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
          Maps.difference(aggregation, outputDomainPseudoAggregation);

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

      NoisedAggregatedResultSet.Builder noisedResultSetBuilder =
          NoisedAggregatedResultSet.builder();

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
    } catch (ExecutionException e) {
      throw new DomainProcessException(e);
    }
  }

  private ListenableFuture<ImmutableList<EncryptedReport>> readShardAsync(
      Job ctx, DataLocation shard, long shardIndex) {
    return blockingThreadPool.submit(() -> readShard(ctx, shard, shardIndex));
  }

  private ImmutableList<EncryptedReport> readShard(Job ctx, DataLocation shard, long shardIndex) {
    Stopwatch avroStopwatch =
        stopwatches.createStopwatch(String.format("shard-read-%d", shardIndex));
    avroStopwatch.start();
    try (InputStream shardStream = blobStorageClient.getBlob(shard);
        AvroReportsReader reader = readerFactory.create(shardStream)) {
      ImmutableList<EncryptedReport> shardReports =
          reader.streamRecords().map(encryptedReportConverter).collect(toImmutableList());
      avroStopwatch.stop();
      return shardReports;
    } catch (BlobStorageClientException | IOException | AvroRuntimeException e) {
      throw new ConcurrentShardReadException(e);
    }
  }

  private ListenableFuture<ImmutableList<DecryptionValidationResult>> decryptShardAsync(
      Job ctx, ListenableFuture<ImmutableList<EncryptedReport>> shard, long shardIndex) {
    return Futures.transform(
        shard,
        encryptedShard -> decryptShard(ctx, encryptedShard, shardIndex),
        nonBlockingThreadPool);
  }

  private ImmutableList<DecryptionValidationResult> decryptShard(
      Job ctx, ImmutableList<EncryptedReport> shard, long shardIndex) {
    Stopwatch decryptionStopwatch =
        stopwatches.createStopwatch(String.format("shard-decrypt-%d", shardIndex));
    decryptionStopwatch.start();
    ImmutableList<DecryptionValidationResult> results =
        shard.stream()
            .map(report -> reportDecrypterAndValidator.decryptAndValidate(report, ctx))
            .collect(toImmutableList());
    decryptionStopwatch.stop();
    return results;
  }

  private ListenableFuture<ImmutableList<DecryptionValidationResult>> collectInvalidReportsAsync(
      ListenableFuture<ImmutableList<DecryptionValidationResult>> shard) {
    return Futures.transform(shard, this::collectInvalidReports, nonBlockingThreadPool);
  }

  private ImmutableList<DecryptionValidationResult> collectInvalidReports(
      ImmutableList<DecryptionValidationResult> decryptedShard) {
    return decryptedShard.stream()
        .filter(decryptedReport -> decryptedReport.report().isEmpty())
        .limit(MAX_INVALID_REPORTS_COLLECTED)
        .collect(toImmutableList());
  }

  private ListenableFuture<Void> aggregateShardAsync(
      AggregationEngine aggregationEngine,
      ListenableFuture<ImmutableList<DecryptionValidationResult>> shard,
      long shardIndex) {
    return Futures.transform(
        shard,
        decryptedShard -> aggregateShard(aggregationEngine, decryptedShard, shardIndex),
        nonBlockingThreadPool);
  }

  private Void aggregateShard(
      AggregationEngine aggregationEngine,
      ImmutableList<DecryptionValidationResult> decryptedShard,
      long shardIndex) {
    Stopwatch aggregateStopwatch =
        stopwatches.createStopwatch(String.format("shard-aggregation-%d", shardIndex));
    aggregateStopwatch.start();
    decryptedShard.stream()
        .map(DecryptionValidationResult::report)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(aggregationEngine);
    aggregateStopwatch.stop();
    return null;
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
