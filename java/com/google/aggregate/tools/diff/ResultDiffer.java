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
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * Differ for aggregation result Avro files
 *
 * <p>Add files for the first result by repeatedly applying the flag --first_result PATH; similarly
 * for --second_result.
 *
 * <p>The diffing should happen on results that are not noised.
 */
public final class ResultDiffer {

  private final AvroResultsFileReader resultsFileReader;

  @Inject
  ResultDiffer(AvroResultsFileReader resultsFileReader) {
    this.resultsFileReader = resultsFileReader;
  }

  public static void main(String[] args) throws IOException {
    Args cliArgs = new Args();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);

    System.out.println("First results paths: " + cliArgs.firstResult);
    System.out.println("Second results paths: " + cliArgs.secondResult);

    Injector injector = Guice.createInjector(new Env());
    ResultDiffer differ = injector.getInstance(ResultDiffer.class);

    ImmutableList<Path> firstResults =
        cliArgs.firstResult.stream().map(Paths::get).collect(toImmutableList());
    ImmutableList<Path> secondResults =
        cliArgs.secondResult.stream().map(Paths::get).collect(toImmutableList());

    MapDifference<BigInteger, AggregatedFact> diff =
        differ.diffResults(firstResults, secondResults);

    System.out.println("Only in first:");
    System.out.println("===========================");
    System.out.println(diff.entriesOnlyOnLeft());
    System.out.println("===========================");

    System.out.println("Only in second:");
    System.out.println("===========================");
    System.out.println(diff.entriesOnlyOnRight());
    System.out.println("===========================");

    System.out.println("Differing aggregations:");
    System.out.println("===========================");
    System.out.println(diff.entriesDiffering());
  }

  MapDifference<BigInteger, AggregatedFact> diffResults(
      ImmutableList<Path> firstResults, ImmutableList<Path> secondResults) {
    Stream<AggregatedFact> firstResultConcatenated =
        firstResults.stream().flatMap(this::readAggregatedFacts);
    Stream<AggregatedFact> secondResultConcatenated =
        secondResults.stream().flatMap(this::readAggregatedFacts);
    return diffResults(firstResultConcatenated, secondResultConcatenated);
  }

  private Stream<AggregatedFact> readAggregatedFacts(Path file) {
    try {
      return resultsFileReader.readAvroResultsFile(file).stream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static MapDifference<BigInteger, AggregatedFact> diffResults(
      Stream<AggregatedFact> firstResults, Stream<AggregatedFact> secondResults) {
    ImmutableMap<BigInteger, AggregatedFact> firstResultsKeyed = keyAggregatedFacts(firstResults);
    ImmutableMap<BigInteger, AggregatedFact> secondResultsKeyed = keyAggregatedFacts(secondResults);

    return Maps.difference(firstResultsKeyed, secondResultsKeyed);
  }

  private static ImmutableMap<BigInteger, AggregatedFact> keyAggregatedFacts(
      Stream<AggregatedFact> facts) {
    return facts.collect(toImmutableMap(AggregatedFact::getBucket, Function.identity()));
  }

  private static final class Env extends AbstractModule {}

  private static final class Args {

    @Parameter(names = "--first_result")
    private List<String> firstResult;

    @Parameter(names = "--second_result")
    private List<String> secondResult;
  }
}
