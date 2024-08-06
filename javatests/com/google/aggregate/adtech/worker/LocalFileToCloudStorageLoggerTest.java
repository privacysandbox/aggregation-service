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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.EnableParallelSummaryUpload;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.LocalFileToCloudStorageLogger.ResultWorkingDirectory;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.util.OutputShardFileHelper;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroDebugResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroResultFileWriter;
import com.google.aggregate.protocol.avro.AvroDebugResultsReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.aggregate.protocol.avro.AvroDebugResultsRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClient;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

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
          AggregatedFact.create(BigInteger.valueOf(1123), 50L),
          AggregatedFact.create(BigInteger.valueOf(1456), 30L),
          AggregatedFact.create(BigInteger.valueOf(1789), 40L),
          AggregatedFact.create(BigInteger.valueOf(2123), 60L),
          AggregatedFact.create(BigInteger.valueOf(2456), 20L),
          AggregatedFact.create(BigInteger.valueOf(2789), 10L),
          AggregatedFact.create(BigInteger.valueOf(3123), 80L),
          AggregatedFact.create(BigInteger.valueOf(3456), 70L),
          AggregatedFact.create(BigInteger.valueOf(3789), 90L),
          AggregatedFact.create(BigInteger.valueOf(4123), 100L));

  private final ImmutableList<AggregatedFact> debugResults =
      ImmutableList.of(
          AggregatedFact.create(BigInteger.valueOf(1123), 50L, 20L, annotationReportAndDomain),
          AggregatedFact.create(BigInteger.valueOf(1456), 30L, 10L, annotationDomainOnly),
          AggregatedFact.create(BigInteger.valueOf(1789), 40L, 30L, annotationReportOnly),
          AggregatedFact.create(BigInteger.valueOf(2123), 60L, 60L, annotationReportAndDomain),
          AggregatedFact.create(BigInteger.valueOf(2456), 20L, 40L, annotationDomainOnly),
          AggregatedFact.create(BigInteger.valueOf(2789), 10L, 50L, annotationReportOnly),
          AggregatedFact.create(BigInteger.valueOf(3123), 80L, 100L, annotationReportAndDomain),
          AggregatedFact.create(BigInteger.valueOf(3456), 700L, 90L, annotationDomainOnly),
          AggregatedFact.create(BigInteger.valueOf(3789), 90L, 80L, annotationReportOnly),
          AggregatedFact.create(BigInteger.valueOf(4123), 100L, 70L, annotationReportOnly));

  // Under test
  @Inject private Provider<LocalFileToCloudStorageLogger> localFileToCloudStorageLogger;

  @Inject private FSBlobStorageClient blobStorageClient;
  @Inject private AvroResultsFileReader avroResultsFileReader;
  @Inject private AvroDebugResultsReaderFactory readerFactory;
  @Inject private ParallelUploadFlagHelper uploadFlagHelper;
  @Inject private FileSystem testFS;
  @Inject @ResultWorkingDirectory private Path workingDirectory;

  @Before
  public void beforeEach() throws Exception {
    // Clear the filesystem for the next test.
    if (Files.exists(testFS.getPath("/workdir"))) {
      deleteTestFSDirContents(testFS.getPath("/workdir"), false);
    }
    if (Files.exists(testFS.getPath("/bucket"))) {
      deleteTestFSDirContents(testFS.getPath("/bucket"), true);
    }

    uploadFlagHelper.setEnableParallelSummaryUpload(true);
  }

  @Test
  public void logResultsTest_parallelUpload() throws Exception {
    logResultsTest();
  }

  @Test
  public void logResultsTest_singleThreaded() throws Exception {
    uploadFlagHelper.setEnableParallelSummaryUpload(false);
    logResultsTest();
  }

  private void logResultsTest() throws Exception {
    OutputShardFileHelper.setOutputShardFileSizeBytes(100_000_000L);

    // Write the results
    localFileToCloudStorageLogger.get().logResults(results, ctx, /* isDebugRun= */ false);

    // Check that the results were written correctly
    ImmutableList<AggregatedFact> writtenResults =
        avroResultsFileReader.readAvroResultsFile(blobStorageClient.getLastWrittenFile());
    // Check the output file name
    assertThat(blobStorageClient.getLastWrittenFile().toString())
        .isEqualTo("/bucket/dataHandle-1-of-1");
    assertThat(writtenResults).containsExactly(results.toArray());
    // Check that no local file exists in the working directory
    assertThat(Files.list(workingDirectory).collect(toImmutableList())).isEmpty();
  }

  @Test
  public void logResults_InvalidS3BucketThrowsException() throws Exception {
    OutputShardFileHelper.setOutputShardFileSizeBytes(100_000_000L);

    // Throw an exception from the Future.
    doThrow(new RuntimeException("mock exception"))
        .when(blobStorageClient)
        .putBlob(ArgumentMatchers.any(), ArgumentMatchers.any());

    Assert.assertThrows(
        ResultLogException.class,
        () ->
            localFileToCloudStorageLogger
                .get()
                .logResults(ImmutableList.of(), ctx, /* isDebugRun= */ false));

    // Remove any mock methods from the client.
    reset(blobStorageClient);
  }

  @Test
  public void logDebugResultsTest() throws Exception {
    Stream<AvroDebugResultsRecord> writtenResults;
    // Configure large shard size to get a single shard.
    OutputShardFileHelper.setOutputShardFileSizeBytes(100_000_000L);

    // Takes the aggregation debug results and logs them
    localFileToCloudStorageLogger.get().logResults(debugResults, ctx, /* isDebugRun= */ true);

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
    // Check file name for single shard
    assertThat(blobStorageClient.getLastWrittenFile().toString())
        .isEqualTo("/bucket/debug/dataHandle-1-of-1");
    // Check that no local file exists in the working directory
    assertThat(Files.list(workingDirectory).collect(toImmutableList())).isEmpty();
  }

  // logResults would yield 4 shards for 10 output key-value pairs.
  @Test
  public void logResultsTestWithMultiShards() throws Exception {
    ArrayList<AggregatedFact> resultList = new ArrayList<>();
    // Configure shard values to get multiple shards (3 records per shard)
    OutputShardFileHelper.setOutputShardFileSizeBytes(
        3 * OutputShardFileHelper.getOneRecordFileSizeBytes()
            + OutputShardFileHelper.getAvroMetadataSizeBytes());
    ImmutableList<Path> expectedFiles =
        ImmutableList.of(
            Path.of("/bucket/dataHandle-1-of-4"),
            Path.of("/bucket/dataHandle-2-of-4"),
            Path.of("/bucket/dataHandle-3-of-4"),
            Path.of("/bucket/dataHandle-4-of-4"));
    ArrayList<Integer> recordCountPerShard = new ArrayList<>();

    // Write the results
    localFileToCloudStorageLogger.get().logResults(results, ctx, /* isDebugRun= */ false);
    ImmutableList<Path> resultFiles =
        Files.list(blobStorageClient.getLastWrittenFile().getParent()).collect(toImmutableList());
    for (Path path : resultFiles) {
      ImmutableList<AggregatedFact> results = avroResultsFileReader.readAvroResultsFile(path);
      recordCountPerShard.add(results.size());
      for (AggregatedFact result : results) {
        resultList.add(result);
      }
    }

    // Check that the results were written correctly
    // Jimfs Path list doesn't work with "contains" and "containsExactly".
    // Use toString comparison, instead.
    assertThat(resultFiles.toString()).isEqualTo(expectedFiles.toString());
    // Check that the results were written correctly
    assertThat(resultList).containsExactly(results.toArray());
    // Check the number of shards
    assertThat(recordCountPerShard.size()).isEqualTo(4);
    // Check the number of records on each shard
    assertThat(recordCountPerShard.get(0)).isEqualTo(3);
    assertThat(recordCountPerShard.get(1)).isEqualTo(3);
    assertThat(recordCountPerShard.get(2)).isEqualTo(3);
    assertThat(recordCountPerShard.get(3)).isEqualTo(1);
  }

  // logResults on debug run would yield 4 shards for 10 output key-value pairs.
  @Test
  public void logResultsTestOnDebugRunWithMultiShards() throws Exception {
    // Configure shard values to get multiple shards (3 records per shard)
    OutputShardFileHelper.setOutputShardFileSizeBytes(
        3 * OutputShardFileHelper.getOneRecordFileSizeBytes()
            + OutputShardFileHelper.getAvroMetadataSizeBytes());
    ImmutableList<Path> expectedFiles =
        ImmutableList.of(
            Path.of("/bucket/debug/dataHandle-1-of-4"),
            Path.of("/bucket/debug/dataHandle-2-of-4"),
            Path.of("/bucket/debug/dataHandle-3-of-4"),
            Path.of("/bucket/debug/dataHandle-4-of-4"));
    Stream<AvroDebugResultsRecord> writtenResults;
    ArrayList<AggregatedFact> resultList = new ArrayList<>();
    ArrayList<Integer> recordCountPerShard = new ArrayList<>();

    // Write the results
    localFileToCloudStorageLogger.get().logResults(debugResults, ctx, /* isDebugRun= */ true);
    ImmutableList<Path> resultFiles =
        Files.list(blobStorageClient.getLastWrittenFile().getParent()).collect(toImmutableList());
    for (Path path : resultFiles) {
      try (AvroDebugResultsReader reader = getReader(path)) {
        writtenResults = reader.streamRecords();
      }
      List<AggregatedFact> results =
          writtenResults
              .map(
                  writtenResult ->
                      AggregatedFact.create(
                          writtenResult.bucket(),
                          writtenResult.metric(),
                          writtenResult.unnoisedMetric(),
                          writtenResult.debugAnnotations()))
              .collect(Collectors.toList());
      recordCountPerShard.add(results.size());
      resultList.addAll(results);
    }

    // Check that the results were written correctly
    // JimsfsPath list doesn't work with "contains" and "containsExactly".
    // Use toString comparison, instead.
    assertThat(resultFiles.toString()).isEqualTo(expectedFiles.toString());
    // Check that the results were written correctly
    assertThat(resultList).containsExactly(debugResults.toArray());
    // Check the number of shards
    assertThat(recordCountPerShard.size()).isEqualTo(4);
    // Check the number of records on each shard
    assertThat(recordCountPerShard.get(0)).isEqualTo(3);
    assertThat(recordCountPerShard.get(1)).isEqualTo(3);
    assertThat(recordCountPerShard.get(2)).isEqualTo(3);
    assertThat(recordCountPerShard.get(3)).isEqualTo(1);
  }

  private AvroDebugResultsReader getReader(Path avroFile) throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  private void deleteTestFSDirContents(Path directoryPath, boolean deleteSourceDir)
      throws Exception {
    Files.walk(directoryPath)
        .sorted(Comparator.reverseOrder())
        .forEach(
            f -> {
              try {
                if (!deleteSourceDir && f.equals(directoryPath)) {
                  return;
                }
                testFS.provider().deleteIfExists(f);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static final class ParallelUploadFlagHelper {

    boolean isEnableParallelSummaryUpload = true;

    boolean isEnableParallelSummaryUpload() {
      return isEnableParallelSummaryUpload;
    }

    public void setEnableParallelSummaryUpload(boolean flag) {
      isEnableParallelSummaryUpload = flag;
    }
  }

  private static final class TestEnv extends AbstractModule {

    private final ParallelUploadFlagHelper uploadFlagHelper = new ParallelUploadFlagHelper();

    @Override
    protected void configure() {
      bind(LocalResultFileWriter.class)
          .annotatedWith(ResultWriter.class)
          .to(LocalAvroResultFileWriter.class);

      bind(LocalResultFileWriter.class)
          .annotatedWith(DebugWriter.class)
          .to(LocalAvroDebugResultFileWriter.class);

      FileSystem jimFS =
          Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
      bind(FileSystem.class).toInstance(jimFS);
      bind(Path.class)
          .annotatedWith(ResultWorkingDirectory.class)
          .toInstance(jimFS.getPath("/workdir"));

      // Create a Spy fs client to test exception handling.
      FSBlobStorageClient fsClient = spy(new FSBlobStorageClient(jimFS));

      bind(FSBlobStorageClient.class).toInstance(fsClient);
      bind(BlobStorageClient.class).to(FSBlobStorageClient.class);
      bind(ParallelUploadFlagHelper.class).toInstance(uploadFlagHelper);
    }

    @Provides
    @EnableParallelSummaryUpload
    boolean provideEnableParallelSummaryUpload() {
      return uploadFlagHelper.isEnableParallelSummaryUpload();
    }

    @Provides
    @Singleton
    @BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
    }
  }
}
