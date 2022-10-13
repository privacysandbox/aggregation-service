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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.RecordReader.RecordReadException;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClient;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalNioPathAvroReaderFactoryTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private AvroReportWriterFactory writerFactory;

  // Under test
  @Inject private LocalNioPathAvroReaderFactory avroRecordReaderFactory;

  private FileSystem filesystem;
  private Path avroFile;
  private AvroReportWriter avroReportWriter;

  private static final String KEY_ID = "8dab7951-e459-4a19-bd6c-d81c0a600f86";

  @Before
  public void setUp() throws IOException {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("output.avro");
    Files.createFile(avroFile); // Creates an empty file.

    // write out some bytes for avro to recognize file as avro file.
    avroReportWriter = writerFactory.create(Files.newOutputStream(avroFile, CREATE));
    avroReportWriter.writeRecords(
        /* metadata = */ ImmutableList.of(),
        ImmutableList.of(
            AvroReportRecord.create(
                ByteSource.wrap(new byte[] {0x01}), KEY_ID, /* sharedInfo= */ "")));
  }

  @Test
  public void of_localNioPath() throws RecordReadException {
    DataLocation dataLocation = DataLocation.ofLocalNioPath(avroFile);

    // does not throw exception.
    assertThat(avroRecordReaderFactory.of(dataLocation)).isNotNull();
  }

  @Test
  public void of_throwsExceptionForNonExistentLocalNioFilePath() {
    DataLocation dataLocation = DataLocation.ofLocalNioPath(filesystem.getPath("fakePath"));

    assertThrows(RecordReadException.class, () -> avroRecordReaderFactory.of(dataLocation));
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
