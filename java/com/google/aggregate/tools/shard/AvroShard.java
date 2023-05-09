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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/*
 * This tool is for sharding Avro report or domain.
 * The reports can be sharded with:
 * bazel run //java/com/google/aggregate/tools/shard:AvroShard \
 *  --input $PWD/20k_reports.avro \
 *  --output_dir $PWD/20k_shards \
 *  --num_shards 20
 * The domain can ba sharded with:
 * bazel run //java/com/google/aggregate/tools/shard:AvroShard \
 *  --input $PWD/20k_domain.avro \
 *  --output_dir $PWD/20k_domain \
 *  --num_shards 20 \
 *  --domain
 */
final class AvroShard {

  static Injector injector = Guice.createInjector(new Env());

  public static void main(String[] args) throws IOException {

    Args cliArgs = new Args();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);

    Path inputPath = Paths.get(cliArgs.input);
    Path outputDirPath = Paths.get(cliArgs.outputDir);
    final AtomicInteger numRecords = new AtomicInteger();
    int numShards = cliArgs.numShards;

    if (cliArgs.domain) {
      try (Stream<AvroOutputDomainRecord> stream = streamDomainRecords(inputPath)) {
        System.out.println("Iterating over domain records");
        stream.forEach(
            shard -> {
              numRecords.incrementAndGet();
            });
        System.out.println("Finished iterating over domain records");
      }
      // Determining stream length is a terminal operation, so the Stream of Domain Records is
      // created twice
      try (Stream<AvroOutputDomainRecord> stream = streamDomainRecords(inputPath)) {
        System.out.printf("Total domain records: %d\n", numRecords.get());
        System.out.printf("Shards: %d\n", numShards);
        System.out.printf("Shard size: %d\n", numRecords.get() / numShards);

        writeDomainRecordsFromStream(stream, outputDirPath, numRecords.get(), numShards);
      }
    } else {
      try (Stream<AvroReportRecord> stream = streamReportRecords(inputPath)) {
        System.out.println("Iterating over records");
        stream.forEach(
            shard -> {
              numRecords.incrementAndGet();
            });
        System.out.println("Finished iterating over records");
      }
      // Determining stream length is a terminal operation, so the Stream of Report Records is
      // created twice
      try (Stream stream = streamReportRecords(inputPath)) {
        System.out.printf("Total records: %d\n", numRecords.get());
        System.out.printf("Shards: %d\n", numShards);
        System.out.printf("Shard size: %d\n", numRecords.get() / numShards);

        writeReportRecordsFromStream(stream, outputDirPath, numRecords.get(), numShards);
      }
    }
  }

  public static Stream<AvroOutputDomainRecord> streamDomainRecords(Path inputPath) {
    AvroOutputDomainReaderFactory domainReaderFactory =
        injector.getInstance(AvroOutputDomainReaderFactory.class);
    Stream<AvroOutputDomainRecord> stream = null;
    try {
      InputStream avroStream = Files.newInputStream(inputPath);
      AvroOutputDomainReader reader = domainReaderFactory.create(avroStream);
      stream = reader.streamRecords();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return stream;
  }

  public static Stream<AvroReportRecord> streamReportRecords(Path inputPath) {
    AvroReportsReaderFactory reportReaderFactory =
        injector.getInstance(AvroReportsReaderFactory.class);
    Stream<AvroReportRecord> stream = null;
    try {
      InputStream avroStream = Files.newInputStream(inputPath);
      AvroReportsReader reader = reportReaderFactory.create(avroStream);
      stream = reader.streamRecords();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return stream;
  }

  private static void writeDomainRecordsFromStream(
      Stream<AvroOutputDomainRecord> records, Path outputDirPath, int numRecords, int numShards)
      throws IOException {

    AvroOutputDomainWriterFactory domainWriterFactory =
        injector.getInstance(AvroOutputDomainWriterFactory.class);

    Files.createDirectories(outputDirPath);

    Spliterator<AvroOutputDomainRecord> recordsSpliterator = records.spliterator();
    int recordsWritten = 0;
    for (int currentShard = 1; currentShard <= numShards; currentShard++) {
      int numRecordsToWrite = getNumRecordsToWrite(numRecords, numShards, currentShard);
      Path shardPath =
          outputDirPath.resolve(
              String.format("shard-domain-%d-of-%d.avro", currentShard, numShards));

      System.out.printf(
          "Writing domain shard %d at %s\n", currentShard, shardPath.toAbsolutePath());

      if (currentShard == numShards) {
        numRecordsToWrite = numRecords - recordsWritten;
      }

      try (OutputStream shardStream = Files.newOutputStream(shardPath, CREATE);
          AvroOutputDomainWriter writer = domainWriterFactory.create(shardStream)) {
        writer.writeRecordsFromSpliterator(
            /* metadata= */ ImmutableList.of(), recordsSpliterator, numRecordsToWrite);
        recordsWritten += numRecordsToWrite;
        System.out.printf("Domain Shard %d written", currentShard);
      }
    }
  }

  private static void writeReportRecordsFromStream(
      Stream<AvroReportRecord> records, Path outputDirPath, int numRecords, int numShards)
      throws IOException {

    AvroReportWriterFactory reportWriterFactory =
        injector.getInstance(AvroReportWriterFactory.class);

    Files.createDirectories(outputDirPath);

    Spliterator<AvroReportRecord> recordsSpliterator = records.spliterator();

    int recordsWritten = 0;
    for (int currentShard = 1; currentShard <= numShards; currentShard++) {
      int numRecordsToWrite = getNumRecordsToWrite(numRecords, numShards, currentShard);
      Path shardPath =
          outputDirPath.resolve(
              String.format("shard-report-%d-of-%d.avro", currentShard, numShards));

      System.out.printf("Writing shard %d at %s\n", currentShard, shardPath.toAbsolutePath());

      if (currentShard == numShards) {
        numRecordsToWrite = numRecords - recordsWritten;
      }
      try (OutputStream shardStream = Files.newOutputStream(shardPath, CREATE);
          AvroReportWriter writer = reportWriterFactory.create(shardStream)) {
        writer.writeRecordsFromSpliterator(
            /* metadata= */ ImmutableList.of(), recordsSpliterator, numRecordsToWrite);
        recordsWritten += numRecordsToWrite;
        System.out.printf("Report Shard %d written", currentShard);
      }
    }
  }

  private static int getNumRecordsToWrite(int numRecords, int numShards, int currentShard) {
    int minRecordsPerShard = numRecords / numShards;
    int numShardsWithSpillover = numRecords % numShards;
    int spilloverRecordsInCurrentShard = currentShard < numShardsWithSpillover ? 1 : 0;

    return minRecordsPerShard + spilloverRecordsInCurrentShard;
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
