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

package com.google.aggregate.adtech.worker.reader.avro;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.RecordReader.RecordReadException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClient;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.adtech.worker.shared.testing.StringToByteSourceConverter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.AvroRuntimeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalNioPathAvroRecordReaderTest {

  private static final StringToByteSourceConverter TO_BYTE_SOURCE_CONVERTER =
      new StringToByteSourceConverter();
  private static final String KEY_ID = "8dab7951-e459-4a19-bd6c-d81c0a600f86";

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private AvroReportWriterFactory writerFactory;
  // Under test
  @Inject private LocalNioPathAvroReaderFactory avroRecordReaderFactory;

  private FileSystem filesystem;
  private Path avroFile;
  private AvroReportWriter avroReportWriter;
  private Job.Builder JobBuilder;

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("output.avro");
    Files.createFile(avroFile); // Creates an empty file.

    JobBuilder = FakeJobGenerator.generate("foo").toBuilder();
  }

  private void setUpAvroFile(ImmutableList<String> inputs) throws IOException {
    ImmutableList<AvroReportRecord> records =
        inputs.stream()
            .map(
                bytes ->
                    AvroReportRecord.create(
                        TO_BYTE_SOURCE_CONVERTER.convert(bytes), KEY_ID, "sharedInfo"))
            .collect(toImmutableList());
    avroReportWriter = writerFactory.create(Files.newOutputStream(avroFile, CREATE));
    avroReportWriter.writeRecords(/* metadata = */ ImmutableList.of(), records);
  }

  @Test
  public void readEncryptedReport() throws IOException, RecordReadException {
    // Setup avro file with records.
    setUpAvroFile(ImmutableList.of("foo", "bar"));
    // Setup Job to read from local file.
    DataLocation dataLocation = DataLocation.ofLocalNioPath(avroFile);
    LocalNioPathAvroRecordReader avroRecordReader = avroRecordReaderFactory.of(dataLocation);

    try (Stream<EncryptedReport> reports = avroRecordReader.readEncryptedReports(dataLocation)) {
      assertThat(
              reports
                  .map(EncryptedReport::payload)
                  .map(bytes -> TO_BYTE_SOURCE_CONVERTER.reverse().convert(bytes)))
          .containsExactly("foo", "bar");
    }
  }

  @Test
  public void readLargeEncryptedReport_localNioPath() throws IOException, RecordReadException {
    // Setup avro file with records with size greater than the avro internal buffer to test stream
    // is being closed properly.
    String record1 = Strings.repeat("foo", 10000);
    String record2 = Strings.repeat("bar", 10000);
    setUpAvroFile(ImmutableList.of(record1, record2));
    // Setup Job to read from local file.
    DataLocation dataLocation = DataLocation.ofLocalNioPath(avroFile);
    LocalNioPathAvroRecordReader avroRecordReader = avroRecordReaderFactory.of(dataLocation);

    try (Stream<EncryptedReport> reports = avroRecordReader.readEncryptedReports(dataLocation)) {
      assertThat(
              reports
                  .map(EncryptedReport::payload)
                  .map(bytes -> TO_BYTE_SOURCE_CONVERTER.reverse().convert(bytes)))
          .containsExactly(record1, record2);
    }
  }

  @Test
  public void readAfterClose_throwsException() throws Exception {
    // Setup avro file with records with size greater than the avro internal buffer to test stream
    // is being closed properly.
    String record1 = Strings.repeat("foo", 10000);
    String record2 = Strings.repeat("bar", 10000);
    setUpAvroFile(ImmutableList.of(record1, record2));
    // Setup Job to read from local file.
    DataLocation dataLocation = DataLocation.ofLocalNioPath(avroFile);
    LocalNioPathAvroRecordReader avroRecordReader = avroRecordReaderFactory.of(dataLocation);

    // read reports and close before materializing.
    Stream<EncryptedReport> reports = avroRecordReader.readEncryptedReports(dataLocation);
    avroRecordReader.close();

    AvroRuntimeException exception =
        assertThrows(AvroRuntimeException.class, () -> reports.collect(toImmutableList()));
    assertThat(exception).hasCauseThat().isInstanceOf(IOException.class);
    assertThat(exception).hasMessageThat().isEqualTo("java.io.IOException: stream is closed");
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FSBlobStorageClient.class).in(TestScoped.class);
      bind(BlobStorageClient.class).to(FSBlobStorageClient.class);
    }

    @Provides
    FileSystem provideFilesystem() {
      return Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    }
  }
}
