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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionModule;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.ConcurrentShardReadException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeReportWriter;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportReaderTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();

  private Path reportsDirectory;

  @Before
  public void before() throws Exception {
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");
    Files.createDirectory(reportsDirectory);
  }

  @Inject private ReportReader reportReader;

  @Inject private FakeReportWriter fakeReportWriter;

  @Test
  public void getInputReportsShards_withNoShards_throwsAggregationJobProcessException()
      throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
            .setInputDataBlobPrefix("")
            .build();

    AggregationJobProcessException exception =
        assertThrows(
            AggregationJobProcessException.class,
            () -> reportReader.getInputReportsShards(requestInfo));
    assertThat(exception.getCode()).isEqualTo(AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED);
  }

  @Test
  public void getInputReportsShards_withSingleShard() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
            .setInputDataBlobPrefix("")
            .build();
    Report nullReport = FakeReportGenerator.generateNullReport();
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(nullReport));

    ImmutableList<DataLocation> shardLocations = reportReader.getInputReportsShards(requestInfo);

    assertThat(shardLocations).hasSize(1);
  }

  @Test
  public void getInputReportsShards_withMultipleShards() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
            .setInputDataBlobPrefix("")
            .build();
    Report nullReport = FakeReportGenerator.generateNullReport();
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_10.avro"), ImmutableList.of(nullReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_11.avro"), ImmutableList.of(nullReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_3.avro"), ImmutableList.of(nullReport));

    ImmutableList<DataLocation> shardLocations = reportReader.getInputReportsShards(requestInfo);

    assertThat(shardLocations).hasSize(3);
  }

  @Test
  public void getInputReportsShards_withPrefixes() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.newBuilder()
            .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
            .addInputDataBlobPrefixes("reports_1")
            .addInputDataBlobPrefixes("reports_5")
            .build();
    Report nullReport = FakeReportGenerator.generateNullReport();
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_10.avro"), ImmutableList.of(nullReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_11.avro"), ImmutableList.of(nullReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_3.avro"), ImmutableList.of(nullReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_56.avro"), ImmutableList.of(nullReport));

    ImmutableList<DataLocation> shardLocations = reportReader.getInputReportsShards(requestInfo);

    assertThat(shardLocations).hasSize(3);
  }

  @Test
  public void getEncryptedReports() throws Exception {
    Report nullReport = FakeReportGenerator.generateNullReport();
    Report aggregatableReport1 =
        FakeReportGenerator.generateWithFactList(ImmutableList.of(), SharedInfo.LATEST_VERSION);
    Report aggregatableReport2 =
        FakeReportGenerator.generateWithFactList(ImmutableList.of(), SharedInfo.LATEST_VERSION);
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports.avro"),
        ImmutableList.of(nullReport, aggregatableReport1, aggregatableReport2));

    List<EncryptedReport> readReports =
        reportReader
            .getEncryptedReports(
                DataLocation.ofBlobStoreDataLocation(
                    DataLocation.BlobStoreDataLocation.create(
                        reportsDirectory.toAbsolutePath().toString(), "reports.avro")))
            .toList()
            .blockingGet();

    assertThat(readReports).hasSize(3);
  }

  @Test
  public void getEncryptedReports_withInvalidShard() {
    // The shard is invalid because the location doesn't exist.
    assertThrows(
        ConcurrentShardReadException.class,
        () ->
            reportReader
                .getEncryptedReports(
                    DataLocation.ofBlobStoreDataLocation(
                        DataLocation.BlobStoreDataLocation.create(
                            reportsDirectory.toAbsolutePath().toString(), "reports.avro")))
                .toList()
                .blockingGet());
  }

  @Test
  public void getEncryptedReports_withEmptyShard() throws Exception {
    fakeReportWriter.writeReports(reportsDirectory.resolve("reports.avro"), ImmutableList.of());

    List<EncryptedReport> readReports =
        reportReader
            .getEncryptedReports(
                DataLocation.ofBlobStoreDataLocation(
                    DataLocation.BlobStoreDataLocation.create(
                        reportsDirectory.toAbsolutePath().toString(), "reports.avro")))
            .toList()
            .blockingGet();

    assertThat(readReports).isEmpty();
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      // Report reading
      install(new FSBlobStorageClientModule());
      bind(FileSystem.class).toInstance(FileSystems.getDefault());

      // decryption
      bind(FakeDecryptionKeyService.class).in(TestScoped.class);
      bind(DecryptionKeyService.class).to(FakeDecryptionKeyService.class);
      install(new HybridDecryptionModule());
      bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class);
      bind(PayloadSerdes.class).to(CborPayloadSerdes.class);
    }
  }
}
