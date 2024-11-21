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

package com.google.aggregate.adtech.worker.aggregation.domain;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.serdes.AvroDebugResultsSerdes;
import com.google.aggregate.adtech.worker.model.serdes.AvroResultsSerdes;
import com.google.aggregate.adtech.worker.util.OutputShardFileHelper;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.model.AggregatedResults;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import com.google.aggregate.privacy.noise.model.SummaryReportAvro;
import com.google.aggregate.privacy.noise.model.SummaryReportAvroSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads the output domain and conflate output domain with Aggregatable reports. */
public abstract class OutputDomainProcessor {

  private static final Logger logger = LoggerFactory.getLogger(OutputDomainProcessor.class);

  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();
  private static final int NUM_READ_THREADS = NUM_CPUS;
  private static final int NUM_PROCESS_THREADS = NUM_CPUS;
  private final int MAX_DOMAIN_READ_BUFFER_SIZE = 10000;
  private final int MAX_DOMAIN_PROCESS_BUFFER_SIZE =
      (MAX_DOMAIN_READ_BUFFER_SIZE * NUM_READ_THREADS) / NUM_PROCESS_THREADS;

  private final ListeningExecutorService blockingThreadPool; // for blocking I/O operations
  private final ListeningExecutorService nonBlockingThreadPool; // for other processing operations
  private final BlobStorageClient blobStorageClient;
  private final Boolean domainOptional;
  private final Boolean enableThresholding;
  private final AvroResultsSerdes resultsSerdes;
  private final AvroDebugResultsSerdes debugResultsSerdes;

  OutputDomainProcessor(
      ListeningExecutorService blockingThreadPool,
      ListeningExecutorService nonBlockingThreadPool,
      BlobStorageClient blobStorageClient,
      AvroResultsSerdes resultsSerdes,
      AvroDebugResultsSerdes debugResultsSerdes,
      Boolean domainOptional,
      Boolean enableThresholding) {
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.blobStorageClient = blobStorageClient;
    this.domainOptional = domainOptional;
    this.enableThresholding = enableThresholding;
    this.resultsSerdes = resultsSerdes;
    this.debugResultsSerdes = debugResultsSerdes;
  }

  /**
   * Read all shards at {@link DataLocation} on the cloud storage provider.
   *
   * @return ImmutableList<DataLocation> containing the location of the shards.
   * @throws DomainReadException (unchecked) if there is an error listing the shards.
   */
  public ImmutableList<DataLocation> listShards(DataLocation outputDomainLocation) {
    try {
      ImmutableList<String> shardBlobs = blobStorageClient.listBlobs(outputDomainLocation);

      logger.info("Output domain shards detected by blob storage client: " + shardBlobs);

      BlobStoreDataLocation blobsPrefixLocation = outputDomainLocation.blobStoreDataLocation();

      ImmutableList<DataLocation> shards =
          shardBlobs.stream()
              .map(shard -> BlobStoreDataLocation.create(blobsPrefixLocation.bucket(), shard))
              .map(DataLocation::ofBlobStoreDataLocation)
              .collect(toImmutableList());

      logger.info("Output domain shards to be used: " + shards);

      return shards;
    } catch (BlobStorageClientException e) {
      throw new DomainReadException(e);
    }
  }

