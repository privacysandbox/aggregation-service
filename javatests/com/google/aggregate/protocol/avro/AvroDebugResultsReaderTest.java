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
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
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
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.EnumSymbol;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroDebugResultsReaderTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroDebugResultsWriterFactory writerFactory;
  @Inject private AvroDebugResultsReaderFactory readerFactory;
  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<MetadataElement> metadata;

  private static byte[] readBigInteger(BigInteger bucket) {
    return NumericConversions.toUnsignedByteArray(bucket);
  }

  private List<DebugBucketAnnotation> debugAnnotationList1 =
      List.of(DebugBucketAnnotation.IN_DOMAIN, DebugBucketAnnotation.IN_REPORTS);

  private List<DebugBucketAnnotation> debugAnnotationList2 =
      List.of(DebugBucketAnnotation.IN_DOMAIN);

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

    ImmutableList<AvroDebugResultsRecord> records;
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    try (AvroDebugResultsReader reader = getReader()) {
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
            createAvroDebugResultsRecord(new byte[] {0x01}, 1L, 2L, debugAnnotationList1),
            createAvroDebugResultsRecord(new byte[] {0x02, 0x03}, 3L, 4L, debugAnnotationList2)));

    ImmutableList<AvroDebugResultsRecord> records;
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroDebugResultsReader reader = getReader()) {
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
    assertThat(records.get(0).metric()).isEqualTo(1L);
    assertThat(records.get(1).metric()).isEqualTo(3L);
    assertThat(records.get(0).unnoisedMetric()).isEqualTo(2L);
    assertThat(records.get(1).unnoisedMetric()).isEqualTo(4L);
    assertThat(records.get(0).debugAnnotations()).isEqualTo(debugAnnotationList1);
    assertThat(records.get(1).debugAnnotations()).isEqualTo(debugAnnotationList2);
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
    Schema schema =
        SchemaBuilder.record("DebugAggregatedFact")
            .fields()
            .requiredLong("unnoised_metric")
            .requiredLong("noise")
            .name("annotations")
            .type()
            .array()
            .items()
            .enumeration("bucket_tags")
            .symbols("in_domain", "in_reports")
            .noDefault()
            .endRecord();

    ImmutableList<EnumSymbol> enumList =
        debugAnnotationList1.stream()
            .map(annotation -> new GenericData.EnumSymbol(schema, annotation.toString()))
            .collect(toImmutableList());

    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("unnoised_metric", 1L);
      record.put("noise", 1L);
      record.put("annotations", enumList);
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroDebugResultsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field bucket");
  }

  @Test
  public void missingUnnoisedMetric() throws Exception {
    Schema schema =
        SchemaBuilder.record("AggregationUnnoisedMetric")
            .fields()
            .requiredBytes("bucket")
            .requiredLong("noise")
            .name("annotations")
            .type()
            .array()
            .items()
            .enumeration("bucket_tags")
            .symbols("in_domain", "in_reports")
            .noDefault()
            .endRecord();

    ImmutableList<EnumSymbol> enumList =
        debugAnnotationList1.stream()
            .map(annotation -> new GenericData.EnumSymbol(schema, annotation.toString()))
            .collect(toImmutableList());

    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("bucket", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("noise", 1L);
      record.put("annotations", enumList);
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroDebugResultsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field unnoised_metric");
  }

  @Test
  public void missingNoise() throws Exception {
    Schema schema =
        SchemaBuilder.record("DebugAggregatedFact")
            .fields()
            .requiredBytes("bucket")
            .requiredLong("unnoised_metric")
            .name("annotations")
            .type()
            .array()
            .items()
            .enumeration("bucket_tags")
            .symbols("in_domain", "in_reports")
            .noDefault()
            .endRecord();
    ImmutableList<EnumSymbol> enumList =
        debugAnnotationList1.stream()
            .map(annotation -> new GenericData.EnumSymbol(schema, annotation.toString()))
            .collect(toImmutableList());
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("bucket", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("unnoised_metric", 1L);
      record.put("annotations", enumList);
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroDebugResultsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }

    assertThat(readException).hasMessageThat().contains("missing required field noise");
  }

  @Test
  public void missingAnnotations() throws Exception {
    Schema schema =
        SchemaBuilder.record("DebugAggregatedFact")
            .fields()
            .requiredBytes("bucket")
            .requiredLong("unnoised_metric")
            .requiredLong("noise")
            .endRecord();
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("bucket", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("unnoised_metric", 1L);
      record.put("noise", 1L);
      openAvroWriter.append(record);
    }

    AvroRuntimeException readException;
    try (AvroDebugResultsReader reader = getReader()) {
      readException =
          assertThrows(
              AvroRuntimeException.class, () -> reader.streamRecords().collect(toImmutableList()));
    }
    assertThat(readException).hasMessageThat().contains("missing required field annotations");
  }

  @Test
  public void extraField() throws Exception {
    Schema schema =
        SchemaBuilder.record("DebugAggregatedFact")
            .fields()
            .requiredBytes("bucket")
            .requiredLong("unnoised_metric")
            .requiredLong("noise")
            .requiredBytes("extraBytes")
            .name("annotations")
            .type()
            .array()
            .items()
            .enumeration("bucket_tags")
            .symbols("in_domain", "in_reports")
            .noDefault()
            .endRecord();
    ImmutableList<EnumSymbol> enumList =
        debugAnnotationList1.stream()
            .map(annotation -> new GenericData.EnumSymbol(schema, annotation.toString()))
            .collect(toImmutableList());
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schema));
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        DataFileWriter<GenericRecord> openAvroWriter =
            avroWriter.create(schema, outputAvroStream)) {
      GenericRecord record = new GenericData.Record(schema);
      record.put("bucket", ByteBuffer.wrap(new byte[] {0x01, 0x02}));
      record.put("unnoised_metric", 1L);
      record.put("noise", 2L);
      record.put("annotations", enumList);
      record.put("extraBytes", ByteBuffer.wrap(new byte[] {0x03, 0x04}));
      openAvroWriter.append(record);
    }

    ImmutableList<AvroDebugResultsRecord> records;
    try (AvroDebugResultsReader reader = getReader()) {
      records = reader.streamRecords().collect(toImmutableList());
    }

    assertThat(records).hasSize(1);
    assertThat(readBigInteger(records.get(0).bucket()))
        .asList()
        .containsExactly((byte) 0x01, (byte) 0x02)
        .inOrder();
    assertThat(records.get(0).metric()).isEqualTo(3L);
    assertThat(records.get(0).unnoisedMetric()).isEqualTo(1L);
    assertThat(records.get(0).debugAnnotations()).isEqualTo(debugAnnotationList1);
  }

  private AvroDebugResultsReader getReader() throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  private void writeRecords(ImmutableList<AvroDebugResultsRecord> avroDebugResultsRecord)
      throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(avroFile, CREATE);
        AvroDebugResultsWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecords(metadata, avroDebugResultsRecord);
    }
  }

  private AvroDebugResultsRecord createAvroDebugResultsRecord(
      byte[] bytes,
      long metric,
      long unnoisedMetric,
      List<DebugBucketAnnotation> debugAnnotations) {
    return AvroDebugResultsRecord.create(
        NumericConversions.uInt128FromBytes((bytes)), metric, unnoisedMetric, debugAnnotations);
  }

  private static final class TestEnv extends AbstractModule {}
}
