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
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
public class AvroOutputDomainReaderTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroOutputDomainWriterFactory writerFactory;
  // Under test (through factory).
  @Inject private AvroOutputDomainReaderFactory readerFactory;
  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<MetadataElement> metadata;

  private static byte[] readBigInteger(BigInteger bucket) {
    return NumericConversions.toUnsignedByteArray(bucket);
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

    ImmutableList<AvroOutputDomainRecord> records;
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    try (AvroOutputDomainReader reader = getReader()) {
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
            createAvroOutputDomainRecord(new byte[] {0x01}),
            createAvroOutputDomainRecord(new byte[] {0x02, 0x03})));

    ImmutableList<AvroOutputDomainRecord> records = ImmutableList.of();
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroOutputDomainReader reader = getReader()) {
      // This stream can only be consumed once. Materializing before reader closes.
      // Reading meta in random order to see if streaming logic deals with it.
      metaFoo = reader.getMeta("foo");
      records = reader.streamRecords().collect(toImmutableList());
      metaAbc = reader.getMeta("abc");
      metaNonExistent = reader.getMeta("random");
    }

    assertThat(records).hasSize(2);
    assertThat(readBigInteger(records.get(0).bucket()))
        .asList()
        .containsExactly((byte) 0x01)
        .inOrder();
    assertThat(readBigInteger(records.get(1).bucket()))
        .asList()
        .containsExactly((byte) 0x02, (byte) 0x03)
        .inOrder();
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

    IOException readException = assertThrows(IOException.class, this::getReader);

    assertThat(readException).hasMessageThat().startsWith("Not an Avro data file");
  }

  @Test
  public void missingBucket() throws Exception {
    Schema schema = SchemaBuilder.record("AggregationBucket").fields().endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroOutputDomainReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field bucket");
  }

  @Test
  public void extraField() throws Exception {
    Schema schema =
        SchemaBuilder.record("AggregationBucket")
            .fields()
            .requiredBytes("bucket")
            .requiredBytes("extraBytes")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("bucket", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("extraBytes", ByteBuffer.wrap(new byte[] {0x03, 0x04}));
      openAvroWriter.append(record);
    }

    ImmutableList<AvroOutputDomainRecord> records;
    try (AvroOutputDomainReader reader = getReader()) {
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(records).hasSize(1);
    assertThat(readBigInteger(records.get(0).bucket()))
        .asList()
        .containsExactly((byte) 0x01, (byte) 0x02)
        .inOrder();
  }

  private AvroOutputDomainReader getReader() throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  private void writeRecords(ImmutableList<AvroOutputDomainRecord> avroOutputDomainRecord)
      throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroOutputDomainWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecords(metadata, avroOutputDomainRecord);
    }
  }

  private AvroOutputDomainRecord createAvroOutputDomainRecord(byte[] bytes) {
    return AvroOutputDomainRecord.create(NumericConversions.uInt128FromBytes((bytes)));
  }

  private static final class TestEnv extends AbstractModule {}
}
