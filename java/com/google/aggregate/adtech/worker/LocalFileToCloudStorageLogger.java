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

package com.google.aggregate.adtech.worker;

import static com.google.aggregate.adtech.worker.util.DebugSupportHelper.getDebugFilePrefix;
import static com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.getDataLocation;
import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.EnableParallelSummaryUpload;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.OutputShardFileHelper;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@code ResultLogger} that writes a local file then uploads it to cloud storage.
 */
public final class LocalFileToCloudStorageLogger implements ResultLogger {

  private final LocalResultFileWriter localResultFileWriter;
  private final LocalResultFileWriter localDebugResultFileWriter;
  private final BlobStorageClient blobStorageClient;
  private final Path workingDirectory;
  private final ListeningExecutorService blockingThreadPool;

  @Inject
  LocalFileToCloudStorageLogger(
      @ResultWriter LocalResultFileWriter localResultFileWriter,
      @DebugWriter LocalResultFileWriter localDebugResultFileWriter,
      @BlockingThreadPool ListeningExecutorService blockingThreadPool,
      BlobStorageClient blobStorageClient,
      @ResultWorkingDirectory Path workingDirectory,
      @EnableParallelSummaryUpload boolean enableParallelUpload) {
    this.localResultFileWriter = localResultFileWriter;
    this.localDebugResultFileWriter = localDebugResultFileWriter;
    this.blobStorageClient = blobStorageClient;
    this.workingDirectory = workingDirectory;
    if (enableParallelUpload) {
      this.blockingThreadPool = blockingThreadPool;
    } else {
      this.blockingThreadPool =
          MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }
  }

  /**
   * Write the results to a local file then write that local file to cloud storage Local filename
   * format: job-[debug]-[JobKey]-[ShardId]-[UUID].avro Output filename format:
   * [Prefix]-[ShardId]-of-[NumShard][.avro if Prefix contains .avro extension] Note: Prefix is
   * provided by a user through a job parameter.
   */
  @Override
  public void logResults(ImmutableList<AggregatedFact> results, Job ctx, boolean isDebugRun)
      throws ResultLogException {
    int totalRecords = results.size();
    int totalShards = OutputShardFileHelper.getNumShards(totalRecords);
    long recordsPerShard = totalRecords / totalShards;

    // remainingRecordsAtTheEnd is the number of records left over after dividing the number of
    // "recordsCountPerShard" records evenly among the shards. remainingRecordsAtTheEnd will be
    // added to the additional shard.
    long remainingRecordsAtTheEnd = totalRecords % totalShards;
    if (remainingRecordsAtTheEnd > 0) {
      totalShards++;
    }
    String fileExtension =
        isDebugRun
            ? localDebugResultFileWriter.getFileExtension()
            : localResultFileWriter.getFileExtension();

    // Stream shardId to write and upload each shard in parallel.
    // In every shard, ImmutableList.subList provides a view of a sharded portion
    // of the results for each shard ID.
    final int finalTotalShards = totalShards; // For lambda function.

    ListenableFuture<List<Void>> fileLogResults =
        Futures.allAsList(
            IntStream.range(1, totalShards + 1)
                .boxed()
                .map(
                    shardId -> {
                      String localFileName =
                          isDebugRun
                              ? getLocalDebugFileName(ctx, shardId, fileExtension)
                              : getLocalFileName(ctx, shardId, fileExtension);
                      Path localResultsFilePath =
                          workingDirectory
                              .getFileSystem()
                              .getPath(
                                  Paths.get(workingDirectory.toString(), localFileName).toString());

                      return writeFile(
                          results
                              .subList(
                                  Long.valueOf((shardId - 1) * recordsPerShard).intValue(),
                                  OutputShardFileHelper.getEndIndexOfShard(
                                      shardId,
                                      finalTotalShards,
                                      recordsPerShard,
                                      remainingRecordsAtTheEnd))
                              .stream(),
                          ctx,
                          localResultsFilePath,
                          isDebugRun ? localDebugResultFileWriter : localResultFileWriter,
                          isDebugRun,
                          shardId,
                          finalTotalShards);
                    })
                .collect(ImmutableList.toImmutableList()));

    try {
      fileLogResults.get();
    } catch (InterruptedException | CancellationException e) {
      throw new ResultLogException(e);
    } catch (ExecutionException e) {
      throw new ResultLogException(e.getCause());
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private ListenableFuture<Void> writeFile(
      Stream<AggregatedFact> aggregatedFacts,
      Job ctx,
      Path localFilepath,
      LocalResultFileWriter writer,
      Boolean isDebugFile,
      int shardId,
      int numShards) {
    return Futures.submitAsync(
        () -> {
          Files.createDirectories(workingDirectory);
          writer.writeLocalFile(aggregatedFacts, localFilepath);

          String outputDataBlobBucket = ctx.requestInfo().getOutputDataBucketName();
          String outputDataBlobPrefix =
              OutputShardFileHelper.getOutputFileNameWithShardInfo(
                  ctx.requestInfo().getOutputDataBlobPrefix(), shardId, numShards);

          DataLocation resultLocation;
          if (isDebugFile) {
            resultLocation =
                getDataLocation(outputDataBlobBucket, getDebugFilePrefix(outputDataBlobPrefix));
          } else {
            resultLocation = getDataLocation(outputDataBlobBucket, outputDataBlobPrefix);
          }

          blobStorageClient.putBlob(resultLocation, localFilepath);
          Files.deleteIfExists(localFilepath);

          return Futures.immediateVoidFuture();
        },
        blockingThreadPool);
  }

  /**
   * The local file name has a random UUID in it to prevent cases where an item is processed twice
   * by the same worker and clobbers other files being written.
   */
  private static String getLocalFileName(Job ctx, int shardId, String fileExtension) {
    // Example: job-JOBKEY-1-d12oc234-123d-4567-abc2-abcdefgh12i3.avro
    return "job-"
        + toJobKeyString(ctx.jobKey())
        + "-"
        + shardId
        + "-"
        + UUID.randomUUID()
        + fileExtension;
  }

  private static String getLocalDebugFileName(Job ctx, int shardId, String fileExtension) {
    // Example: job-debug-JOBKEY-1-d12oc234-123d-4567-abc2-abcdefgh12i3.avro
    return "job-debug-"
        + toJobKeyString(ctx.jobKey())
        + "-"
        + shardId
        + "-"
        + UUID.randomUUID()
        + fileExtension;
  }

  /**
   * Annotation for the {@link java.nio.file.Path} the worker will use to temporarily store results
   * files before persisting them elsewhere.
   */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface ResultWorkingDirectory {}
}
