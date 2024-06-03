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

import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;

import com.beust.jcommander.JCommander;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.testing.InMemoryResultLogger;
import com.google.aggregate.adtech.worker.testing.MaterializedAggregationResults;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ServiceManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.privacysandbox.otel.OTelConfiguration;
import com.google.protobuf.util.JsonFormat;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.operator.shared.testing.AwsHermeticTestHelper;
import com.google.scp.operator.shared.testing.SimulationTestParams;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * AggregationWorkerHermeticTest covers tests with different worker args and noises. The threshold
 * used in AggregationWorkerHermeticTest is calculated from formula in {@code ThresholdSupplier}.
 * Given noising_epsilon, noising_delta and noising_l1_sensitivity in provided args, the threshold
 * is round 4.57 in this test.
 */
@RunWith(JUnit4.class)
public class AggregationWorkerHermeticTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();

  private AggregationWorker worker;
  private ServiceManager serviceManager;
  private Path hybridKey;
  private Path reportsAvro;
  private Path reportShardsDir;
  private Path domainAvro;
  private Path domainShardsDir;
  private Path resultFile;
  private String[] args;
  private Path stopwatchFile;

  private ImmutableList<String> simulationInputFileLines, allThresholdedSimulationInputFileLines;
  private ImmutableList<MetadataElement> metadata;

  private static final String UUID1 = "416abdc0-0697-4021-9300-10c1224ba204";
  private static final String UUID2 = "914a2679-bad6-46c0-bb8e-26b8c5757008";
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private AvroReportWriterFactory writerFactory;
  @Inject private AvroOutputDomainWriterFactory domainWriterFactory;

  @Before
  public void setUp() throws Exception {
    // TODO[b/275585065]: Avoid using GlobalOpenTelemetry directly.
    OTelConfiguration.resetForTest();
    Path testWorkingDirPath = testWorkingDir.getRoot().toPath();
    stopwatchFile = testWorkingDirPath.resolve("stopwatches.txt");
    hybridKey = testWorkingDirPath.resolve("hybrid.key");
    reportsAvro = testWorkingDirPath.resolve("reports.avro");
    reportShardsDir = testWorkingDirPath.resolve("report_shards");
    resultFile = testWorkingDirPath.resolve("results.json");
    domainAvro = testWorkingDirPath.resolve("domain.avro");
    domainShardsDir = testWorkingDirPath.resolve("domain_shards");
    // Result of summing the values of the keys: 22:56, 33:33, 44:123, 55:3
    simulationInputFileLines =
        ImmutableList.of(
            "22:1,33:2",
            "22:10,55:3,33:10",
            "22:15",
            "22:10",
            "22:20,33:15",
            "44:123,33:5",
            "33:1");
    allThresholdedSimulationInputFileLines = ImmutableList.of("22:1,33:2", "44:3,55:1");
    metadata =
        ImmutableList.of(
            MetadataElement.create(/* key= */ "foo", /* value= */ "bar"),
            MetadataElement.create(/* key= */ "abc", /* value= */ "xyz"));
    // Set domain_optional arg to true for most test cases in order to bypass creating domain files.
    // Otherwise, the aggregated result would be empty if domain is required without domain files.
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false, /* outputDomain= */ false, /* domainOptional= */ true);
    Files.createDirectory(reportShardsDir);
    Files.createDirectory(domainShardsDir);
  }

  @After
  public void tearDown() throws Exception {
    worker = null;
    serviceManager = null;
  }

  @Test
  public void localTestNoNoising_doesThresholding() throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // Some keys got filtered out due to delta thresholding.
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 56, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 33, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 123, /* unnoisedMetric= */ 123L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2, expectedFact4);
  }

  @Test
  public void localTestNoNoisingNoEncryption() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ true, /* outputDomain= */ false, /* domainOptional= */ true);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(allThresholdedSimulationInputFileLines)
            .setNoEncryption(true)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    System.out.println(Arrays.toString(args));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
  }

  @Test
  public void worker_domainOptionalFalse_noOutputDomainPath_throwsException() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ true, /* outputDomain= */ false, /* domainOptional= */ false);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setNoEncryption(true)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();
    assertThat(actualResult.getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.INVALID_JOB.name());
    assertThat(actualResult.getReturnMessage())
        .containsMatch(
            "Job parameters for the job 'request' does not have output domain location specified in"
                + " 'output_domain_bucket_name' and 'output_domain_blob_prefix' fields. Please"
                + " refer to the API documentation for output domain parameters at"
                + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md");
  }

  @Test
  public void worker_domainOptionalFalse_outputDomainKeysFile_throwsException() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ true, /* outputDomain= */ true, /* domainOptional= */ false);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setNoEncryption(true)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();
    assertThat(actualResult.getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED.name());
    assertThat(actualResult.getReturnMessage())
        .containsMatch(".*Exception while reading domain input data.*");
  }

  @Test
  public void worker_protectedAudienceApiTest_domainRequired_constantNoising() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false, /* outputDomain= */ true, /* domainOptional= */ false);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setNoEncryption(false)
            .setApiType(SharedInfo.PROTECTED_AUDIENCE_API)
            .build());

    Files.copy(reportsAvro, reportShardsDir.resolve("reports.avro"));

    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "11");
    Files.move(domainAvro, domainShardsDir.resolve("domain_1.avro"));
    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "33");
    Files.move(domainAvro, domainShardsDir.resolve("domain_2.avro"));
    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "55");
    Files.move(domainAvro, domainShardsDir.resolve("domain_3.avro"));

    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    runWorker();

    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(11), /* metric= */ 10, /* unnoisedMetric= */ 0L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 43, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 13, /* unnoisedMetric= */ 3L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2, expectedFact3);
  }

  @Test
  public void worker_sharedStorageApiTest_domainOptional_constantNoising() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false, /* outputDomain= */ false, /* domainOptional= */ true);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setNoEncryption(false)
            .setApiType(SharedInfo.SHARED_STORAGE_API)
            .build());

    Files.copy(reportsAvro, reportShardsDir.resolve("reports.avro"));

    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    runWorker();

    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 66, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 43, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 133, /* unnoisedMetric= */ 123L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 13, /* unnoisedMetric= */ 3L);
    assertThat(factList)
        .containsExactly(expectedFact1, expectedFact2, expectedFact3, expectedFact4);
  }

  @Test
  public void localTestConstantNoising() throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // All values have 10 added due to constant noising.
    // Nothing gets filtered out now because everything meets the threshold.
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 66, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 43, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 133, /* unnoisedMetric= */ 123L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 13, /* unnoisedMetric= */ 3L);
    assertThat(factList)
        .containsExactly(expectedFact1, expectedFact2, expectedFact3, expectedFact4);
  }

  @Test
  public void localTestConstantNoising_shardedReport() throws Exception {
    simulationInputFileLines = ImmutableList.of("22:1,33:2", "44:10", "55:10,66:2", "66:5,77:1");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));

    /*
    Generate input for second shard with same input file lines.
    Reports will have unique report_ids
    * */
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(1));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // '22' and '77' get filtered out due to thresholding
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 5, /* unnoisedMetric= */ 4L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 21, /* unnoisedMetric= */ 20L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 21, /* unnoisedMetric= */ 20L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(66), /* metric= */ 15, /* unnoisedMetric= */ 14L);
    assertThat(factList)
        .containsExactly(expectedFact1, expectedFact2, expectedFact3, expectedFact4);
  }

  @Test
  public void localTestConstantNoising_shardedDomain() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false, /* outputDomain= */ true, /* domainOptional= */ true);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "55");
    Files.move(domainAvro, domainShardsDir.resolve("shard_1.avro"));
    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "11");
    Files.move(domainAvro, domainShardsDir.resolve("shard_2.avro"));

    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // All values have 10 added due to constant noising.
    // Nothing gets filtered out now because everything meets the threshold.
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 66, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 43, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 133, /* unnoisedMetric= */ 123L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 13, /* unnoisedMetric= */ 3L);
    AggregatedFact expectedFact5 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(11), /* metric= */ 10, /* unnoisedMetric= */ 0L);

    assertThat(factList)
        .containsExactly(expectedFact1, expectedFact2, expectedFact3, expectedFact4, expectedFact5);
  }

  @Test
  public void localTestConstantNoising_DomainRequired() throws Exception {
    args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false, /* outputDomain= */ true, /* domainOptional= */ false);
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    AwsHermeticTestHelper.writeDomainAvroFile(domainWriterFactory, domainAvro, "22");
    Files.copy(domainAvro, domainShardsDir.resolve("shard.avro"));

    setupLocalAggregationWorker(args);
    Injector injector = worker.getInjector();
    FakeNoiseApplierSupplier noiserSupplier = injector.getInstance(FakeNoiseApplierSupplier.class);
    noiserSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // All values have 10 added due to constant noising.
    // Only key 22 is in the result because only the key in domain would be output when domain is
    // required.
    AggregatedFact expectedFact =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 66, /* unnoisedMetric= */ 56L);

    assertThat(factList).containsExactly(expectedFact);
  }

  // TODO(b/260642993): Fix sharedInfo and encryption/decryption issues
  @Test
  public void localTestUsingCustomizedAvroReport() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ UUID1,
                /* payload= */ new byte[] {0x01},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ UUID2,
                /* payload= */ new byte[] {0x02, 0x03},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
  }

  // TODO(b/260642993): Fix sharedInfo and encryption/decryption issues
  @Test
  public void localTestUsingSameKeyAvroReports() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ UUID1,
                /* payload= */ new byte[] {0x01},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ UUID1,
                /* payload= */ new byte[] {0x02, 0x03},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
  }

  @Test
  public void localTestUsingEmptyAvroReports() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords = ImmutableList.of();
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(ErrorSummary.getDefaultInstance())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS.name())
                .build());
  }

  // TODO(b/260642993): Fix sharedInfo and encryption/decryption issues
  @Test
  public void localTestUsingAllEmptyBytesReports() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ UUID1, /* payload= */ new byte[] {}, /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ UUID2, /* payload= */ new byte[] {}, /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
  }

  // TODO(b/260642993): Fix sharedInfo and encryption/decryption issues
  @Test
  public void localTestUsingAvroReportsWithOneKeyEmpty() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ UUID1, /* payload= */ new byte[] {}, /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ UUID2,
                /* payload= */ new byte[] {0x02, 0x03},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
  }

  // TODO(b/260642993): Fix sharedInfo and encryption/decryption issues
  @Test
  public void localTestUsingAvroReportsWithSpecialBytes() throws Exception {
    ImmutableList<AvroReportRecord> avroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ UUID1,
                /* payload= */ new byte[] {0x00, 0x00, 0x00, 127},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ UUID2,
                /* payload= */ new byte[] {-128, -128, -128, -128},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(avroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    assertThat(factList).isEmpty();
    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
  }

  @Test
  public void localTestConstantNoNoising_shardedReport_InvalidAPIType() throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));

    /*
     * Generate input for second shard with same input file lines but invalid API type in SharedInfo.
     * The reports in this shard will be ignored in aggregation because of invalid API type.
     */
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setApiType("invalid-api")
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));

    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 56, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 33, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 123, /* unnoisedMetric= */ 123L);
    /** Only reports from shard_1.avro are considered in aggregation. */
    assertThat(factList).containsExactly(expectedFact1, expectedFact2, expectedFact3);
  }

  @Test
  public void localTestConstantNoNoising_shardedReport_higherMajorVersion_jobFails()
      throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    // Generate input for second shard with same input file lines but higher major version in
    // SharedInfo.
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setVersion("2.0")
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(args);

    runWorker();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult.getReturnMessage()).contains("AggregationJobProcessException");
    assertThat(actualResult.getReturnMessage())
        .contains(
            "Current Aggregation Service deployment does not support Aggregatable reports with"
                + " shared_info.version");
    assertThat(actualResult.getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.UNSUPPORTED_REPORT_VERSION.name());
  }

  @Test
  public void localTestConstantNoNoising_shardedReport_higherMinorVersion_succeeds()
      throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setVersion("0.2")
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 56, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 33, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 123, /* unnoisedMetric= */ 123L);
    // Higher Minor version 0.2 reports are included in aggregation.
    assertThat(factList).containsExactly(expectedFact1, expectedFact2, expectedFact3);
  }

  @Test
  public void aggregate_withSourceRegistrationTimeValueZeroAndNegative_succeeds() throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setSourceRegistrationTime(Instant.EPOCH) // Equivalent to 0 in json
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setSourceRegistrationTime(Instant.EPOCH.minusSeconds(600))
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 112, /* unnoisedMetric= */ 112L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 66, /* unnoisedMetric= */ 66L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 246, /* unnoisedMetric= */ 246L);
    AggregatedFact expectedFact4 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(55), /* metric= */ 6, /* unnoisedMetric= */ 6L);
    assertThat(factList)
        .containsExactly(expectedFact1, expectedFact2, expectedFact3, expectedFact4);
  }

  @Test
  public void aggregate_withNoQueriedFilteringId_aggregatesDefaultOrIdContributions()
      throws Exception {
    // Input facts with every entry in set corresponding to a report and facts are separated by
    // comma.
    // All facts without ids, so considered for aggregation.
    ImmutableList<String> inputWithoutIds = ImmutableList.of("1:10,1:20,0:0", "5:50");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithoutIds)
            .setVersion(VERSION_0_1)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    // In this input with ids, only facts corresponding to id = 0 will be considered for
    // aggregation.
    ImmutableList<String> inputWithIds = ImmutableList.of("1:10:0,1:2:10,0:0:0", "5:50:4");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithIds)
            .setSourceRegistrationTime(Instant.EPOCH.minusSeconds(600))
            .setVersion(VERSION_1_0)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "10",
            /* enablePrivacyBudgetKeyFiltering= */ true,
            /* filteringIds= */ ""));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // Aggregates all the facts without id or has id = 0.
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(1), /* metric= */ 40, /* unnoisedMetric= */ 40L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(5), /* metric= */ 50, /* unnoisedMetric= */ 50L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2);
  }

  @Test
  public void aggregate_withQueriedFilteringId_aggregatesCorrespondingContributions()
      throws Exception {
    ImmutableList<String> inputWithoutIds = ImmutableList.of("1:10,1:20,0:0", "5:50");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithoutIds)
            .setVersion(VERSION_0_1)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    UnsignedLong queriedFilteringId1 =
        UnsignedLong.valueOf(
            BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE));
    UnsignedLong queriedFilteringId2 = UnsignedLong.valueOf((1L << 32) + 5);
    // In this input with ids, only facts corresponding to id = 18446744073709551615 and 4294967301
    // are considered for aggregation.
    ImmutableList<String> inputWithIds =
        ImmutableList.of(
            String.format("1:10:0,1:20:%s,0:0:0", queriedFilteringId1),
            String.format("5:51:%s", queriedFilteringId2));
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithIds)
            .setSourceRegistrationTime(Instant.EPOCH.minusSeconds(600))
            .setVersion(VERSION_1_0)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "10",
            /* enablePrivacyBudgetKeyFiltering= */ true,
            /* filteringIds= */ queriedFilteringId1.toString()
                + ","
                + queriedFilteringId2.toString()));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(1), /* metric= */ 20, /* unnoisedMetric= */ 20L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(5), /* metric= */ 51, /* unnoisedMetric= */ 51L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2);
  }

  @Test
  public void
      aggregate_withFilteringNotEnabled_ignoresQueriedIds_aggregatesDefaultOrIdContributions()
          throws Exception {
    // All facts without ids, so considered for aggregation.
    ImmutableList<String> inputWithoutIds = ImmutableList.of("1:10,1:20,0:0", "5:50");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithoutIds)
            .setVersion(VERSION_0_1)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    // In this input with ids, only facts corresponding to id = 0 will be considered for
    // aggregation.
    ImmutableList<String> inputWithIds = ImmutableList.of("1:10:0,1:20:10,0:0:0", "5:51:4");
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(inputWithIds)
            .setSourceRegistrationTime(Instant.EPOCH.minusSeconds(600))
            .setVersion(VERSION_1_0)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_2.avro"));
    setupLocalAggregationWorker(
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "10",
            /* enablePrivacyBudgetKeyFiltering= */ false,
            /* filteringIds= */ "4,10"));

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    // Aggregates all the facts without id or has id = 0.
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(1), /* metric= */ 40, /* unnoisedMetric= */ 40L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(5), /* metric= */ 50, /* unnoisedMetric= */ 50L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2);
  }

  @Test
  public void aggregate_withInvalidFilteringIds_throwsValidation() throws Exception {
    setupLocalAggregationWorker(
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "10",
            /* enablePrivacyBudgetKeyFiltering= */ false,
            /* filteringIds= */ "invalid,not a number,1,2,3"));

    runWorker();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();
    assertThat(actualResult.getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.INVALID_JOB.name());
    assertThat(actualResult.getReturnMessage())
        .containsMatch(
            "Job parameters for the job 'request' should have comma separated integers for"
                + " 'filtering_ids' parameter.");
  }

  @Test
  public void aggregate_withDecryptionErrors_withThreshold10Percent_failsEarly() throws Exception {
    // 8 reports with non-deserializable SharedInfo and empty payload.
    ImmutableList<AvroReportRecord> invalidAvroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-10c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-10c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-30c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-40c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-50c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-60c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-70c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-80c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(invalidAvroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    String[] args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "10");
    setupLocalAggregationWorker(args);

    runWorker();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(8L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(8L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job failed early because the number of reports excluded from"
                        + " aggregation exceeded threshold.")
                .setReturnCode(
                    AggregationWorkerReturnCode.REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD.name())
                .build());
  }

  @Test
  public void aggregate_withErrorsWithinThreshold_completesTheJob() throws Exception {
    // 4 reports with non-deserializable SharedInfo and empty payload.
    ImmutableList<AvroReportRecord> invalidAvroReportRecords =
        ImmutableList.of(
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-10c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-20c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-30c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"),
            createAvroReportRecord(
                /* keyId= */ "416abdc0-0697-4021-9300-40c1224ba204",
                /* payload= */ new byte[] {},
                /* sharedInfo= */ "sharedInfo"));
    generateCustomizedAvroReport(invalidAvroReportRecords);
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    // 7 valid reports
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));
    String[] args =
        getLocalAggregationWorkerArgs(
            /* noEncryption= */ false,
            /* outputDomain= */ false,
            /* domainOptional= */ true,
            /* reportErrorThresholdPercentage= */ "40.0");
    setupLocalAggregationWorker(args);

    runWorker();
    ImmutableList<AggregatedFact> factList = waitForAggregation();

    String actualResultSerialized = Files.readString(resultFile);
    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();
    assertThat(actualResult)
        .isEqualTo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .addAllErrorCounts(
                            ImmutableList.of(
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                    .setDescription(ErrorCounter.DECRYPTION_ERROR.getDescription())
                                    .setCount(4L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setDescription(
                                        ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                                    .setCount(4L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage(
                    "Aggregation job successfully processed but some reports have errors.")
                .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                .build());
    AggregatedFact expectedFact1 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(22), /* metric= */ 56, /* unnoisedMetric= */ 56L);
    AggregatedFact expectedFact2 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(33), /* metric= */ 33, /* unnoisedMetric= */ 33L);
    AggregatedFact expectedFact3 =
        AggregatedFact.create(
            /* key= */ createBucketFromInt(44), /* metric= */ 123, /* unnoisedMetric= */ 123L);
    assertThat(factList).containsExactly(expectedFact1, expectedFact2, expectedFact3);
  }

  private String[] getLocalAggregationWorkerArgs(
      boolean noEncryption, boolean outputDomain, boolean domainOptional) throws Exception {
    return getLocalAggregationWorkerArgs(
        noEncryption,
        outputDomain,
        domainOptional,
        "100",
        /** enablePrivacyBudgetKeyFiltering = */
        true,
        /** filteringIds = */
        null);
  }

  private String[] getLocalAggregationWorkerArgs(
      boolean noEncryption,
      boolean outputDomain,
      boolean domainOptional,
      String reportErrorThresholdPercentage)
      throws Exception {
    return getLocalAggregationWorkerArgs(
        noEncryption,
        outputDomain,
        domainOptional,
        reportErrorThresholdPercentage,
        /** enablePrivacyBudgetKeyFiltering = */
        true,
        /** filteringIds = */
        null);
  }

  private String[] getLocalAggregationWorkerArgs(
      boolean noEncryption,
      boolean outputDomain,
      boolean domainOptional,
      String reportErrorThresholdPercentage,
      boolean enablePrivacyBudgetKeyFiltering,
      String filteringIds)
      throws Exception {
    // Create the local key
    HybridConfig.register();
    ImmutableList.Builder<String> argsBuilder =
        ImmutableList.<String>builder()
            .add(
                "--local_file_decryption_key_path",
                hybridKey.toAbsolutePath().toString(),
                "--job_client",
                "LOCAL_FILE",
                "--blob_storage_client",
                "LOCAL_FS_CLIENT",
                "--result_working_directory_path",
                "/tmp/newton",
                "--local_file_single_puller_path",
                reportShardsDir.toAbsolutePath().toString(),
                "--local_file_job_info_path",
                resultFile.toAbsolutePath().toString(),
                "--local_output_domain_path",
                outputDomain ? domainShardsDir.toAbsolutePath().toString() : "",
                "--decryption_key_service",
                "LOCAL_FILE_DECRYPTION_KEY_SERVICE",
                "--record_reader",
                "LOCAL_NIO_AVRO",
                "--decryption",
                noEncryption ? "NOOP" : "HYBRID",
                "--result_logger",
                "IN_MEMORY",
                "--noising",
                "CONSTANT_NOISING",
                "--timer_exporter",
                "PLAIN_FILE",
                "--timer_exporter_file_path",
                stopwatchFile.toAbsolutePath().toString(),
                "--simulation_inputs",
                "--noising_epsilon",
                "64",
                "--noising_delta",
                "1e-4",
                "--noising_l1_sensitivity",
                "4",
                "--report_error_threshold_percentage",
                reportErrorThresholdPercentage);
    if (domainOptional) {
      argsBuilder.add("--domain_optional");
    }
    if (enablePrivacyBudgetKeyFiltering) {
      argsBuilder.add("--labeled_privacy_budget_keys_enabled");
    }
    if (!Strings.isNullOrEmpty(filteringIds)) {
      argsBuilder.add("--local_job_params_input_filtering_ids").add(filteringIds);
    }
    return argsBuilder.build().toArray(String[]::new);
  }

  private void setupLocalAggregationWorker(String[] args) {
    AggregationWorkerArgs cliArgs = new AggregationWorkerArgs();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);
    AggregationWorkerModule guiceModule = new AggregationWorkerModule(cliArgs);
    worker = AggregationWorker.fromModule(guiceModule);
    serviceManager = worker.createServiceManager();
  }

  private void generateCustomizedAvroReport(ImmutableList<AvroReportRecord> avroReportRecord)
      throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(reportsAvro, CREATE);
        AvroReportWriter reportWriter = writerFactory.create(outputAvroStream)) {
      reportWriter.writeRecords(metadata, avroReportRecord);
    }
  }

  private AvroReportRecord createAvroReportRecord(String keyId, byte[] payload, String sharedInfo) {
    return AvroReportRecord.create(ByteSource.wrap(payload), keyId, sharedInfo);
  }

  private void runWorker() throws Exception {
    serviceManager.startAsync();
    serviceManager.awaitStopped();
  }

  private ImmutableList<AggregatedFact> waitForAggregation() throws Exception {
    Injector injector = worker.getInjector();
    InMemoryResultLogger logger = injector.getInstance(InMemoryResultLogger.class);
    MaterializedAggregationResults results = null;
    boolean loggerTriggered = false;

    if (logger.hasLogged()) {
      loggerTriggered = true;
      results = logger.getMaterializedAggregationResults();
    }

    if (results == null) {
      // Worker hasn't completed after polling.
      if (loggerTriggered) {
        throw new Exception("MaterializedAggregationResults returns null.");
      }
      throw new TimeoutException("logResults is never called. Worker timed out.");
    }

    return results.getMaterializedAggregations();
  }

  private static final class TestEnv extends AbstractModule {}
}
