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

package com.google.aggregate.protocol.avro;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;

import com.google.acai.Acai;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroReportWriterTest {

  private static final String UUID1 = "8dab7951-e459-4a19-bd6c-d81c0a600f86";
  private static final String UUID2 = "e4aab9af-d259-48da-9270-9484c2eed256";
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroReportsReaderFactory readerFactory;
  // Under test (through factory).
  @Inject private AvroReportWriterFactory writerFactory;
  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<MetadataElement> metadata;

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("output.avro");
    Files.createFile(avroFile); // Creates an empty file.

    metadata =
        ImmutableList.of(
            MetadataElement.create(/* key= */ "foo", /* value= */ "bar"),
            MetadataElement.create(/* key= */ "abc", /* value= */ "xyz"));
  }

  @Test
  public void noRecordsToWriteJustMeta() throws Exception {
    writeRecords(ImmutableList.of());

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroReportsReader reader = getReader()) {
      metaFoo = reader.getMeta("foo");
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
    assertThat(metaNonExistent).isEmpty();
    assertThat(records).isEmpty();
  }

  @Test
  public void genericWriteAsList() throws Exception {
    writeRecords(
        ImmutableList.of(
            createAvroReportRecord(UUID1, new byte[] {0x01}, /* sharedInfo= */ ""),
            createAvroReportRecord(UUID2, new byte[] {0x02, 0x03}, /* sharedInfo= */ "")));

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroReportsReader reader = getReader()) {
      metaFoo = reader.getMeta("foo");
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
    assertThat(metaNonExistent).isEmpty();
    assertThat(records).hasSize(2);
  }

  @Test
  public void genericWriteSharedInfo() throws Exception {
    writeRecords(
        ImmutableList.of(
            createAvroReportRecord(UUID1, new byte[] {0x01}, /* sharedInfo= */ "abc")));

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    try (AvroReportsReader reader = getReader()) {
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(records).hasSize(1);
    AvroReportRecord record = records.stream().collect(onlyElement());
    assertThat(record.sharedInfo()).isEqualTo("abc");
  }

  @Test
  public void genericWriteAsStream() throws Exception {
    writeRecordsAsStream(
        ImmutableList.of(
            createAvroReportRecord(UUID1, new byte[] {0x01}, /* sharedInfo= */ ""),
            createAvroReportRecord(UUID2, new byte[] {0x02, 0x03}, /* sharedInfo= */ "")));

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroReportsReader reader = getReader()) {
      metaFoo = reader.getMeta("foo");
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
    assertThat(metaNonExistent).isEmpty();
    assertThat(records).hasSize(2);
  }

  @Test
  public void genericWriteAsSpliterator() throws Exception {
    writeRecordsAsSpliterator(
        ImmutableList.of(
            createAvroReportRecord(UUID1, new byte[] {0x01}, /* sharedInfo= */ ""),
            createAvroReportRecord(UUID2, new byte[] {0x02, 0x03}, /* sharedInfo= */ "")));

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroReportsReader reader = getReader()) {
      metaFoo = reader.getMeta("foo");
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
    assertThat(metaNonExistent).isEmpty();
    assertThat(records).hasSize(2);
  }

  private AvroReportsReader getReader() throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  private void writeRecords(ImmutableList<AvroReportRecord> avroReportRecord) throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroReportWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecords(metadata, avroReportRecord);
    }
  }

  private void writeRecordsAsStream(ImmutableList<AvroReportRecord> avroReportRecord)
      throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroReportWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecordsFromStream(metadata, avroReportRecord.stream());
    }
  }

  private void writeRecordsAsSpliterator(ImmutableList<AvroReportRecord> avroReportRecord)
      throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroReportWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecordsFromSpliterator(
          metadata, avroReportRecord.spliterator(), avroReportRecord.size());
    }
  }

  private AvroReportRecord createAvroReportRecord(String keyId, byte[] bytes, String sharedInfo) {
    return AvroReportRecord.create(ByteSource.wrap(bytes), keyId, sharedInfo);
  }

  private static final class TestEnv extends AbstractModule {}
}
