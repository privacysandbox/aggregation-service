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

package com.google.aggregate.adtech.worker.writer.avro;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.testing.AvroReportsFileReader;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalAvroResultFileWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject LocalAvroResultFileWriter localAvroResultFileWriter;

  @Inject AvroResultsFileReader avroResultsFileReader;
  @Inject AvroReportsFileReader avroReportsFileReader;

  private FileSystem filesystem;
  private Path avroFile;
  ImmutableList<AggregatedFact> results;
  private ImmutableList<EncryptedReport> reports;
  private final ByteSource encryptedReport1Payload = ByteSource.wrap(new byte[] {0x00, 0x01});
  private final ByteSource encryptedReport2Payload = ByteSource.wrap(new byte[] {0x01, 0x02});
  private final EncryptedReport encryptedReport1 =
      EncryptedReport.builder()
          .setPayload(encryptedReport1Payload)
          .setKeyId("key1")
          .setSharedInfo("foo")
          .build();

  private final EncryptedReport encryptedReport2 =
      EncryptedReport.builder()
          .setPayload(encryptedReport2Payload)
          .setKeyId("key2")
          .setSharedInfo("bar")
          .build();

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("results.avro");

    results =
        ImmutableList.of(
            AggregatedFact.create(BigInteger.valueOf(123), 50L),
            AggregatedFact.create(BigInteger.valueOf(456), 30L),
            AggregatedFact.create(BigInteger.valueOf(789), 40L));
    reports = ImmutableList.of(encryptedReport1, encryptedReport2);
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteFile() throws Exception {
    localAvroResultFileWriter.writeLocalFile(results.stream(), avroFile);

    ImmutableList<AggregatedFact> writtenResults =
        avroResultsFileReader.readAvroResultsFile(avroFile);
    assertThat(writtenResults).containsExactly(results.toArray());
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        avroFile.getFileSystem().getPath("/doesnotexist", avroFile.toString());

    assertThrows(
        FileWriteException.class,
        () -> localAvroResultFileWriter.writeLocalFile(results.stream(), nonExistentDirectory));
  }

  @Test
  public void localReportWrite_succeeds() throws Exception {
    localAvroResultFileWriter.writeLocalReportFile(reports.stream(), avroFile);

    ImmutableList<EncryptedReport> writtenReports =
        avroReportsFileReader.readAvroReportsFile(avroFile);
    assertThat(writtenReports.get(0).sharedInfo()).isEqualTo(encryptedReport1.sharedInfo());
    assertTrue(writtenReports.get(0).payload().contentEquals(encryptedReport1.payload()));
    assertThat(writtenReports.get(0).keyId()).isEqualTo(encryptedReport1.keyId());

    assertThat(writtenReports.get(1).sharedInfo()).isEqualTo(encryptedReport2.sharedInfo());
    assertTrue(writtenReports.get(1).payload().contentEquals(encryptedReport2.payload()));
    assertThat(writtenReports.get(1).keyId()).isEqualTo(encryptedReport2.keyId());
  }

  @Test
  public void localReportWrite_invalidWritePath_fails() throws Exception {
    Path nonExistentDirectory =
        avroFile.getFileSystem().getPath("/doesnotexist", avroFile.toString());

    assertThrows(
        FileWriteException.class,
        () ->
            localAvroResultFileWriter.writeLocalReportFile(reports.stream(), nonExistentDirectory));
  }

  @Test
  public void testFileExtension() {
    assertThat(localAvroResultFileWriter.getFileExtension()).isEqualTo(".avro");
  }

  public static final class TestEnv extends AbstractModule {}
}
