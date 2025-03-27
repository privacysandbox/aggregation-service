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

package com.google.aggregate.adtech.worker.testing;

import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionModule;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportsReader;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeReportWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private FakeReportWriter fakeReportWriter;

  @Inject private AvroReportsReaderFactory avroReportsReaderFactory;
  @Inject private DecryptionKeyService fakeDecryptionKeyService;
  @Inject private SharedInfoSerdes sharedInfoSerdes;
  @Inject private PayloadSerdes payloadSerdes;

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();

  private Path reportsDirectory;

  @Before
  public void before() throws Exception {
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");
    Files.createDirectory(reportsDirectory);
  }

  @Test
  public void writeReports() throws Exception {
    Report report = FakeReportGenerator.generateNullReport();
    Path reportPath = reportsDirectory.resolve("reports.avro");

    fakeReportWriter.writeReports(reportPath, ImmutableList.of(report));

    assertThat(readAndDecryptReports(reportPath)).containsExactly(report);
  }

  @Test
  public void writeReports_withMultipleReports() throws Exception {
    Report nullReport = FakeReportGenerator.generateNullReport();
    Report aggregatableReport =
        FakeReportGenerator.generateWithParam(
            /* reportId= */ 12345, SharedInfo.LATEST_VERSION, /* reportingOrigin= */ "foo.com");
    Path reportPath = reportsDirectory.resolve("reports.avro");

    fakeReportWriter.writeReports(reportPath, ImmutableList.of(nullReport, aggregatableReport));

    assertThat(readAndDecryptReports(reportPath)).containsExactly(aggregatableReport, nullReport);
  }

  @Test
  public void writeReports_withNoReports() throws Exception {
    Path reportPath = reportsDirectory.resolve("reports.avro");

    fakeReportWriter.writeReports(reportPath, ImmutableList.of());

    assertThat(readAndDecryptReports(reportPath)).isEmpty();
  }

  @Test
  public void writeReports_invalidLocation() throws Exception {
    assertThrows(
        RuntimeException.class,
        () -> fakeReportWriter.writeReports(reportsDirectory, ImmutableList.of()));
  }

  private ImmutableList<Report> readAndDecryptReports(Path reportPath) throws IOException {
    AvroReportsReader reader = avroReportsReaderFactory.create(Files.newInputStream(reportPath));
    return reader.streamRecords().map(this::decryptReport).collect(ImmutableList.toImmutableList());
  }

  private Report decryptReport(AvroReportRecord avroReportRecord) {
    try {
      HybridDecrypt hybridDecrypt = fakeDecryptionKeyService.getDecrypter(avroReportRecord.keyId());
      SharedInfo sharedInfo = sharedInfoSerdes.convert(avroReportRecord.sharedInfo()).get();
      byte[] decryptedPayload =
          hybridDecrypt.decrypt(
              avroReportRecord.payload().read(),
              (ASSOCIATED_DATA_PREFIX + avroReportRecord.sharedInfo()).getBytes(UTF_8));
      Payload payload = payloadSerdes.convert(ByteSource.wrap(decryptedPayload)).get();
      return Report.builder().setSharedInfo(sharedInfo).setPayload(payload).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
