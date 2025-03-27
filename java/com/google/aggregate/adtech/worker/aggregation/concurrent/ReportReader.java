/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.aggregation.concurrent;

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.ConcurrentShardReadException;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import io.reactivex.rxjava3.core.Flowable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for reading input reports. */
final class ReportReader {

  private static final Logger logger = LoggerFactory.getLogger(ReportAggregator.class);
  private final AvroReportsReaderFactory readerFactory;
  private final AvroRecordEncryptedReportConverter encryptedReportConverter;
  private final BlobStorageClient blobStorageClient;

  @Inject
  ReportReader(
      AvroReportsReaderFactory readerFactory,
      AvroRecordEncryptedReportConverter encryptedReportConverter,
      BlobStorageClient blobStorageClient) {
    this.readerFactory = readerFactory;
    this.encryptedReportConverter = encryptedReportConverter;
    this.blobStorageClient = blobStorageClient;
  }

  /**
   * Returns a reactive stream of {@code EncryptedReport} from the given shard.
   *
   * <p>This getting and reading the InputStream are combined into a small unit here to efficiently
   * manage resources by closing inputStream immediately after use.
   */
  Flowable<EncryptedReport> getEncryptedReports(DataLocation shard) {
    return Flowable.using(
        () -> getInputStream(shard),
        inputStream -> Flowable.fromStream(readReportsFromInputStream(inputStream)),
        InputStream::close);
  }

  /**
   * Gets the input report shards.
   *
   * @throws ConcurrentShardReadException when there is an error reading the report shards, and
   *     AggregationJobProcessException if the input is empty.
   */
  ImmutableList<DataLocation> getInputReportsShards(RequestInfo requestInfo)
      throws AggregationJobProcessException {
    List<String> inputDataBlobPrefixes;
    if (!requestInfo.getInputDataBlobPrefixesList().isEmpty()) {
      inputDataBlobPrefixes = requestInfo.getInputDataBlobPrefixesList();
    } else {
      inputDataBlobPrefixes = ImmutableList.of(requestInfo.getInputDataBlobPrefix());
    }
    ImmutableList<DataLocation> dataShards =
        findShards(requestInfo.getInputDataBucketName(), inputDataBlobPrefixes);

    if (dataShards.isEmpty()) {
      throw new AggregationJobProcessException(
          INPUT_DATA_READ_FAILED, "No report shards found for location: " + inputDataBlobPrefixes);
    }
    return dataShards;
  }

  /** Gets all the shards from the given bucket and all the prefixes. */
  private ImmutableList<DataLocation> findShards(String bucket, List<String> inputPrefixes) {
    List<String> shardBlobs = Collections.synchronizedList(new ArrayList<>(inputPrefixes.size()));
    inputPrefixes.parallelStream()
        .map(inputPrefix -> BlobStorageClient.getDataLocation(bucket, inputPrefix))
        .map(
            dataLocation -> {
              try {
                return blobStorageClient.listBlobs(dataLocation);
              } catch (BlobStorageClientException e) {
                throw new ConcurrentShardReadException(e);
              }
            })
        .forEach(shardBlobs::addAll);

    logger.info("Reports shards detected by blob storage client: " + shardBlobs);

    ImmutableList<DataLocation> shards =
        shardBlobs.stream()
            .map(shard -> BlobStoreDataLocation.create(bucket, shard))
            .map(DataLocation::ofBlobStoreDataLocation)
            .collect(toImmutableList());

    logger.info("Reports shards to be used: " + shards);

    return shards;
  }

  /**
   * Returns {@code InputStream} of the shard.
   *
   * @throws ConcurrentShardReadException when there is an error reading the shard.
   */
  private InputStream getInputStream(DataLocation shard) {
    try {
      if (blobStorageClient.getBlobSize(shard) <= 0) {
        return InputStream.nullInputStream();
      }
      return blobStorageClient.getBlob(shard);
    } catch (BlobStorageClient.BlobStorageClientException e) {
      throw new ConcurrentShardReadException(e);
    }
  }

  /**
   * Returns EncryptedReport stream from the inputStream.
   *
   * @throws ConcurrentShardReadException when there is an error reading the shard.
   */
  private Stream<EncryptedReport> readReportsFromInputStream(InputStream shardInputStream) {
    try {
      return readerFactory.create(shardInputStream).streamRecords().map(encryptedReportConverter);
    } catch (IOException | AvroRuntimeException e) {
      throw new ConcurrentShardReadException(e);
    }
  }
}
