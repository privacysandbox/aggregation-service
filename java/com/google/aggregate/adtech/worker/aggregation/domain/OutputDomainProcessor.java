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

import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads the output domain */
public abstract class OutputDomainProcessor {

  private static final Logger logger = LoggerFactory.getLogger(OutputDomainProcessor.class);

  private final ListeningExecutorService blockingThreadPool; // for blocking I/O operations
  private final ListeningExecutorService nonBlockingThreadPool; // for other processing operations
  private final BlobStorageClient blobStorageClient;
  private final StopwatchRegistry stopwatches;

  OutputDomainProcessor(
      ListeningExecutorService blockingThreadPool,
      ListeningExecutorService nonBlockingThreadPool,
      BlobStorageClient blobStorageClient,
      StopwatchRegistry stopwatches) {
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.blobStorageClient = blobStorageClient;
    this.stopwatches = stopwatches;
  }

  /**
   * Asynchronously reads output domain from {@link DataLocation} and returns a deduped set of
   * buckets in output domain as {@link BigInteger}. The input data location can contain many
   * shards.
   *
   * <p>Shards are listed synchronously and read asynchronously. If there is an error reading the
   * shards the future will complete with an exception.
   *
   * @throws DomainReadException (unchecked) if there is an error listing the shards or the location
   *     provided has no shards present.
   * @return ListenableFuture containing the output domain buckets in a set
   */
  public ListenableFuture<ImmutableSet<BigInteger>> readAndDedupDomain(
      DataLocation outputDomainLocation) {
    ImmutableList<DataLocation> shards = listShards(outputDomainLocation);

    if (shards.isEmpty()) {
      throw new DomainReadException(
          new IllegalArgumentException(
              "No output domain shards found for location: " + outputDomainLocation));
    }

    ImmutableList<ListenableFuture<ImmutableList<BigInteger>>> futureShardReads =
        shards.stream()
            .map(shard -> blockingThreadPool.submit(() -> readShard(shard)))
            .collect(ImmutableList.toImmutableList());

    ListenableFuture<List<ImmutableList<BigInteger>>> allFutureShards =
        Futures.allAsList(futureShardReads);

    return Futures.transform(
        allFutureShards,
        readShards -> {
          Stopwatch stopwatch =
              stopwatches.createStopwatch(
                  String.format("domain-combine-shards-%s", UUID.randomUUID()));
          stopwatch.start();
          ImmutableSet<BigInteger> domain =
              readShards.stream()
                  .flatMap(Collection::stream)
                  .collect(ImmutableSet.toImmutableSet());
          stopwatch.stop();
          if (domain.isEmpty()) {
            throw new DomainReadException(
                new IllegalArgumentException(
                    String.format(
                        "No output domain provided in the location. : %s. Please refer to the API"
                            + " documentation for output domain parameters at"
                            + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md",
                        outputDomainLocation)));
          }
          return domain;
        },
        nonBlockingThreadPool);
  }

  private ImmutableList<DataLocation> listShards(DataLocation outputDomainLocation) {
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
   * Reads a given shard of the output domain
   *
   * @param shardLocation the location of the file to read
   * @return the contents of the shard as a {@link ImmutableList}
   */
  protected abstract ImmutableList<BigInteger> readShard(DataLocation shardLocation);
}
