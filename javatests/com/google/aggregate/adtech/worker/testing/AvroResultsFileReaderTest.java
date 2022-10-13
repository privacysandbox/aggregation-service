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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroResultFileWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroResultsFileReaderTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject LocalAvroResultFileWriter localAvroResultFileWriter;

  // Under test
  @Inject AvroResultsFileReader avroResultsFileReader;

  private FileSystem filesystem;
  private Path avroFile;
  private ImmutableList<AggregatedFact> results;

  @Before
  public void setUp() {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    avroFile = filesystem.getPath("results.avro");

    results =
        ImmutableList.of(
            AggregatedFact.create(BigInteger.valueOf(123), 55L),
            AggregatedFact.create(BigInteger.valueOf(456), 25L));
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteAndRead() throws Exception {
    localAvroResultFileWriter.writeLocalFile(results.stream(), avroFile);

    ImmutableList<AggregatedFact> writtenResults =
        avroResultsFileReader.readAvroResultsFile(avroFile);
    assertThat(writtenResults).containsExactly(results.toArray());
  }

  public static final class TestEnv extends AbstractModule {}
}
