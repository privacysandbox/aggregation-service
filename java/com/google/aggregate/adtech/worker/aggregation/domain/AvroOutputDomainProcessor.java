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

import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.protocol.avro.AvroOutputDomainReader;
import com.google.aggregate.protocol.avro.AvroOutputDomainReaderFactory;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;

/**
 * Reads output domain from an avro file with schema defined in protocol/avro/output_domain.avsc
 *
 * <p>TODO(b/228085828): Add e2e tests with Avro processor after making output domain required.
 */
public final class AvroOutputDomainProcessor extends OutputDomainProcessor {

  private final BlobStorageClient blobStorageClient;
  private final AvroOutputDomainReaderFactory avroReaderFactory;
  private final StopwatchRegistry stopwatches;

  @Inject
  public AvroOutputDomainProcessor(
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      BlobStorageClient blobStorageClient,
      AvroOutputDomainReaderFactory avroReaderFactory,
      StopwatchRegistry stopwatches) {
    super(
        /* blockingThreadPool= */ blockingThreadPool,
        /* nonBlockingThreadPool= */ nonBlockingThreadPool,
        /* blobStorageClient= */ blobStorageClient,
        /* stopwatches= */ stopwatches);
    this.blobStorageClient = blobStorageClient;
    this.avroReaderFactory = avroReaderFactory;
    this.stopwatches = stopwatches;
  }

  @Override
  protected ImmutableList<BigInteger> readShard(DataLocation outputDomainLocation) {
    Stopwatch stopwatch =
        stopwatches.createStopwatch(String.format("domain-shard-read-%s", UUID.randomUUID()));
    stopwatch.start();
    try (InputStream domainStream = blobStorageClient.getBlob(outputDomainLocation)) {
      AvroOutputDomainReader outputDomainReader = avroReaderFactory.create(domainStream);
      ImmutableList<BigInteger> shard =
          outputDomainReader
              .streamRecords()
              .map(AvroOutputDomainRecord::bucket)
              .collect(toImmutableList());
      stopwatch.stop();
      return shard;
    } catch (IOException | BlobStorageClientException | AvroRuntimeException e) {
      stopwatch.stop(); // stop the stopwatch if an exception occurs
      throw new DomainReadException(e);
    }
  }
}
