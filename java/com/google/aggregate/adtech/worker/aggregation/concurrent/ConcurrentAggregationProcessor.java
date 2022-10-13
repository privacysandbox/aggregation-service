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

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PERMISSION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.whenAllSucceed;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.scp.operator.protos.shared.backend.ReturnCodeProto.ReturnCode.INPUT_DATA_READ_FAILED;
import static com.google.scp.operator.protos.shared.backend.ReturnCodeProto.ReturnCode.OUTPUT_DATAWRITE_FAILED;
import static com.google.scp.operator.protos.shared.backend.ReturnCodeProto.ReturnCode.SUCCESS;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.ReportDecrypterAndValidator;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.ResultLogger.ResultLogException;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor.DomainReadException;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge.PrivacyBudgetingServiceBridgeException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.util.DebugSupportHelper;
import com.google.aggregate.perf.StopwatchRegistry;
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
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.time.Clock;
import java.time.Instant;
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

  public static final String RESULT_SUCCESS_MESSAGE = "Aggregation job successfully processed";

  // Key for user provided debug epsilon value in the job params of the job request.
  public static final String JOB_PARAM_DEBUG_PRIVACY_EPSILON = "debug_privacy_epsilon";
  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";
  public static final String JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX = "output_domain_blob_prefix";
  public static final String JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME = "output_domain_bucket_name";
  public static final String JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT = "debug_privacy_budget_limit";
  // Key to indicate whether this is a debug job
  public static final String JOB_PARAM_DEBUG_RUN = "debug_run";

  // Limit on how many invalid reports are collected; if a batch has a lot of invalid records, not
  // all of them will necessarily be captured.
  private static final int MAX_INVALID_REPORTS_COLLECTED = 1000;

  private static final String PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE =
      "Insufficient privacy budget for one or more aggregatable reports. No aggregatable report can"
          + " appear in more than one batch or contribute to more than one summary report.";
  private static final Logger logger =
      LoggerFactory.getLogger(ConcurrentAggregationProcessor.class);

  private final ReportDecrypterAndValidator reportDecrypterAndValidator;
  private final Provider<AggregationEngine> engineProvider;
  private final OutputDomainProcessor outputDomainProcessor;
  private final NoisedAggregationRunner noisedAggregationRunner;
  private final ResultLogger resultLogger;
  private final BlobStorageClient blobStorageClient;
  private final AvroReportsReaderFactory readerFactory;
  private final AvroRecordEncryptedReportConverter encryptedReportConverter;
  private final Clock clock;
  private final StopwatchRegistry stopwatches;
  private final PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge;
  private final ListeningExecutorService blockingThreadPool;
  private final ListeningExecutorService nonBlockingThreadPool;
  private final boolean domainOptional;
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
      Clock clock,
      StopwatchRegistry stopwatches,
      PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge,
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      @DomainOptional Boolean domainOptional) {
    this.reportDecrypterAndValidator = reportDecrypterAndValidator;
    this.engineProvider = engineProvider;
    this.outputDomainProcessor = outputDomainProcessor;
    this.noisedAggregationRunner = noisedAggregationRunner;
    this.resultLogger = resultLogger;
    this.blobStorageClient = blobStorageClient;
    this.readerFactory = readerFactory;
    this.encryptedReportConverter = encryptedReportConverter;
    this.clock = clock;
    this.stopwatches = stopwatches;
    this.privacyBudgetingServiceBridge = privacyBudgetingServiceBridge;
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.domainOptional = domainOptional;
  }

  @Override
  public JobResult process(Job job) throws AggregationJobProcessException {
    Stopwatch processingStopwatch =
        stopwatches.createStopwatch("concurrent-" + toJobKeyString(job.jobKey()));
    processingStopwatch.start();

    JobResult.Builder jobResultBuilder = JobResult.builder().setJobKey(job.jobKey());

    final Boolean debugRun = DebugSupportHelper.isDebugRun(job);
    final Optional<Double> debugPrivacyEpsilon = getPrivacyEpsilonForJob(job);
    try {
      if (debugPrivacyEpsilon.isPresent()) {
        Double privacyEpsilonForJob = debugPrivacyEpsilon.get();
        if (!(privacyEpsilonForJob > 0d && privacyEpsilonForJob <= 64d)) {
          return handleInvalidEpsilon(jobResultBuilder);
        }
      }
    } catch (Exception e) {
      // TODO cleanup exception handling
      logger.error(
          String.format("Failed Parsing Job parameters for %s", JOB_PARAM_DEBUG_PRIVACY_EPSILON),
          e);
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
        throw new ConcurrentShardReadException(
            new IllegalArgumentException(
                "No report shards found for location: " + reportsLocation));
      }

      outputDomain =
          outputDomainLocation
              .map(outputDomainProcessor::readAndDedupDomain)
              .orElse(immediateFuture(ImmutableSet.of()));
    } catch (ConcurrentShardReadException | DomainReadException e) {
      // Error occurred in data client from the main thread.
      // TODO(b/197999001) report exception in some monitoring counter
      return jobResultForDataReadException(jobResultBuilder, e);
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

      Optional<Integer> debugPrivacyBudgetLimit = getPrivacyBudgetLimitForJob(job);
      ImmutableList<PrivacyBudgetUnit> missingPrivacyBudgetUnits = ImmutableList.of();

      // Do not consume any privacy budget for debug-run jobs.
      if (!debugRun) {
        try {
          // Only send request to PBS if there are units to consume budget for, the list of units
          // can be empty if all reports failed decryption
          ImmutableList<PrivacyBudgetUnit> budgetsToConsume =
              aggregationEngine.getPrivacyBudgetUnits();
          if (!budgetsToConsume.isEmpty()) {
            missingPrivacyBudgetUnits =
                privacyBudgetingServiceBridge.consumePrivacyBudget(
                    /* budgetsToConsume= */ budgetsToConsume,
                    /* attributionReportTo= */ job.requestInfo()
                        .getJobParameters()
                        .get(JOB_PARAM_ATTRIBUTION_REPORT_TO),
                    /* debugPrivacyBudgetLimit= */ debugPrivacyBudgetLimit);
          }
        } catch (PrivacyBudgetingServiceBridgeException e) {
          // Only fail the job if the worker is configured as such. At times during origin trial the
          // job won't fail if there is an error reaching PBS (but still fails if budget is
          // exhausted). This is done to lessen the impact of any instability in the privacy budget
          // service.
          return jobResultBuilder
              .setResultInfo(
                  ResultInfo.newBuilder()
                      .setReturnCode(PRIVACY_BUDGET_ERROR.name())
                      .setReturnMessage(
                          "Exception while consuming privacy budget: " + getStackTraceAsString(e))
                      .setErrorSummary(ErrorSummary.getDefaultInstance())
                      .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                      .build())
              .build();
        }

        if (!missingPrivacyBudgetUnits.isEmpty()) {
          // Truncate the message in order to not overflow the result table.
          return jobResultBuilder
              .setResultInfo(
                  ResultInfo.newBuilder()
                      .setReturnCode(PRIVACY_BUDGET_EXHAUSTED.name())
                      .setReturnMessage(PRIVACY_BUDGET_EXHAUSTED_ERROR_MESSAGE)
                      .setErrorSummary(ErrorSummary.getDefaultInstance())
                      .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                      .build())
              .build();
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

      return jobResultBuilder
          .setResultInfo(
              ResultInfo.newBuilder()
                  .setReturnCode(SUCCESS.name())
                  .setReturnMessage(RESULT_SUCCESS_MESSAGE)
                  .setErrorSummary(errorSummary)
                  .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                  .build())
          .build();
    } catch (ResultLogException e) {
      // Error occurred in data write
      // TODO(b/197999001) report exception in some monitoring counter
      logger.error("Exception occurred during result data write. Reporting processing failure.", e);
      return jobResultBuilder
          .setResultInfo(
              ResultInfo.newBuilder()
                  .setReturnCode(OUTPUT_DATAWRITE_FAILED.name())
                  // TODO see if there's a better error message than the stack trace
                  .setReturnMessage(getStackTraceAsString(e))
                  .setErrorSummary(ErrorSummary.getDefaultInstance())
                  .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                  .build())
          .build();
    } catch (AccessControlException e) {
      return handlePermissionException(e, jobResultBuilder);
    } catch (ExecutionException e) {
      if ((e.getCause() instanceof ConcurrentShardReadException)
          || (e.getCause() instanceof DomainReadException)) {
        // Error occurred in data read
        // TODO(b/197999001) report exception in some monitoring counter
        return jobResultForDataReadException(jobResultBuilder, e.getCause());
      }
      if (e.getCause() instanceof AccessControlException) {
        return handlePermissionException((AccessControlException) e.getCause(), jobResultBuilder);
      }

      throw new AggregationJobProcessException(e);
    } catch (InterruptedException e) {
      throw new AggregationJobProcessException(e);
    }
  }

  private JobResult handleInvalidEpsilon(JobResult.Builder jobResultBuilder) {
    return jobResultBuilder
        .setResultInfo(
            ResultInfo.newBuilder()
                .setReturnCode(INVALID_JOB.name())
                .setReturnMessage(
                    String.format("%s should be > 0 and <= 64", JOB_PARAM_DEBUG_PRIVACY_EPSILON))
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                .build())
        .build();
  }

  private JobResult handlePermissionException(
      AccessControlException e, JobResult.Builder jobResultBuilder) {
    logger.error("Exception occurred due to permission issues: " + e.getMessage());
    return jobResultBuilder
        .setResultInfo(
            ResultInfo.newBuilder()
                .setReturnCode(PERMISSION_ERROR.name())
                // TODO see if there's a better error message than the stack trace
                .setReturnMessage(getStackTraceAsString(e))
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                .build())
        .build();
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
        NoisedAggregationResult noisedReportsOnlyThreshold =
            noisedAggregationRunner.noise(
                pseudoDiff.entriesOnlyOnLeft().values(),
                /* doThreshold= */ true,
                debugPrivacyEpsilon);
        return noisedResultSetBuilder
            .setNoisedResult(
                NoisedAggregationResult.merge(noisedDomainNoThreshold, noisedReportsOnlyThreshold))
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

  private JobResult jobResultForDataReadException(JobResult.Builder jobResultBuilder, Throwable e) {
    logger.error("Exception occurred during input data read. Reporting processing failure.", e);
    return jobResultBuilder
        .setResultInfo(
            ResultInfo.newBuilder()
                .setReturnCode(INPUT_DATA_READ_FAILED.name())
                // TODO see if there's a better error message than the stack trace
                .setReturnMessage(getStackTraceAsString(e))
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                .build())
        .build();
  }

  /** Retrieve limit from nested optional fields */
  private Optional<Integer> getPrivacyBudgetLimitForJob(Job job) {
    if (job.requestInfo().getJobParameters().containsKey(JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT)) {
      return Optional.of(
          Integer.parseInt(
              job.requestInfo().getJobParameters().get(JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT)));
    } else {
      return Optional.empty();
    }
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

  static final class DomainProcessException extends RuntimeException {

    DomainProcessException(Throwable cause) {
      super(cause);
    }
  }

  static final class ConcurrentShardReadException extends RuntimeException {

    ConcurrentShardReadException(Throwable cause) {
      super(cause);
    }
  }
}
