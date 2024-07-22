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
import com.google.privacysandbox.otel.OTelConfiguration;
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

  private Path sampleDataDirectory;
  private Path expectedOutputJson;
  private Path outputDirectoryPath;
  private ObjectMapper objectMapper = new ObjectMapper();

  private final String ATTRIBUTION_DATASET_1 = "attribution_1";
  private final String ATTRIBUTION_DATASET_2 = "attribution_2";
  private final String ATTRIBUTION_DATASET_3 = "attribution_3";
  private final String ATTRIBUTION_DATASET_4 = "attribution_4";

  private final String PROTECTED_AUDIENCE_DATASET_1 = "protected_audience_1";
  private final String PROTECTED_AUDIENCE_DATASET_2 = "protected_audience_2";

  private final String THRESHOLDING_DATASET_1 = "thresholding_1";

  private final String OUTPUT_SET_1 = "1";
  private final String OUTPUT_SET_3 = "3";
  private final String OUTPUT_SET_4 = "4";
  private final String OUTPUT_SET_THRESHOLDING = "thresholding";

  @Before
  public void setUp() throws IOException {
    OTelConfiguration.resetForTest();
    workingDirectory = testWorkingDir.getRoot().toPath();
    outputDirectoryPath = Path.of("worker/testing/data/library/");
    baseDirectory = Path.of("worker/testing/data/library/");
    sampleDataDirectory = Path.of("sampledata/");
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
  public void parameter_validation_does_not_throw_domain_missing() throws IOException {
    String[] cli =
        new String[] {
          "--input_data_avro_file", "dummy_path",
          "--output_directory", "dummy_path",
          "--skip_domain"
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

  /** Throws null pointer exception because internalMain() returns null. */
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

  /**
   * Test to validate sampledata.
   *
   * @throws IOException
   * @throws TimeoutException
   */
  @Test
  public void testMainMethodJsonOutputConstantNoise_sampleData()
      throws IOException, TimeoutException {
    String pathToAvro = sampleDataDirectory.resolve("output_debug_reports.avro").toString();
    String pathToDomain = sampleDataDirectory.resolve("output_domain.avro").toString();
    Path expectedOutputJson = sampleDataDirectory.resolve("aggregate_result.json");

    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          sampleDataDirectory.toString(),
          "--no_noising",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = sampleDataDirectory.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));

    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_1).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
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
  public void testSkipDomain_NoThresholding() throws Exception {
    String pathToAvro = getInputBatchAvro(THRESHOLDING_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_THRESHOLDING);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--output_directory",
          outputDirectory,
          "--no_noising",
          "--json_output",
          "--skip_domain"
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
  public void testMainMethodJsonOutputConstantNoise_Set1_localPaths() throws Exception {
    Path currentDir = Paths.get("");

    Path pathToInput = getInputBatchAvro(ATTRIBUTION_DATASET_1);
    Path pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_1);

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
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> expectedOutput =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(expectedOutputJson)));

    assertThat(output).containsExactlyElementsIn(expectedOutput);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set1_skipDomain()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_1).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
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
  public void testMainMethodJsonOutputConstantNoise_Set3()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_3).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_3).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_3);
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
  public void testMainMethodJsonOutputConstantNoise_Set4()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_4).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_4).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_4);
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
  public void testMainMethodJsonOutputConstantNoise_Set2_no_noise()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
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
            .filter((aggregatedFact) -> aggregatedFact.getMetric() != 0)
            .collect(Collectors.toList());

    assertThat(nonZeroFacts).hasSize(0);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set2_noise_epsilon64()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
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
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.getMetric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));

    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(1024);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(2048);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set2_noise_epsilon1()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
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
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.getMetric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));

    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(65536);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(131072);
  }

  @Test
  public void testMainMethodJsonOutputConstantNoise_Set2_noise_default_epsilon10()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(ATTRIBUTION_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(ATTRIBUTION_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
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
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.getMetric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));

    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(6552);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(13108);
  }

  /** Test aggregation for FLEDGE reports with domain processing. */
  @Test
  public void testMainMethodJsonOutput_fledge_set1_constantNoise_withDomain()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(PROTECTED_AUDIENCE_DATASET_1).toString();
    String pathToDomain = getDomainAvroFile(PROTECTED_AUDIENCE_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
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

  /** Test aggregation for FLEDGE reports skipping domain processing. */
  @Test
  public void testMainMethodJsonOutput_fledge_set1_constantNoise_skipDomain()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(PROTECTED_AUDIENCE_DATASET_1).toString();
    String pathToDomain = getDomainAvroFile(PROTECTED_AUDIENCE_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
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

  /** Test aggregation that no domain specified and skip_domain set works. */
  @Test
  public void skipDomain_noDomainFile() throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(PROTECTED_AUDIENCE_DATASET_1).toString();
    String pathToDomain = getDomainAvroFile(PROTECTED_AUDIENCE_DATASET_1).toString();
    String outputDirectory = outputDirectoryPath.toString();
    Path expectedOutputJson = getExpectedOutputJson(OUTPUT_SET_1);
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
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

  /**
   * Test aggregation for FLEDGE reports including domain processing and noising. Epsilon set to 64.
   */
  @Test
  public void testMainMethodJsonOutput_fledge_set2_withNoise_epsilon64()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(PROTECTED_AUDIENCE_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(PROTECTED_AUDIENCE_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
    String[] cli =
        new String[] {
          "--input_data_avro_file",
          pathToAvro,
          "--domain_avro_file",
          pathToDomain,
          "--output_directory",
          outputDirectory,
          "--epsilon",
          "64",
          "--json_output",
        };
    ServiceManager serviceManager = LocalRunner.internalMain(cli);
    serviceManager.awaitStopped(Duration.ofMinutes(5));

    Path outputJson = outputDirectoryPath.resolve("output.json");
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    Stream<Long> allMetrics = output.stream().map(aggregatedFact -> aggregatedFact.getMetric());
    Stats statsAccumulator = Stats.of(allMetrics.collect(Collectors.toList()));

    assertThat(statsAccumulator.count()).isEqualTo(10000);
    // min value std.deviation/sqrt(2)
    assertThat(statsAccumulator.populationStandardDeviation()).isGreaterThan(1024);
    // max value sqrt(2)*std.deviation
    assertThat(statsAccumulator.populationStandardDeviation()).isLessThan(2048);
  }

  /** Test aggregation for FLEDGE reports including domain processing and constant noising. */
  @Test
  public void testMainMethodJsonOutput_fledge_set2_constantNoise()
      throws IOException, TimeoutException, InterruptedException {
    String pathToAvro = getInputBatchAvro(PROTECTED_AUDIENCE_DATASET_2).toString();
    String pathToDomain = getDomainAvroFile(PROTECTED_AUDIENCE_DATASET_2).toString();
    String outputDirectory = outputDirectoryPath.toString();
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
    // Verify aggregated metrics value is zero because of no overlap with domain.
    List<AggregatedFact> output =
        convertToAggregatedFact(objectMapper.readTree(Files.newInputStream(outputJson)));
    List<AggregatedFact> nonZeroFacts =
        output.stream()
            .filter((aggregatedFact) -> aggregatedFact.getMetric() != 0)
            .collect(Collectors.toList());

    assertThat(nonZeroFacts).isEmpty();
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
                      entry.get("metric").asLong()));
            });
    return writtenResults;
  }

  private Path getInputBatchAvro(String set) {
    return baseDirectory.resolve(String.format("input_set_%s/batch.avro", set));
  }

  private Path getDomainAvroFile(String set) {
    return baseDirectory.resolve(String.format("input_set_%s/domain.avro", set));
  }

  private Path getExpectedOutputJson(String set) {
    return baseDirectory.resolve(String.format("expected_output_set_%s/output.json", set));
  }
}
