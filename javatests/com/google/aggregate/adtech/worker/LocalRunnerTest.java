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

package com.google.aggregate.adtech.worker;

import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromString;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.common.math.Stats;
import com.google.common.util.concurrent.ServiceManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalRunnerTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  private Path workingDirectory;
  private Path baseDirectory;
  private Path expectedOutputJson;
  private Path outputDirectoryPath;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setUp() throws IOException {
    workingDirectory = testWorkingDir.getRoot().toPath();
    outputDirectoryPath = Path.of("worker/testing/data/library/");
    baseDirectory = Path.of("worker/testing/data/library/");
    expectedOutputJson =
        Paths.get("worker/testing/data/library/output/expected_output_no_noise.json");
  }

  @Test
  public void parameter_validation_does_not_throw_empty() throws IOException {
    String[] cli = new String[] {};
    LocalRunner.internalMain(cli);
  }

  @Test
  public void parameter_validation_does_not_throw_valid() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--domain_avro_file", "dummy_path",
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void requiredParamTest_inputAvroFile() {
    String[] cli =
        new String[] {
          "--output_directory", "dummy_path", "--domain_avro_file", "dummy_path",
        };
    assertThrows(ParameterException.class, () -> LocalRunner.internalMain(cli));
  }

  @Test
  public void requiredParamTest_output_directory() {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
        };
    assertThrows(ParameterException.class, () -> LocalRunner.internalMain(cli));
  }

  @Test
  public void requiredParamTest_domainAvroFile() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
        };
    assertThrows(ParameterException.class, () -> LocalRunner.internalMain(cli));
  }

  @Test
  public void paramTest_epsilon_does_not_throw() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--epsilon", "7",
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void paramTest_epsilon_0() {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--epsilon", "0",
        };
    assertThrows(
        "--epsilon param should >0 and <=64",
        ParameterException.class,
        () -> LocalRunner.internalMain(cli));
  }

  @Test
  public void paramTest_epsilon_greater_than_0() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--epsilon", "0.1",
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void paramTest_epsilon_neg() {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--epsilon", "100",
        };
    assertThrows(
        "--epsilon param should >0 and <=64",
        ParameterException.class,
        () -> LocalRunner.internalMain(cli));
  }

  @Test
  public void paramTest_debug_run() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--domain_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--debug_run"
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void paramTest_print_licenses() throws IOException {
    String[] cli =
        new String[] {
          "--print_licenses",
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void paramTest_help() throws IOException {
    String[] cli =
        new String[] {
          "--help",
        };
    LocalRunner.internalMain(cli);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set0()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(0).toString();
    String pathToDomain = getDomainAvroFile(0).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(0);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));
    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  /**
   * Test that the local runner doesn't throw errors if the file paths provided are to files in the
   * current directory
   */
  @Test
  public void testMainMethodJsonOutputConstantNoise_Set0_localPaths() throws Exception {
    Path currentDir = Paths.get("");

    Path pathToInput = getInputBatchAvro(0);
    Path pathToDomain = getDomainAvroFile(0);

    // Copy the input files to the local directory
    Path localInputPath = pathToInput.getFileName();
    Path localDomainPath = pathToDomain.getFileName();
    Path localOutputPath = currentDir.resolve("output");

    Files.copy(pathToInput, localInputPath);
    Files.copy(pathToDomain, localDomainPath);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          localInputPath.toString(),
          "--domain_avro_file",
          localDomainPath.toString(),
          "--output_directory",
          localOutputPath.toString(),
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = localOutputPath.resolve("output.json");
    Path expectedOutputJson = getExpectedOutputJson(0);
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));
    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set0_skipDomain()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(0).toString();
    String pathToDomain = getDomainAvroFile(0).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(0);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--skip_domain",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));
    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set2()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(2).toString();
    String pathToDomain = getDomainAvroFile(2).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(2);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));
    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set3()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(3).toString();
    String pathToDomain = getDomainAvroFile(3).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(3);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));
    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1_no_noise()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(1).toString();
    String pathToDomain = getDomainAvroFile(1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    // Path expectedOutputJson = getExpectedOutputJson(3);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> nonZeroFacts =
        output.stream()
            .filter((aggregatedFact) -> aggregatedFact.metric() != 0)
            .collect(Collectors.toList());
    assertThat(nonZeroFacts).hasSize(0);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1_noise_epsilon64()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(1).toString();
    String pathToDomain = getDomainAvroFile(1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    // Path expectedOutputJson = getExpectedOutputJson(3);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          // "--no_noising",
          "--epsilon",
          "64",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.metric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));
    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(1024);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(2048);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1_noise_epsilon1()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(1).toString();
    String pathToDomain = getDomainAvroFile(1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    // Path expectedOutputJson = getExpectedOutputJson(3);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          // "--no_noising",
          "--epsilon",
          "1",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.metric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));
    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(65536);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(131072);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1_noise_default_epsilon10()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(1).toString();
    String pathToDomain = getDomainAvroFile(1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    // Path expectedOutputJson = getExpectedOutputJson(3);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          // "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.metric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));
    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(6552);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(13108);
  }

  private List<AggregatedFact> convertToAggregatedFact(JsonNode jsonNode) {
    List<AggregatedFact> writtenResults = new ArrayList<>();
    jsonNode
        .iterator()
        .forEachRemaining(
            entry -> {
              writtenResults.add(
                  AggregatedFact.create(
                      createBucketFromString(entry.get("bucket").asText()),
                      entry.get("value").asLong()));
            });
    return writtenResults;
  }

  private Path getInputBatchAvro(int set) {
    return baseDirectory.resolve(String.format("input_set_%s/batch.avro", set));
  }

  private Path getDomainAvroFile(int set) {
    return baseDirectory.resolve(String.format("input_set_%s/domain.avro", set));
  }

  private Path getExpectedOutputJson(int set) {
    return baseDirectory.resolve(String.format("expected_output_set_%s/output.json", set));
  }
}
