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

package com.google.aggregate.tools.diff;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.beust.jcommander.JCommander;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.testing.LocalAggregationWorkerRunner;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter.FileWriteException;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroResultFileWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * A tool to run diff checks and update golden.
 *
 * <p>Provide test inputs via --test_input, --test_key, and golden via --test_golden. To
 * create/update golden, add the additional flag --update_golden.
 *
 * <p>The tool utilizes {@link LocalAggregationWorkerRunner} to process test inputs.
 */
public final class DiffRunner {

  public static void main(String[] cliArgs) throws Exception {
    Injector injector = Guice.createInjector(new Env());
    LocalAvroResultFileWriter localAvroResultFileWriter =
        injector.getInstance(LocalAvroResultFileWriter.class);
    AvroResultsFileReader avroResultFileReader = injector.getInstance(AvroResultsFileReader.class);

    DiffRunnerArgs args = new DiffRunnerArgs();
    JCommander.newBuilder().addObject(args).build().parse(cliArgs);

    // For storing intermediate results we don't care about.
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path tempDir = Files.createDirectory(fileSystem.getPath("/tmp"));

    Stream<String> testArgs =
        Stream.of(
            "--local_file_single_puller_path",
            args.getTestInput(),
            "--local_file_decryption_key_path",
            args.getTestKey(),
            "--local_output_domain_path",
            args.getTestOutputDomainDir(),
            "--noising_epsilon",
            String.valueOf(args.getNoisingEpsilon()),
            "--noising_l1_sensitivity",
            String.valueOf(args.getNoisingL1Sensitivity()));

    if (args.isDomainOptional()) {
      testArgs = Stream.concat(testArgs, Stream.of("--domain_optional"));
    }

    DiffRunnerResult diffRunnerResult;

    if (args.isUpdateGolden()) {
      diffRunnerResult =
          runUpdateGolden(
              testArgs.collect(toImmutableList()),
              Paths.get(args.getTestGolden()),
              tempDir,
              localAvroResultFileWriter);
    } else {
      diffRunnerResult =
          runDiffResults(
              testArgs.collect(toImmutableList()),
              Paths.get(args.getTestGolden()),
              tempDir,
              avroResultFileReader);
    }
    System.out.println(diffRunnerResult.message());
  }

  static DiffRunnerResult runUpdateGolden(
      ImmutableList<String> args,
      Path output,
      Path tempDir,
      LocalAvroResultFileWriter localAvroResultFileWriter)
      throws IOException, FileWriteException {
    Files.deleteIfExists(output);

    writeAggregatedFacts(runAndGetResult(args, tempDir), output, localAvroResultFileWriter);
    return DiffRunnerResult.create("New golden file is written to: " + output.toAbsolutePath());
  }

  static DiffRunnerResult runDiffResults(
      ImmutableList<String> args,
      Path golden,
      Path tempDir,
      AvroResultsFileReader avroResultFileReader) {
    MapDifference<BigInteger, AggregatedFact> diffs =
        ResultDiffer.diffResults(
            runAndGetResult(args, tempDir).stream(),
            readAggregatedFacts(golden, avroResultFileReader));

    if (diffs.areEqual()) {
      return DiffRunnerResult.create("No diff is found between left(test) and right(golden).");
    } else {
      return DiffRunnerResult.create("Found diffs between left(test) and right(golden).\n" + diffs);
    }
  }

  private static ImmutableList<AggregatedFact> runAndGetResult(
      ImmutableList<String> args, Path tempDir) {
    try {
      LocalAggregationWorkerRunner localAggregationWorker =
          LocalAggregationWorkerRunner.create(/* rootDir= */ tempDir, args.toArray(new String[0]));
      localAggregationWorker.run();
      ImmutableList<AggregatedFact> resultAggregatedFacts =
          localAggregationWorker.waitForAggregation();
      return resultAggregatedFacts.stream()
          .map(fact -> AggregatedFact.create(fact.getBucket(), fact.getMetric()))
          .collect(toImmutableList());

    } catch (ResultLogException | TimeoutException e) {
      throw new LocalWorkerRunnerException(e);
    }
  }

  private static Stream<AggregatedFact> readAggregatedFacts(
      Path file, AvroResultsFileReader avroResultFileReader) {
    try {
      return avroResultFileReader.readAvroResultsFile(file).stream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeAggregatedFacts(
      ImmutableList<AggregatedFact> results,
      Path file,
      LocalAvroResultFileWriter localAvroResultFileWriter)
      throws FileWriteException {
    localAvroResultFileWriter.writeLocalFile(results.stream(), file);
  }

  private static class LocalWorkerRunnerException extends RuntimeException {
    LocalWorkerRunnerException(Throwable e) {
      super(e);
    }
  }

  private static final class Env extends AbstractModule {}
}