  /**
   * Process output domains using RxJava streaming API to read the domains, conflate with report
   * facts, noise, and buffer for summary report. When domainOptional is set, aggregatable
   * report-only facts are also included but thresholded using the noised metric. When debugRun is
   * set, domain only, aggregatable report only, and overlapping facts are annotated and set as
   * NoisedDebugResults. Since output domain keys are processed and added to summary reports
   * separately from report-only keys, enabling debug run or domain optional results in separate
   * files.
   *
   * @return NoisedAggregatedResultSet containing the combined and noised Aggregatable reports and
   *     output domain buckets.
   */
  public AggregatedResults adjustAggregationWithDomainAndNoiseStreaming(
      AggregationEngine aggregationEngine,
      Optional<DataLocation> domainLocation,
      ImmutableList<DataLocation> domainShards,
      NoisedAggregationRunner noisedAggregationRunner,
      Optional<Double> debugPrivacyEpsilon,
      Boolean debugRun)
      throws DomainReadException {
    Set<BigInteger> domainKeySet = Sets.newConcurrentHashSet();
    AtomicLong outputDomainTotalCount = new AtomicLong(0);
    AtomicInteger shardCounter = new AtomicInteger(0);
    Supplier<NoiseApplier> requestScopedNoiseApplier =
        noisedAggregationRunner.getRequestScopedNoiseApplier(debugPrivacyEpsilon);

    // SynchronizedList is thread-safe. Only using bulk addAll function in threads that process
    // buffered facts for each summary report.
    List<SummaryReportAvro> summaryReportAvros = Collections.synchronizedList(new ArrayList<>());
    List<SummaryReportAvro> debugSummaryReportAvros =
        Collections.synchronizedList(new ArrayList<>());

    Flowable.fromStream(domainShards.stream())
        .flatMap(
            dataLocation ->
                processOutputDomainShard(
                    dataLocation,
                    aggregationEngine,
                    noisedAggregationRunner,
                    requestScopedNoiseApplier,
                    domainKeySet,
                    debugRun),
            /* delayErrors= */ false,
            NUM_READ_THREADS,
            MAX_DOMAIN_READ_BUFFER_SIZE)
        .buffer(OutputShardFileHelper.getMaxRecordsPerShard())
        .doOnNext(domains -> outputDomainTotalCount.addAndGet(domains.size()))
        .flatMap(
            summaryFacts ->
                processDomainSummaryFacts(
                    ImmutableList.copyOf(summaryFacts),
                    shardCounter.addAndGet(1),
                    debugRun,
                    summaryReportAvros,
                    debugSummaryReportAvros),
            NUM_PROCESS_THREADS)
        .blockingSubscribe();

    if (domainLocation.isPresent() && outputDomainTotalCount.get() < 1) {
      throw new DomainReadException(
          new IllegalArgumentException(
              String.format(
                  "No output domain provided in the location: %s. Please refer to the API"
                      + " documentation for output domain parameters at"
                      + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md",
                  domainLocation)));
    }

    if (debugRun || domainOptional) {
      Flowable.fromIterable(aggregationEngine.getEntries())
          .map(
              reportOnlyEntry -> {
                BigInteger reportOnlyKey = reportOnlyEntry.getKey();
                AggregatedFact reportOnlyFact =
                    AggregatedFact.create(reportOnlyKey, reportOnlyEntry.getValue().longValue());
                noisedAggregationRunner.noiseSingleFact(reportOnlyFact, requestScopedNoiseApplier);

                if (debugRun) {
                  reportOnlyFact.setDebugAnnotations(List.of(DebugBucketAnnotation.IN_REPORTS));
                }
                return reportOnlyFact;
              })
          .buffer(OutputShardFileHelper.getMaxRecordsPerShard())
          .flatMap(
              summaryFacts ->
                  processReportOnlyFacts(
                      ImmutableList.copyOf(summaryFacts),
                      shardCounter.addAndGet(1),
                      debugRun,
                      debugPrivacyEpsilon,
                      noisedAggregationRunner,
                      summaryReportAvros,
                      debugSummaryReportAvros))
          .blockingSubscribe();
    }

    SummaryReportAvroSet summaryReportAvroSet =
        SummaryReportAvroSet.create(
            ImmutableList.copyOf(summaryReportAvros),
            debugRun
                ? Optional.of(ImmutableList.copyOf(debugSummaryReportAvros))
                : Optional.empty());

    return AggregatedResults.create(summaryReportAvroSet);
  }

  private Flowable<Object> processReportOnlyFacts(
      ImmutableList<AggregatedFact> summaryFacts,
      Integer shardId,
      boolean debugRun,
      Optional<Double> debugPrivacyEpsilon,
      NoisedAggregationRunner noisedAggregationRunner,
      List<SummaryReportAvro> summaryReportAvros,
      List<SummaryReportAvro> debugSummaryReportAvros) {
    if (domainOptional) {
      ImmutableList<AggregatedFact> thresholdedFacts =
          enableThresholding
              ? noisedAggregationRunner.thresholdAggregatedFacts(summaryFacts, debugPrivacyEpsilon)
              : summaryFacts;

      byte[] avroBytes = resultsSerdes.convert(thresholdedFacts);
      summaryReportAvros.add(SummaryReportAvro.create(shardId, avroBytes));
    }

    if (debugRun) {
      byte[] debugAvroBytes = debugResultsSerdes.convert(summaryFacts);
      debugSummaryReportAvros.add(SummaryReportAvro.create(shardId, debugAvroBytes));
    }

    return Flowable.empty();
  }

