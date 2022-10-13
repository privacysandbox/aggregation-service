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

import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Implementation of {@code ResultLogger} that writes a local file then uploads it to cloud storage.
 */
public final class LocalFileToCloudStorageLogger implements ResultLogger {

  private final LocalResultFileWriter localResultFileWriter;
  private final LocalResultFileWriter localDebugResultFileWriter;
  private final BlobStorageClient blobStorageClient;
  private final Path workingDirectory;

  @Inject
  LocalFileToCloudStorageLogger(
      @ResultWriter LocalResultFileWriter localResultFileWriter,
      @DebugWriter LocalResultFileWriter localDebugResultFileWriter,
      BlobStorageClient blobStorageClient,
      @ResultWorkingDirectory Path workingDirectory) {
    this.localResultFileWriter = localResultFileWriter;
    this.localDebugResultFileWriter = localDebugResultFileWriter;
    this.blobStorageClient = blobStorageClient;
    this.workingDirectory = workingDirectory;
  }

  /** Write the results to a local file then write that local file to cloud storage */
  @Override
  public DataLocation logResults(Stream<AggregatedFact> results, Job ctx)
      throws ResultLogException {
    String localFileName = getLocalFileName(ctx);
    Path localResultsFilePath =
        workingDirectory
            .getFileSystem()
            .getPath(Paths.get(workingDirectory.toString(), localFileName).toString());

    return writeFile(
        results, ctx, localResultsFilePath, localResultFileWriter, /* isDebugFile = */ false);
  }

  @Override
  public DataLocation logDebugResults(Stream<AggregatedFact> results, Job ctx)
      throws ResultLogException {
    String localDebugFileName = getLocalDebugFileName(ctx);
    Path localDebugResultsFilePath =
        workingDirectory
            .getFileSystem()
            .getPath(Paths.get(workingDirectory.toString(), localDebugFileName).toString());

    return writeFile(
        results,
        ctx,
        localDebugResultsFilePath,
        localDebugResultFileWriter,
        /* isDebugFile = */ true);
  }

  private DataLocation writeFile(
      Stream<AggregatedFact> results,
      Job ctx,
      Path filePath,
      LocalResultFileWriter writer,
      Boolean isDebugFile)
      throws ResultLogException {
    try {
      // Create the working directory if it doesn't exist
      Files.createDirectories(workingDirectory);

      // Write the results to a local file.
      writer.writeLocalFile(results, filePath);

      DataLocation resultLocation;
      String outputDataBlobBucket = ctx.requestInfo().getOutputDataBucketName();
      String outputDataBlobPrefix = ctx.requestInfo().getOutputDataBlobPrefix();

      if (isDebugFile) {
        resultLocation =
            getDataLocation(outputDataBlobBucket, getDebugFilePrefix(outputDataBlobPrefix));
      } else {
        resultLocation = getDataLocation(outputDataBlobBucket, outputDataBlobPrefix);
      }

      // Upload the file to cloud storage
      blobStorageClient.putBlob(resultLocation, filePath);

      return resultLocation;
    } catch (IOException | FileWriteException | BlobStorageClientException e) {
      throw new ResultLogException(e);
    } finally {
      // Delete the local file since it is no longer needed
      try {
        Files.deleteIfExists(filePath);
      } catch (IOException e) {
        throw new ResultLogException(e);
      }
    }
  }

  /**
   * The local file name has a random UUID in it to prevent cases where an item is processed twice
   * by the same worker and clobbers other files being written.
   */
  private String getLocalFileName(Job ctx) {
    return "job-"
        + toJobKeyString(ctx.jobKey())
        + "-"
        + UUID.randomUUID()
        + localResultFileWriter.getFileExtension();
  }

  private String getLocalDebugFileName(Job ctx) {
    return "job-debug-"
        + toJobKeyString(ctx.jobKey())
        + "-"
        + UUID.randomUUID()
        + localDebugResultFileWriter.getFileExtension();
  }

  /**
   * Annotation for the {@link java.nio.file.Path} the worker will use to temporarily store results
   * files before persisting them elsewhere.
   */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface ResultWorkingDirectory {}
}
