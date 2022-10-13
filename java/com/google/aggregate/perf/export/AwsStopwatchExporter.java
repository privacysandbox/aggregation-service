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

package com.google.aggregate.perf.export;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.StopwatchExporter.StopwatchExportException;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.common.collect.ImmutableList;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stopwatch exporter that exports to a plaintext file in an S3 bucket. */
public final class AwsStopwatchExporter implements StopwatchExporter {

  private final String exportBucketName;
  private final String keyName;
  private final BlobStorageClient blobStorageClient;
  private Path stopwatchFile;

  @Inject
  AwsStopwatchExporter(
      @StopwatchBucketName String exportBucketName,
      @StopwatchKeyName String keyName,
      BlobStorageClient blobStorageClient) {
    this.exportBucketName = exportBucketName;
    this.keyName = keyName;
    this.blobStorageClient = blobStorageClient;
  }

  @Override
  public void export(StopwatchRegistry stopwatches) throws StopwatchExportException {
    // Forms the file lines as just comma separated key/value pairs, key being the stopwatch name,
    // and the value being the recorded millisecond duration.
    ImmutableList<String> fileLines =
        stopwatches.collectStopwatchTimes().entrySet().stream()
            .map(
                stopwatchEntry ->
                    String.format(
                        "%s,%d", stopwatchEntry.getKey(), stopwatchEntry.getValue().toMillis()))
            .collect(toImmutableList());

    try {
      stopwatchFile =
          Files.createTempFile(/* prefix= */ "stopwatches", /* suffix= */ "txt").toAbsolutePath();
      Files.write(stopwatchFile, fileLines);
    } catch (IOException e) {
      throw new StopwatchExportException(e);
    }

    // Upload to S3.
    try {
      DataLocation location =
          DataLocation.ofBlobStoreDataLocation(
              BlobStoreDataLocation.create(exportBucketName, keyName));
      blobStorageClient.putBlob(location, stopwatchFile);
    } catch (BlobStorageClientException e) {
      throw new StopwatchExportException(e);
    }
  }

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface StopwatchBucketName {}

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface StopwatchKeyName {}
}
