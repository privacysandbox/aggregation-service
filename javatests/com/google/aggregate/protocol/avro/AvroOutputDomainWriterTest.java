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
import static com.google.common.truth.Truth8.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;

import com.google.acai.Acai;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
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
public class AvroOutputDomainWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroOutputDomainReaderFactory readerFactory;
  // Under test (through factory).
  @Inject private AvroOutputDomainWriterFactory writerFactory;
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

    ImmutableList<AvroOutputDomainRecord> records;
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroOutputDomainReader reader = getReader()) {
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
  public void genericWriteTwoRecords() throws Exception {
    writeRecords(
        ImmutableList.of(
            createAvroOutputDomainRecord(BigInteger.ONE),
            createAvroOutputDomainRecord(BigInteger.TWO)));

    ImmutableList<AvroOutputDomainRecord> records;
    Optional<String> metaFoo;
    Optional<String> metaAbc;
    Optional<String> metaNonExistent;
    try (AvroOutputDomainReader reader = getReader()) {
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

  private AvroOutputDomainRecord createAvroOutputDomainRecord(BigInteger bucket) {
    return AvroOutputDomainRecord.create(bucket);
  }

  private static final class TestEnv extends AbstractModule {}
}
