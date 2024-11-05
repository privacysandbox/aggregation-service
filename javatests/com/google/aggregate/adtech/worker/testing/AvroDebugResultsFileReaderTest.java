/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroDebugResultFileWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroDebugResultsFileReaderTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private LocalAvroDebugResultFileWriter localAvroDebugResultFileWriter;

  // Under test.
  @Inject AvroDebugResultsFileReader avroDebugResultsFileReader;

  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<AggregatedFact> results;

  @Before
  public void setUp() {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("debug_results.avro");

    results =
        ImmutableList.of(
            AggregatedFact.create(
                BigInteger.valueOf(123),
                55L,
                5L,
                List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN)),
            AggregatedFact.create(
                BigInteger.valueOf(456), 25L, 2L, List.of(DebugBucketAnnotation.IN_REPORTS)));
  }

  @Test
  public void testWriteAndReadDebugResults() throws Exception {
    localAvroDebugResultFileWriter.writeLocalFile(results.stream(), avroFile);

    ImmutableList<AggregatedFact> writtenResults =
        avroDebugResultsFileReader.readAvroResultsFile(avroFile);
    assertThat(writtenResults).containsExactlyElementsIn(results);
  }

  public static final class TestEnv extends AbstractModule {}
}