  private Flowable<Object> processDomainSummaryFacts(
      ImmutableList<AggregatedFact> summaryFacts,
      int shardId,
      Boolean debugRun,
      List<SummaryReportAvro> summaryReportAvros,
      List<SummaryReportAvro> debugSummaryReportAvros) {

    byte[] avroBytes = resultsSerdes.convert(summaryFacts);
    summaryReportAvros.add(SummaryReportAvro.create(shardId, avroBytes));

    if (debugRun) {
      byte[] debugAvroBytes = debugResultsSerdes.convert(summaryFacts);
      debugSummaryReportAvros.add(SummaryReportAvro.create(shardId, debugAvroBytes));
    }

    return Flowable.empty();
  }

  private Flowable<AggregatedFact> processOutputDomainShard(
      DataLocation dataLocation,
      AggregationEngine aggregationEngine,
      NoisedAggregationRunner noisedAggregationRunner,
      Supplier<NoiseApplier> noiseApplier,
      Set<BigInteger> domainKeySet,
      Boolean debugRun) {
    return readShardData(dataLocation)
        .filter(domainKeySet::add)
        .map(
            domainKey -> {
              AggregatedFact aggregatedFact =
                  AggregatedFact.create(
                      domainKey, aggregationEngine.getAggregatedValueOrDefault(domainKey, 0));
              noisedAggregationRunner.noiseSingleFact(aggregatedFact, noiseApplier);

              if (debugRun) {
                List<DebugBucketAnnotation> debugAnnotations = new ArrayList<>();
                if (aggregationEngine.containsKey(domainKey)) {
                  debugAnnotations.add(DebugBucketAnnotation.IN_REPORTS);
                }
                debugAnnotations.add(DebugBucketAnnotation.IN_DOMAIN);

                aggregatedFact.setDebugAnnotations(debugAnnotations);
              }

              aggregationEngine.remove(domainKey);

              return aggregatedFact;
            })
        .subscribeOn(Schedulers.from(blockingThreadPool));
  }

