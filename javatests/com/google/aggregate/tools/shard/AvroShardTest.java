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

package com.google.aggregate.tools.shard;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.aggregate.protocol.avro.AvroOutputDomainReader;
import com.google.aggregate.protocol.avro.AvroOutputDomainReaderFactory;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportsReader;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroShardTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  private Path outPutDirectory;
  private Path baseDirectory;
  static Injector injector = Guice.createInjector(new TestEnv());

  @Inject AvroShard avroShard;

  @Before
  public void setUp() throws IOException {
    outPutDirectory = testWorkingDir.getRoot().toPath();
    baseDirectory = Path.of("worker/testing/data/library/");
  }

  @Test
  public void testReportShard() throws IOException {
    int numShard = 2;
    Path outputReportShardsDir = outPutDirectory.resolve("reportShards");
    Path reportPath = baseDirectory.resolve("input_set_attribution_1/batch.avro");
    List<AvroReportRecord> reportShards = new ArrayList<>();
    String[] cli =
        new String[] {
          "--input", reportPath.toString(),
          "--output_dir", outputReportShardsDir.toString(),
          "--num_shards", String.valueOf(numShard)
        };

    avroShard.main(cli);

    assertTrue(Files.exists(outputReportShardsDir));

    List<String> reportShardPaths = Arrays.asList(outputReportShardsDir.toFile().list());

    assertEquals(reportShardPaths.size(), numShard);

    for (String reportShardPath : reportShardPaths) {
      reportShards.addAll(readReport(outputReportShardsDir.resolve(reportShardPath)));
    }

    assertEquals(reportShards.size(), readReport(reportPath).size());
  }

  @Test
  public void testDomainShard() throws IOException {
    int numShard = 2;
    Path outputDomainShardsDir = outPutDirectory.resolve("domainShards");
    Path domainPath = baseDirectory.resolve("input_set_attribution_1/domain.avro");
    List<AvroOutputDomainRecord> outputDomainShards = new ArrayList<>();
    String[] cli =
        new String[] {
          "--input", domainPath.toString(),
          "--output_dir", outputDomainShardsDir.toString(),
          "--num_shards", String.valueOf(numShard),
          "--domain"
        };

    avroShard.main(cli);

    assertTrue(Files.exists(outputDomainShardsDir));

    List<String> domainShardPaths = Arrays.asList(outputDomainShardsDir.toFile().list());

    assertEquals(domainShardPaths.size(), numShard);

    for (String domainShardPath : domainShardPaths) {
      outputDomainShards.addAll(readDomain(outputDomainShardsDir.resolve(domainShardPath)));
    }

    assertEquals(outputDomainShards.size(), readDomain(domainPath).size());
  }

  private ImmutableList<AvroOutputDomainRecord> readDomain(Path domainPath) throws IOException {
    AvroOutputDomainReaderFactory domainReaderFactory =
        injector.getInstance(AvroOutputDomainReaderFactory.class);
    ImmutableList<AvroOutputDomainRecord> records;
    try (InputStream avroStream = Files.newInputStream(domainPath);
        AvroOutputDomainReader reader = domainReaderFactory.create(avroStream)) {
      records = reader.streamRecords().collect(toImmutableList());
    }
    return records;
  }

  private ImmutableList<AvroReportRecord> readReport(Path domainPath) throws IOException {
    AvroReportsReaderFactory reportsReaderFactory =
        injector.getInstance(AvroReportsReaderFactory.class);
    ImmutableList<AvroReportRecord> records;
    try (InputStream avroStream = Files.newInputStream(domainPath);
        AvroReportsReader reader = reportsReaderFactory.create(avroStream)) {
      records = reader.streamRecords().collect(toImmutableList());
    }
    return records;
  }

  private static final class TestEnv extends AbstractModule {}
}
