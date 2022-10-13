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
import static com.google.common.truth.Truth8.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroReportsReaderTest {

  private static final String UUID1 = "8dab7951-e459-4a19-bd6c-d81c0a600f86";
  private static final String UUID2 = "e4aab9af-d259-48da-9270-9484c2eed256";
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroReportWriterFactory writerFactory;
  // Under test (through factory).
  @Inject private AvroReportsReaderFactory readerFactory;
  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<MetadataElement> metadata;

  private static byte[] readBytes(ByteSource bytes) throws IOException {
    try (InputStream inputBytes = bytes.openStream();
        ByteArrayOutputStream sinkBytes = new ByteArrayOutputStream()) {
      ByteStreams.copy(inputBytes, sinkBytes);
      sinkBytes.flush();
      return sinkBytes.toByteArray();
    }
  }

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
  public void noRecordsToRead() throws Exception {
    // No data written, just random file metadata, zero records otherwise.
    writeRecords(ImmutableList.of());

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    try (AvroReportsReader reader = getReader()) {
      // This stream can only be consumed once. Materializing before reader closes.
      // Reading meta in random order to see if streaming logic deals with it.
      metaAbc = reader.getMeta("abc");
      records = reader.streamRecords().collect(toImmutableList());
      metaFoo = reader.getMeta("foo");
    }

    assertThat(records).isEmpty();
    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
  }

  @Test
  public void readAndExhaust() throws Exception {
    writeRecords(
        ImmutableList.of(
            createAvroReportRecord(UUID1, new byte[] {0x01}, /* sharedInfo= */ "foo"),
            createAvroReportRecord(UUID2, new byte[] {0x02, 0x03}, /* sharedInfo= */ "bar")));

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroReportsReader reader = getReader()) {
      // This stream can only be consumed once. Materializing before reader closes.
      // Reading meta in random order to see if streaming logic deals with it.
      metaFoo = reader.getMeta("foo");
      records = reader.streamRecords().collect(toImmutableList());
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
    }

    assertThat(records).hasSize(2);
    assertThat(readBytes(records.get(0).payload())).asList().containsExactly((byte) 0x01).inOrder();
    assertThat(readBytes(records.get(1).payload()))
        .asList()
        .containsExactly((byte) 0x02, (byte) 0x03)
        .inOrder();
    assertThat(records.get(0).keyId().toString()).isEqualTo(UUID1);
    assertThat(records.get(1).keyId().toString()).isEqualTo(UUID2);
    assertThat(records.get(0).sharedInfo()).isEqualTo("foo");
    assertThat(records.get(1).sharedInfo()).isEqualTo("bar");
    assertThat(metaFoo).hasValue("bar");
    assertThat(metaAbc).hasValue("xyz");
    assertThat(metaNonExistent).isEmpty();
  }

  @Test
  public void malformedAvroFile() throws Exception {
    // Writing garbage bytes to the file.
    try (OutputStream outputStream = Files.newOutputStream(avroFile, CREATE)) {
      outputStream.write(new byte[] {0x01, 0x02, 0x03});
      outputStream.flush();
    }

    IOException readException = assertThrows(IOException.class, () -> getReader());

    assertThat(readException).hasMessageThat().startsWith("Not an Avro data file");
  }

  @Test
  public void missingKeyId() throws Exception {
    Schema schema =
        SchemaBuilder.record("AggregatableReport")
            .fields()
            .requiredBytes("payload")
            .requiredString("shared_info")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("payload", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("shared_info", "foo");
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroReportsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field key_id");
  }

  @Test
  public void missingPayload() throws Exception {
    Schema schema =
        SchemaBuilder.record("BrowserReport")
            .fields()
            .requiredBytes("key_id")
            .requiredString("shared_info")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("key_id", ByteBuffer.wrap(UUID1.getBytes(StandardCharsets.UTF_8)));
      record.put("shared_info", "foo");
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroReportsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field payload");
  }

  @Test
  public void missingSharedInfo() throws Exception {
    Schema schema =
        SchemaBuilder.record("BrowserReport")
            .fields()
            .requiredBytes("payload")
            .requiredBytes("key_id")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("payload", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("key_id", ByteBuffer.wrap(UUID1.getBytes(StandardCharsets.UTF_8)));
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroReportsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field shared_info");
  }

  /** Test that if the file has the wrong name and extra fields then they are just ignored */
  @Test
  public void extraFieldsAndWrongNameAreIgnored() throws Exception {
    Schema schema =
        SchemaBuilder.record("DifferentName")
            .fields()
            .requiredBytes("payload")
            .requiredString("key_id")
            .requiredString("shared_info")
            .requiredBytes("extraBytes")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("payload", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("key_id", UUID1);
      record.put("shared_info", "foo");
      record.put("extraBytes", ByteBuffer.wrap(new byte[] {0x03, 0x04}));
      openAvroWriter.append(record);
    }

    ImmutableList<AvroReportRecord> records = ImmutableList.of();
    try (AvroReportsReader reader = getReader()) {
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(records).hasSize(1);
    assertThat(readBytes(records.get(0).payload()))
        .asList()
        .containsExactly((byte) 0x01, (byte) 0x02)
        .inOrder();
    assertThat(records.get(0).keyId().toString()).isEqualTo(UUID1);
    assertThat(records.get(0).sharedInfo()).isEqualTo("foo");
  }

  @Test
  public void readLargeRecords() throws Exception {
    byte[] record1 = Strings.repeat("foo", 10000).getBytes(StandardCharsets.UTF_8);
    byte[] record2 = Strings.repeat("bar", 500).getBytes(StandardCharsets.UTF_8);
    writeRecords(
        ImmutableList.of(
            createAvroReportRecord(UUID1, record1, /* sharedInfo= */ "fizz"),
            createAvroReportRecord(UUID2, record2, /* sharedInfo= */ "buzz")));

    ImmutableList<AvroReportRecord> records;

    try (AvroReportsReader reader = getReader()) {
      // This stream can only be consumed once. Materializing before reader closes.
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(records).hasSize(2);
    assertThat(readBytes(records.get(0).payload())).isEqualTo(record1);
    assertThat(readBytes(records.get(1).payload())).isEqualTo(record2);
    assertThat(records.get(0).keyId().toString()).isEqualTo(UUID1);
    assertThat(records.get(1).keyId().toString()).isEqualTo(UUID2);
    assertThat(records.get(0).sharedInfo()).isEqualTo("fizz");
    assertThat(records.get(1).sharedInfo()).isEqualTo("buzz");
  }

  @Test
  public void readAfterCloseFails() throws Exception {
    // Setup avro file with records with size greater than the avro internal buffer to test stream
    // is being closed properly.
    byte[] record1 = Strings.repeat("foo", 10000).getBytes(StandardCharsets.UTF_8);
    byte[] record2 = Strings.repeat("bar", 500).getBytes(StandardCharsets.UTF_8);
    writeRecords(
        ImmutableList.of(
            createAvroReportRecord(UUID1, record1, /* sharedInfo= */ "fizz"),
            createAvroReportRecord(UUID2, record2, /* sharedInfo= */ "buzz")));

    Stream<AvroReportRecord> records;
    try (AvroReportsReader reader = getReader()) {
      // This stream can only be consumed once. Materializing before reader closes.
      records = reader.streamRecords();
    }

    AvroRuntimeException exception =
        assertThrows(AvroRuntimeException.class, () -> records.collect(toImmutableList()));
    assertThat(exception).hasMessageThat().isEqualTo("java.io.IOException: stream is closed");
  }

  @Test
  public void genericReadSharedInfo() throws Exception {
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

  private AvroReportsReader getReader() throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  private void writeRecords(ImmutableList<AvroReportRecord> avroReportRecord) throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroReportWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecords(metadata, avroReportRecord);
    }
  }

  private AvroReportRecord createAvroReportRecord(String keyId, byte[] bytes, String sharedInfo) {
    return AvroReportRecord.create(ByteSource.wrap(bytes), keyId, sharedInfo);
  }

  private static final class TestEnv extends AbstractModule {}
}