  /**
   * Conflate aggregated facts with the output domain and noise results using RxJava streaming API.
   * When domainOptional is set, keys only in the aggregatable reports are also included but
   * thresholded using the noised metric. When debugRun is set, domain only, aggregatable report
   * only, and overlapping keys are annotated and set as NoisedDebugResults.
   *
   * @return NoisedAggregatedResultSet containing the combined and noised Aggregatable reports and
   *     output domain buckets.
   */
  public AggregatedResults adjustAggregationWithDomainAndNoise(
      AggregationEngine aggregationEngine,
      Optional<DataLocation> domainLocation,
      ImmutableList<DataLocation> domainShards,
      NoisedAggregationRunner noisedAggregationRunner,
      Optional<Double> debugPrivacyEpsilon,
      Boolean debugRun)
      throws DomainReadException {
    Set<BigInteger> reportsOnlyKeys = Sets.newConcurrentHashSet(aggregationEngine.getKeySet());
    Set<BigInteger> overlappingKeys = Sets.newConcurrentHashSet();

    AtomicLong outputDomainTotalCount = new AtomicLong(0);

    Flowable.fromStream(domainShards.stream())
        .flatMap(
            dataLocation ->
                readShardData(dataLocation).subscribeOn(Schedulers.from(blockingThreadPool)),
            /* delayErrors= */ false,
            NUM_READ_THREADS,
            MAX_DOMAIN_READ_BUFFER_SIZE)
        .buffer(MAX_DOMAIN_PROCESS_BUFFER_SIZE)
        .doOnNext(domains -> outputDomainTotalCount.addAndGet(domains.size()))
        .flatMap(
            domainKeysList ->
                Flowable.just(domainKeysList)
                    .subscribeOn(Schedulers.from(nonBlockingThreadPool))
                    .map(
                        domainKeys -> {
                          domainKeys.forEach(
                              domainKey -> {
                                // keys are separately annotated only for debug run.
                                if (debugRun && reportsOnlyKeys.contains(domainKey)) {
                                  overlappingKeys.add(domainKey);
                                }

                                reportsOnlyKeys.remove(domainKey);
                                aggregationEngine.accept(domainKey);
                              });
                          return Observable.empty();
                        }),
            NUM_PROCESS_THREADS)
        .blockingSubscribe();

    if (domainLocation.isPresent() && outputDomainTotalCount.get() < 1) {
      throw new DomainReadException(
          new IllegalArgumentException(
              String.format(
                  "No output domain provided in the location: %s. Please refer to the API"
                      + " documentation for output domain parameters at"
                      + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md",
                  domainLocation)));
    }

    Map<BigInteger, AggregatedFact> aggregatedResults =
        new HashMap<>(aggregationEngine.makeAggregation());

    List<AggregatedFact> reportOnlyFacts =
        reportsOnlyKeys.stream().map(aggregatedResults::remove).collect(Collectors.toList());

    NoisedAggregationResult noisedOverlappingAndDomainResults =
        noisedAggregationRunner.noise(aggregatedResults.values(), debugPrivacyEpsilon);

    NoisedAggregatedResultSet.Builder noisedResultSetBuilder =
        NoisedAggregatedResultSet.builder().setNoisedResult(noisedOverlappingAndDomainResults);

    if (!(debugRun || domainOptional)) {
      return AggregatedResults.create(noisedResultSetBuilder.build());
    }

    // ReportOnly facts are included only if debug run or domain optional are set.
    NoisedAggregationResult noisedReportOnlyResults =
        noisedAggregationRunner.noise(reportOnlyFacts, debugPrivacyEpsilon);

    if (domainOptional) {
      NoisedAggregationResult noisedReportsDomainOptional =
          enableThresholding
              ? noisedAggregationRunner.threshold(
                  noisedReportOnlyResults.noisedAggregatedFacts(), debugPrivacyEpsilon)
              : noisedReportOnlyResults;
      noisedResultSetBuilder.setNoisedResult(
          NoisedAggregationResult.merge(
              noisedOverlappingAndDomainResults, noisedReportsDomainOptional));
    }

    if (debugRun) {
      List<AggregatedFact> domainOnlyFacts = new ArrayList<>();
      List<AggregatedFact> overlappingFacts = new ArrayList<>();
      noisedOverlappingAndDomainResults
          .noisedAggregatedFacts()
          .forEach(
              (aggregatedFact) -> {
                if (overlappingKeys.contains(aggregatedFact.getBucket())) {
                  overlappingFacts.add(aggregatedFact);
                } else {
                  domainOnlyFacts.add(aggregatedFact);
                }
              });

      NoisedAggregationResult noisedDomainOnlyFacts =
          NoisedAggregationResult.create(
              noisedOverlappingAndDomainResults.privacyParameters(),
              ImmutableList.copyOf(domainOnlyFacts));

      NoisedAggregationResult noisedOverlappingFacts =
          NoisedAggregationResult.create(
              noisedOverlappingAndDomainResults.privacyParameters(),
              ImmutableList.copyOf(overlappingFacts));

      noisedResultSetBuilder.setNoisedDebugResult(
          getAnnotatedDebugResults(
              noisedReportOnlyResults, noisedDomainOnlyFacts, noisedOverlappingFacts));
    }

    return AggregatedResults.create(noisedResultSetBuilder.build());
  }

  private NoisedAggregationResult getAnnotatedDebugResults(
      NoisedAggregationResult noisedReportsOnlyResults,
      NoisedAggregationResult noisedDomainOnlyResults,
      NoisedAggregationResult noisedOverlappingResults) {
    NoisedAggregationResult noisedReportsOnlyWithAnno =
        NoisedAggregationResult.addDebugAnnotations(
            noisedReportsOnlyResults, List.of(DebugBucketAnnotation.IN_REPORTS));

    NoisedAggregationResult noisedDomainOnlyWithAnno =
        NoisedAggregationResult.addDebugAnnotations(
            noisedDomainOnlyResults, List.of(DebugBucketAnnotation.IN_DOMAIN));

    NoisedAggregationResult NoisedOverlappingWithAnno =
        NoisedAggregationResult.addDebugAnnotations(
            noisedOverlappingResults,
            List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN));

    return NoisedAggregationResult.merge(
        NoisedOverlappingWithAnno,
        NoisedAggregationResult.merge(noisedReportsOnlyWithAnno, noisedDomainOnlyWithAnno));
  }

  private Flowable<BigInteger> readShardData(DataLocation shard) {
    return Flowable.using(
        () -> {
          try {
            if (blobStorageClient.getBlobSize(shard) <= 0) {
              return InputStream.nullInputStream();
            }
            return blobStorageClient.getBlob(shard);
          } catch (BlobStorageClientException e) {
            throw new DomainReadException(e);
          }
        },
        inputStream -> Flowable.fromStream(readInputStream(inputStream)),
        InputStream::close);
  }

  public abstract Stream<BigInteger> readInputStream(InputStream shardInputStream);
}
