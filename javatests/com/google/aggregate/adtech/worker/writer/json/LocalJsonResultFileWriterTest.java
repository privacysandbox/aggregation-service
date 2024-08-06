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

package com.google.aggregate.adtech.worker.writer.json;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
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
public class LocalJsonResultFileWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject LocalJsonResultFileWriter localJsonResultFileWriter;

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
            AggregatedFact.create(new BigInteger("123123123123"), 60L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(123), 50L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(456), 30L),
            AggregatedFact.create(NumericConversions.createBucketFromInt(789), 40L));
  }

  /**
   * Simple test that a results file can be written. The file is read back to check that it contains
   * the right data.
   */
  @Test
  public void testWriteFile() throws Exception {
    localJsonResultFileWriter.writeLocalFile(results.stream(), jsonFile);
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
                      entry.get("metric").asLong()));
            });
    assertThat(writtenResults).containsExactly(results.toArray());
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        jsonFile.getFileSystem().getPath("/doesnotexist", jsonFile.toString());

    assertThrows(
        FileWriteException.class,
        () -> localJsonResultFileWriter.writeLocalFile(results.stream(), nonExistentDirectory));
  }

  @Test
  public void testFileExtension() {
    assertThat(localJsonResultFileWriter.getFileExtension()).isEqualTo(".json");
  }

  public static final class TestEnv extends AbstractModule {}
}
