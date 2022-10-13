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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.aggregate.protocol.avro.AvroDebugResultsReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.aggregate.protocol.avro.AvroDebugResultsRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalAvroDebugResultFileWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject LocalAvroDebugResultFileWriter localAvroDebugResultFileWriter;
  @Inject private AvroDebugResultsReaderFactory readerFactory;

  private FileSystem filesystem;
  private Path avroFile;
  private final List<DebugBucketAnnotation> annotationReportAndDomain =
      List.of(DebugBucketAnnotation.IN_DOMAIN, DebugBucketAnnotation.IN_REPORTS);
  private final List<DebugBucketAnnotation> annotationDomainOnly =
      List.of(DebugBucketAnnotation.IN_DOMAIN);
  private final List<DebugBucketAnnotation> annotationReportOnly =
      List.of(DebugBucketAnnotation.IN_REPORTS);
  ImmutableList<AggregatedFact> debugResultsAggregatedFact;

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("debug_results.avro");

    debugResultsAggregatedFact =
        ImmutableList.of(
            AggregatedFact.create(BigInteger.valueOf(123), 50L, 20L, annotationReportAndDomain),
            AggregatedFact.create(BigInteger.valueOf(456), 30L, 10L, annotationDomainOnly),
            AggregatedFact.create(BigInteger.valueOf(789), 40L, 5L, annotationReportOnly));
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteFile() throws Exception {
    localAvroDebugResultFileWriter.writeLocalFile(debugResultsAggregatedFact.stream(), avroFile);
    Stream<AvroDebugResultsRecord> writtenResults;

    try (AvroDebugResultsReader reader = getReader()) {
      writtenResults = reader.streamRecords();
    }
    Stream<AggregatedFact> writtenResultsAggregatedFact =
        writtenResults.map(
            writtenResult ->
                AggregatedFact.create(
                    writtenResult.bucket(),
                    writtenResult.metric(),
                    writtenResult.unnoisedMetric(),
                    writtenResult.debugAnnotations()));
    assertThat(writtenResultsAggregatedFact.collect(toImmutableList()))
        .containsExactly(debugResultsAggregatedFact.toArray());
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        avroFile.getFileSystem().getPath("/doesnotexist", avroFile.toString());

    assertThrows(
        FileWriteException.class,
        () ->
            localAvroDebugResultFileWriter.writeLocalFile(
                debugResultsAggregatedFact.stream(), nonExistentDirectory));
  }

  @Test
  public void testFileExtension() {
    assertThat(localAvroDebugResultFileWriter.getFileExtension()).isEqualTo(".avro");
  }

  private AvroDebugResultsReader getReader() throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  public static final class TestEnv extends AbstractModule {}
}
