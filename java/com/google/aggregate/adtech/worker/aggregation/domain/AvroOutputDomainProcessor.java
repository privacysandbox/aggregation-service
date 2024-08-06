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

import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.protocol.avro.AvroOutputDomainReaderFactory;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;

/** Reads output domain from an avro file with schema defined in protocol/avro/output_domain.avsc */
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
      StopwatchRegistry stopwatches,
      @DomainOptional Boolean domainOptional,
      @EnableThresholding Boolean enableThresholding) {
    super(
        blockingThreadPool,
        nonBlockingThreadPool,
        blobStorageClient,
        stopwatches,
        domainOptional,
        enableThresholding);
    this.blobStorageClient = blobStorageClient;
    this.avroReaderFactory = avroReaderFactory;
    this.stopwatches = stopwatches;
  }

  @Override
  public Stream<BigInteger> readInputStream(InputStream shardInputStream) {
    try {
      return avroReaderFactory
          .create(shardInputStream)
          .streamRecords()
          .map(AvroOutputDomainRecord::bucket);
    } catch (IOException | AvroRuntimeException e) {
      throw new DomainReadException(e);
    }
  }
}
