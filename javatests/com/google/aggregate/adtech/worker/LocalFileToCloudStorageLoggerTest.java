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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.LocalFileToCloudStorageLogger.ResultWorkingDirectory;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroDebugResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroResultFileWriter;
import com.google.aggregate.protocol.avro.AvroDebugResultsReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.aggregate.protocol.avro.AvroDebugResultsRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClient;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalFileToCloudStorageLoggerTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private final Job ctx = FakeJobGenerator.generate("abc123");

  private final ImmutableList<DebugBucketAnnotation> annotationReportAndDomain =
      ImmutableList.of(DebugBucketAnnotation.IN_DOMAIN, DebugBucketAnnotation.IN_REPORTS);
  private final ImmutableList<DebugBucketAnnotation> annotationDomainOnly =
      ImmutableList.of(DebugBucketAnnotation.IN_DOMAIN);
  private final ImmutableList<DebugBucketAnnotation> annotationReportOnly =
      ImmutableList.of(DebugBucketAnnotation.IN_REPORTS);

  private final ImmutableList<AggregatedFact> results =
      ImmutableList.of(
          AggregatedFact.create(BigInteger.valueOf(123), 50L),
          AggregatedFact.create(BigInteger.valueOf(456), 30L),
          AggregatedFact.create(BigInteger.valueOf(789), 40L));

  private final ImmutableList<AggregatedFact> debugResults =
      ImmutableList.of(
          AggregatedFact.create(BigInteger.valueOf(123), 50L, 20L, annotationReportAndDomain),
          AggregatedFact.create(BigInteger.valueOf(456), 30L, 10L, annotationDomainOnly),
          AggregatedFact.create(BigInteger.valueOf(789), 40L, 30L, annotationReportOnly));

  // Under test
  @Inject LocalFileToCloudStorageLogger localFileToCloudStorageLogger;

  @Inject FSBlobStorageClient blobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;
  @Inject private AvroDebugResultsReaderFactory readerFactory;

  @Inject @ResultWorkingDirectory Path workingDirectory;

  @Test
  public void logResultsTest() throws Exception {
    // No additional set up

    // Write the results
    DataLocation dataLocation = localFileToCloudStorageLogger.logResults(results.stream(), ctx);

    // Check that the result folder is written with the right name
    assertThat(dataLocation.blobStoreDataLocation().bucket()).isEqualTo("bucket");
    // Check that the results file is written with the right name
    assertThat(dataLocation.blobStoreDataLocation().key()).isEqualTo("dataHandle");
    // Check that the results were written correctly
    ImmutableList<AggregatedFact> writtenResults =
        avroResultsFileReader.readAvroResultsFile(blobStorageClient.getLastWrittenFile());
    assertThat(writtenResults).containsExactly(results.toArray());
    // Check that no local file exists in the working directory
    assertThat(Files.list(workingDirectory).collect(toImmutableList())).isEmpty();
  }

  @Test
  public void logDebugResultsTest() throws Exception {

    Stream<AvroDebugResultsRecord> writtenResults;

    // Takes the aggregation debug results and logs them
    DataLocation dataLocation =
        localFileToCloudStorageLogger.logDebugResults(debugResults.stream(), ctx);

    // Check that the result folder is written with the right name
    assertThat(dataLocation.blobStoreDataLocation().bucket()).isEqualTo("bucket");
    // Check that the result file is written with the right name
    assertThat(dataLocation.blobStoreDataLocation().key()).isEqualTo("debug/dataHandle");

    try (AvroDebugResultsReader reader = getReader(blobStorageClient.getLastWrittenFile())) {
      writtenResults = reader.streamRecords();
    }
    Stream<AggregatedFact> writtenResultsAggregatedFact =
        writtenResults.map(
            writtenResult ->
                AggregatedFact.create(
                    writtenResult.bucket(),
                    writtenResult.metric(),
                    writtenResult.unnoisedMetric(),
                    writtenResult.debugAnnotations()));
    // Check that the debug results were written correctly
    assertThat(writtenResultsAggregatedFact.collect(toImmutableList()))
        .containsExactly(debugResults.toArray());
    // Check that no local file exists in the working directory
    assertThat(Files.list(workingDirectory).collect(toImmutableList())).isEmpty();
  }

  private AvroDebugResultsReader getReader(Path avroFile) throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(LocalResultFileWriter.class)
          .annotatedWith(ResultWriter.class)
          .to(LocalAvroResultFileWriter.class);

      bind(LocalResultFileWriter.class)
          .annotatedWith(DebugWriter.class)
          .to(LocalAvroDebugResultFileWriter.class);
      bind(FSBlobStorageClient.class).in(TestScoped.class);
      bind(BlobStorageClient.class).to(FSBlobStorageClient.class);
    }

    @Provides
    FileSystem provideFilesystem() {
      return Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    }

    @Provides
    @Singleton
    @ResultWorkingDirectory
    Path provideWorkingDirectory() throws Exception {
      FileSystem fileSystem =
          Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
      Path workdir = fileSystem.getPath("/workdir");
      Files.createDirectory(workdir);
      return workdir;
    }
  }
}
