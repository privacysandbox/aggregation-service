/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.writer.json;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.serdes.AvroDebugResultsSerdes;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalJsonDebugResultFileWriterTest {

  private static final ImmutableList<DebugBucketAnnotation> DEBUG_ANNOTATIONS =
      ImmutableList.of(DebugBucketAnnotation.IN_DOMAIN, DebugBucketAnnotation.IN_REPORTS);

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject LocalJsonDebugResultFileWriter localJsonDebugResultFileWriter;

  @Inject AvroDebugResultsSerdes avroResultsSerdes;

  ImmutableList<AggregatedFact> results;
  private FileSystem filesystem;
  private Path jsonFile;

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    jsonFile = filesystem.getPath("results.json");

    results =
        ImmutableList.of(
            AggregatedFact.create(new BigInteger("123123123123"), 60L, 55L, DEBUG_ANNOTATIONS),
            AggregatedFact.create(
                NumericConversions.createBucketFromInt(123), 50L, 55L, DEBUG_ANNOTATIONS),
            AggregatedFact.create(
                NumericConversions.createBucketFromInt(456), 30L, 35L, DEBUG_ANNOTATIONS),
            AggregatedFact.create(
                NumericConversions.createBucketFromInt(789), 40L, 35L, DEBUG_ANNOTATIONS));
  }

  @Test
  public void testWriteFileBytes() throws Exception {
    byte[] resultsBytes = avroResultsSerdes.convert(results);
    localJsonDebugResultFileWriter.writeLocalFile(resultsBytes, jsonFile);
    assertJsonResults();
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteFile() throws Exception {
    localJsonDebugResultFileWriter.writeLocalFile(results.stream(), jsonFile);
    assertJsonResults();
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        jsonFile.getFileSystem().getPath("/doesnotexist", jsonFile.toString());

    assertThrows(
        FileWriteException.class,
        () ->
            localJsonDebugResultFileWriter.writeLocalFile(results.stream(), nonExistentDirectory));
  }

  @Test
  public void testFileExtension() {
    assertThat(localJsonDebugResultFileWriter.getFileExtension()).isEqualTo(".json");
  }

  private void assertJsonResults() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(Files.newInputStream(jsonFile));
    List<AggregatedFact> writtenResults = new ArrayList<>();
    jsonNode
        .iterator()
        .forEachRemaining(
            entry -> {
              writtenResults.add(
                  AggregatedFact.create(
                      NumericConversions.createBucketFromString(entry.get("bucket").asText()),
                      /* metric= */ entry.get("unnoised_metric").asLong()
                          + entry.get("noise").asLong(),
                      /* unnoisedMetric= */ entry.get("unnoised_metric").asLong(),
                      DEBUG_ANNOTATIONS));
            });
    assertThat(writtenResults).containsExactlyElementsIn(results);
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    public void configure() {
      bind(AvroResultsSchemaSupplier.class).toInstance(new AvroResultsSchemaSupplier());
    }
  }
}
