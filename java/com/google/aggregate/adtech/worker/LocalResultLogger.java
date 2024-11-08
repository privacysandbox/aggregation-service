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

import static com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.getDataLocation;

import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.LibraryAnnotations.LocalOutputDirectory;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.aggregate.privacy.noise.model.SummaryReportAvro;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

final class LocalResultLogger implements ResultLogger {

  private final LocalResultFileWriter localResultFileWriter;
  private final LocalResultFileWriter localDebugResultFileWriter;
  private final Path workingDirectory;

  @Inject
  public LocalResultLogger(
      @ResultWriter LocalResultFileWriter localResultFileWriter,
      @DebugWriter LocalResultFileWriter localDebugFileWriter,
      @LocalOutputDirectory Path localOutputDirectory) {
    this.localResultFileWriter = localResultFileWriter;
    this.localDebugResultFileWriter = localDebugFileWriter;
    this.workingDirectory = localOutputDirectory;
  }

  @Override
  public void logResults(ImmutableList<AggregatedFact> results, Job ctx, boolean isDebugRun)
      throws ResultLogException {
    String localFileName = isDebugRun ? getLocalDebugFileName(ctx) : getLocalFileName(ctx);
    Path localResultsFilePath =
        workingDirectory
            .getFileSystem()
            .getPath(Paths.get(workingDirectory.toString(), localFileName).toString());
    writeFile(
        results.stream(),
        ctx,
        localResultsFilePath,
        isDebugRun ? localDebugResultFileWriter : localResultFileWriter);
  }

  @Override
  public void logResultsAvros(
      ImmutableList<SummaryReportAvro> summaryReportAvros, Job ctx, boolean isDebugRun) {

    summaryReportAvros.forEach(
        summaryReportAvro -> {
          String localFileName =
              isDebugRun
                  ? getLocalDebugFilename(ctx, summaryReportAvro.shardId())
                  : getLocalFilename(ctx, summaryReportAvro.shardId());
          Path localResultsFilePath =
              workingDirectory
                  .getFileSystem()
                  .getPath(Paths.get(workingDirectory.toString(), localFileName).toString());

          writeFileBytes(
              summaryReportAvro.reportBytes(),
              ctx,
              localResultsFilePath,
              isDebugRun ? localDebugResultFileWriter : localResultFileWriter);
        });
  }

  private DataLocation writeFileBytes(
      byte[] avroBytes, Job ctx, Path filePath, LocalResultFileWriter writer) {
    try {
      Files.createDirectories(workingDirectory);
      writer.writeLocalFile(avroBytes, filePath);

      return getDataLocation(
          ctx.requestInfo().getOutputDataBucketName(), ctx.requestInfo().getOutputDataBlobPrefix());
    } catch (IOException | FileWriteException e) {
      throw new ResultLogException(e);
    }
  }

  private DataLocation writeFile(
      Stream<AggregatedFact> results, Job ctx, Path filePath, LocalResultFileWriter writer)
      throws ResultLogException {
    try {
      Files.createDirectories(workingDirectory);
      writer.writeLocalFile(results, filePath);

      return getDataLocation(
          ctx.requestInfo().getOutputDataBucketName(), ctx.requestInfo().getOutputDataBlobPrefix());
    } catch (IOException | FileWriteException e) {
      throw new ResultLogException(e);
    }
  }

  private String getLocalFileName(Job ctx) {
    return "output" + localResultFileWriter.getFileExtension();
  }

  private String getLocalFilename(Job ctx, int shardId) {
    return "output_" + shardId + localResultFileWriter.getFileExtension();
  }

  private String getLocalDebugFilename(Job ctx, int shardId) {
    return "debug_output_" + shardId + localDebugResultFileWriter.getFileExtension();
  }

  private String getLocalDebugFileName(Job ctx) {
    return "debug_output" + localDebugResultFileWriter.getFileExtension();
  }
}
