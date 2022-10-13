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
import static java.nio.file.StandardOpenOption.CREATE;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.aggregate.protocol.avro.AvroOutputDomainReader;
import com.google.aggregate.protocol.avro.AvroOutputDomainReaderFactory;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriter;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.aggregate.protocol.avro.AvroReportsReader;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** A tool for sharding Avro report or domain inputs */
final class AvroShard {

  static Injector injector = Guice.createInjector(new Env());

  public static void main(String[] args) throws IOException {

    Args cliArgs = new Args();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);

    Path inputPath = Paths.get(cliArgs.input);
    Path outputDirPath = Paths.get(cliArgs.outputDir);
    int recordSize;
    int shardSize;
    int shardNum = cliArgs.numShards;

    if (cliArgs.domain) {
      ImmutableList<AvroOutputDomainRecord> records = readDomainRecords(inputPath);
      recordSize = records.size();
      shardSize = recordSize / shardNum;
      System.out.printf("Total records: %d\n", recordSize);
      System.out.printf("Shards: %d\n", shardNum);
      System.out.printf("Shard size: %d\n", shardSize);
      writeDomainRecords(records, outputDirPath, shardSize, shardNum);
    } else {
      ImmutableList<AvroReportRecord> records = readReportRecords(inputPath);
      recordSize = records.size();
      shardSize = recordSize / shardNum;
      System.out.printf("Total records: %d\n", recordSize);
      System.out.printf("Shards: %d\n", shardNum);
      System.out.printf("Shard size: %d\n", shardSize);
      writeReportRecords(records, outputDirPath, shardSize, shardNum);
    }
  }

  public static ImmutableList<AvroOutputDomainRecord> readDomainRecords(Path inputPath)
      throws IOException {
    AvroOutputDomainReaderFactory domainReaderFactory =
        injector.getInstance(AvroOutputDomainReaderFactory.class);
    ImmutableList<AvroOutputDomainRecord> records;
    try (InputStream avroStream = Files.newInputStream(inputPath);
        AvroOutputDomainReader reader = domainReaderFactory.create(avroStream)) {
      records = reader.streamRecords().collect(toImmutableList());
    }
    return records;
  }

  public static ImmutableList<AvroReportRecord> readReportRecords(Path inputPath)
      throws IOException {
    AvroReportsReaderFactory reportReaderFactory =
        injector.getInstance(AvroReportsReaderFactory.class);
    ImmutableList<AvroReportRecord> records;
    try (InputStream avroStream = Files.newInputStream(inputPath);
        AvroReportsReader reader = reportReaderFactory.create(avroStream)) {
      records = reader.streamRecords().collect(toImmutableList());
    }
    return records;
  }

  private static void writeDomainRecords(
      ImmutableList<AvroOutputDomainRecord> records,
      Path outputDirPath,
      int shardSize,
      int shardNum)
      throws IOException {

    AvroOutputDomainWriterFactory domainWriterFactory =
        injector.getInstance(AvroOutputDomainWriterFactory.class);
    int runningShard = 0;
    for (List<AvroOutputDomainRecord> shard : Iterables.partition(records, shardSize)) {
      Path shardPath =
          outputDirPath.resolve(
              String.format("shard-domain-%d-of-%d.avro", runningShard, shardNum));

      System.out.printf(
          "Writing domain shard %d at %s\n", runningShard, shardPath.toAbsolutePath());

      try {
        if (!Files.exists(outputDirPath)) {
          Files.createDirectories(outputDirPath);
        }
        OutputStream shardStream = Files.newOutputStream(shardPath, CREATE);
        AvroOutputDomainWriter writer = domainWriterFactory.create(shardStream);
        writer.writeRecords(/* metadata= */ ImmutableList.of(), ImmutableList.copyOf(shard));
      } catch (Exception ex) {
        System.out.println(ex.getMessage());
      }
      System.out.println("Domain shard written");
      runningShard++;
    }
  }

  private static void writeReportRecords(
      ImmutableList<AvroReportRecord> records, Path outputDirPath, int shardSize, int shardNum)
      throws IOException {

    AvroReportWriterFactory reportWriterFactory =
        injector.getInstance(AvroReportWriterFactory.class);
    int runningShard = 0;
    for (List<AvroReportRecord> shard : Iterables.partition(records, shardSize)) {
      Path shardPath =
          outputDirPath.resolve(String.format("shard-%d-of-%d.avro", runningShard, shardNum));

      System.out.printf("Writing shard %d at %s\n", runningShard, shardPath.toAbsolutePath());

      try (OutputStream shardStream = Files.newOutputStream(shardPath, CREATE);
          AvroReportWriter writer = reportWriterFactory.create(shardStream)) {
        writer.writeRecords(/* metadata= */ ImmutableList.of(), ImmutableList.copyOf(shard));
      }
      System.out.println("Shard written");
      runningShard++;
    }
  }

  private static final class Env extends AbstractModule {}

  private static final class Args {

    @Parameter(names = "--input", required = true)
    private String input;

    @Parameter(
        names = "--output_dir",
        description = "The directory for storing outputs shards.",
        required = true)
    private String outputDir;

    @Parameter(
        names = "--num_shards",
        description = "Number of shards to generate",
        required = true)
    private int numShards;

    @Parameter(
        names = "--domain",
        description = "If the input is domain file, this flag should be set.")
    private boolean domain = false;
  }
}
