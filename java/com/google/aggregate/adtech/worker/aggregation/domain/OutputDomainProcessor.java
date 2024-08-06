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
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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
  private final StopwatchRegistry stopwatches;
  private final Boolean domainOptional;
  private final Boolean enableThresholding;

  OutputDomainProcessor(
      ListeningExecutorService blockingThreadPool,
      ListeningExecutorService nonBlockingThreadPool,
      BlobStorageClient blobStorageClient,
      StopwatchRegistry stopwatches,
      Boolean domainOptional,
      Boolean enableThresholding) {
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.blobStorageClient = blobStorageClient;
    this.stopwatches = stopwatches;
    this.domainOptional = domainOptional;
    this.enableThresholding = enableThresholding;
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
   * Conflate aggregated facts with the output domain and noise results using RxJava streaming API.
   * When domainOptional is set, keys only in the aggregatable reports are also included but
   * thresholded using the noised metric. When debugRun is set, domain only, aggregatable report
   * only, and overlapping keys are annotated and set as NoisedDebugResults.
   *
   * @return NoisedAggregatedResultSet containing the combined and noised Aggregatable reports and
   *     output domain buckets.
   */
  public NoisedAggregatedResultSet adjustAggregationWithDomainAndNoiseStreaming(
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
      return noisedResultSetBuilder.build();
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

    return noisedResultSetBuilder.build();
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
