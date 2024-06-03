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

package com.google.aggregate.adtech.worker.writer.json;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalJsonResultFileWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject LocalJsonResultFileWriter localJsonResultFileWriter;

  ImmutableList<AggregatedFact> results;
  private FileSystem filesystem;
  private Path jsonFile;

  private ImmutableList<EncryptedReport> reports;

  // Not testing for payload, since encrypted payload in json is not useful.
  private final ByteSource encryptedReportPayload = ByteSource.wrap(new byte[] {0x00, 0x01});
  private final EncryptedReport encryptedReport1 =
      EncryptedReport.builder()
          .setPayload(encryptedReportPayload)
          .setKeyId("key1")
          .setSharedInfo("foo")
          .build();

  private final EncryptedReport encryptedReport2 =
      EncryptedReport.builder()
          .setPayload(encryptedReportPayload)
          .setKeyId("key2")
          .setSharedInfo("bar")
          .build();

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    jsonFile = filesystem.getPath("results.json");

    results =
        ImmutableList.of(
            AggregatedFact.create(new BigInteger("123123123123"), 60L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(123), 50L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(456), 30L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(789), 40L));
    reports = ImmutableList.of(encryptedReport1, encryptedReport2);
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteFile() throws Exception {
    localJsonResultFileWriter.writeLocalFile(results.stream(), jsonFile);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(Files.newInputStream(jsonFile));
    List<AggregatedFact> writtenResults = new ArrayList<>();
    jsonNode
        .iterator()
        .forEachRemaining(
            entry -> {
              writtenResults.add(
                  AggregatedFact.create(
                      NumericConversions.createBucketFromString(entry.get("bucket").asText()),
                      entry.get("metric").asLong()));
            });
    assertThat(writtenResults).containsExactly(results.toArray());
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        jsonFile.getFileSystem().getPath("/doesnotexist", jsonFile.toString());

    assertThrows(
        FileWriteException.class,
        () -> localJsonResultFileWriter.writeLocalFile(results.stream(), nonExistentDirectory));
  }

  @Test
  public void writeLocalJsonReport_succeeds() throws Exception {
    localJsonResultFileWriter.writeLocalReportFile(reports.stream(), jsonFile);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(Files.newInputStream(jsonFile));
    List<EncryptedReport> writtenReports = new ArrayList<>();
    jsonNode
        .iterator()
        .forEachRemaining(
            entry -> {
              writtenReports.add(
                  EncryptedReport.builder()
                      .setSharedInfo((entry.get("shared_info").asText()))
                      .setKeyId(entry.get("key_id").asText())
                      .setPayload(encryptedReportPayload)
                      .build());
            });
    assertThat(writtenReports.get(0).sharedInfo()).isEqualTo(encryptedReport1.sharedInfo());
    assertThat(writtenReports.get(0).keyId()).isEqualTo(encryptedReport1.keyId());

    assertThat(writtenReports.get(1).sharedInfo()).isEqualTo(encryptedReport2.sharedInfo());
    assertThat(writtenReports.get(1).keyId()).isEqualTo(encryptedReport2.keyId());
  }

  @Test
  public void writeLocalJsonReport_invalidPath_fails() throws Exception {
    Path nonExistentDirectory =
        jsonFile.getFileSystem().getPath("/doesnotexist", jsonFile.toString());

    assertThrows(
        FileWriteException.class,
        () ->
            localJsonResultFileWriter.writeLocalReportFile(reports.stream(), nonExistentDirectory));
  }

  @Test
  public void testFileExtension() {
    assertThat(localJsonResultFileWriter.getFileExtension()).isEqualTo(".json");
  }

  public static final class TestEnv extends AbstractModule {}
}
