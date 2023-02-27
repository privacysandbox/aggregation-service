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

import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.StandardOpenOption.CREATE;

import com.beust.jcommander.JCommander;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.testing.InMemoryResultLogger;
import com.google.aggregate.adtech.worker.testing.MaterializedAggregationResults;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.aggregate.protocol.avro.AvroReportRecord;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ServiceManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.protobuf.util.JsonFormat;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.operator.protos.shared.backend.ReturnCodeProto.ReturnCode;
import com.google.scp.operator.shared.testing.AwsHermeticTestHelper;
import com.google.scp.operator.shared.testing.SimulationTestParams;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private ImmutableList<String> simulationInputFileLines;
  private ImmutableList<MetadataElement> metadata;

  private static final String UUID1 = "416abdc0-0697-4021-9300-10c1224ba204";
  private static final String UUID2 = "914a2679-bad6-46c0-bb8e-26b8c5757008";
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private AvroReportWriterFactory writerFactory;
  @Inject private AvroOutputDomainWriterFactory domainWriterFactory;

  @Before
  public void setUp() throws Exception {
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
  public void localTestNoNoising() throws Exception {
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
            .setSimulationInputFileLines(simulationInputFileLines)
            .setNoEncryption(true)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard.avro"));
    System.out.println(Arrays.toString(args));
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
    // The name of the job is empty on both sides of '|' because this is a fully local job. The
    // name 'simple' is coming form the simple aggregation processor. This is just a quick check to
    // ensure the stopwatches are exported correctly.
    assertThat(AwsHermeticTestHelper.stopwatchNamesRecorded(stopwatchFile))
        .containsExactly(
            "shard-decrypt-0", "concurrent-request", "shard-aggregation-0", "shard-read-0");
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
                                    .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(ReturnCode.SUCCESS.name())
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
                                    .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(ReturnCode.SUCCESS.name())
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
                .setReturnCode(ReturnCode.SUCCESS.name())
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
                                    .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(ReturnCode.SUCCESS.name())
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
                                    .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(ReturnCode.SUCCESS.name())
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
                                    .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
                                    .setCount(2L)
                                    .build(),
                                ErrorCount.newBuilder()
                                    .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                    .setCount(2L)
                                    .build()))
                        .build())
                .setFinishedAt(actualResult.getFinishedAt())
                .setReturnMessage("Aggregation job successfully processed")
                .setReturnCode(ReturnCode.SUCCESS.name())
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
  public void localTestConstantNoNoising_shardedReport_InvalidVersion() throws Exception {
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .build());
    Files.copy(reportsAvro, reportShardsDir.resolve("shard_1.avro"));

    /*
     * Generate input for second shard with same input file lines but invalid Version in SharedInfo.
     * The reports in this shard will be ignored in aggregation because of invalid Version.
     */
    AwsHermeticTestHelper.generateAvroReportsFromTextList(
        SimulationTestParams.builder()
            .setHybridKey(hybridKey)
            .setReportsAvro(reportsAvro)
            .setSimulationInputFileLines(simulationInputFileLines)
            .setVersion("invalid-version")
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

  private String[] getLocalAggregationWorkerArgs(
      boolean noEncryption, boolean outputDomain, boolean domainOptional) throws Exception {
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
                "4");
    if (domainOptional) {
      argsBuilder.add("--domain_optional");
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
