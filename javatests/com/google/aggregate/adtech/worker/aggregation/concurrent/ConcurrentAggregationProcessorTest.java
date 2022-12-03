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

package com.google.aggregate.adtech.worker.aggregation.concurrent;

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INPUT_DATA_READ_FAILED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PERMISSION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.RESULT_LOGGING_ERROR;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_ATTRIBUTION_REPORT_TO;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_DEBUG_PRIVACY_EPSILON;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_DEBUG_RUN;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.RESULT_SUCCESS_MESSAGE;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.NUM_REPORTS_WITH_ERRORS;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.scp.operator.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory.GENERAL_ERROR;
import static com.google.scp.operator.protos.shared.backend.ReturnCodeProto.ReturnCode.SUCCESS;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.FailJobOnPbsException;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.TextOutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.privacy.FakePrivacyBudgetingServiceBridge;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.adtech.worker.aggregation.privacy.UnlimitedPrivacyBudgetingServiceBridge;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionModule;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeValidator;
import com.google.aggregate.adtech.worker.testing.InMemoryResultLogger;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.NoisedAggregationRunnerImpl;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConcurrentAggregationProcessorTest {

  private static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00Z");

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  @Inject AvroReportWriterFactory reportWriterFactory;
  @Inject AvroRecordEncryptedReportConverter avroConverter;
  // These are the same reader and decrypter that are inside the processor, through Guice bindings.
  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject FakeValidator fakeValidator;
  @Inject InMemoryResultLogger resultLogger;
  @Inject FakeDecryptionKeyService fakeDecryptionKeyService;
  @Inject PayloadSerdes payloadSerdes;
  @Inject ProxyPrivacyBudgetingServiceBridge privacyBudgetingServiceBridge;
  @Inject SharedInfoSerdes sharedInfoSerdes;
  private Path outputDomainDirectory;
  private Path reportsDirectory;
  private Job ctx;
  private JobResult expectedJobResult;
  private ResultInfo.Builder resultInfoBuilder;
  private ImmutableList<EncryptedReport> encryptedReports1;
  private ImmutableList<EncryptedReport> encryptedReports2;

  // Settable value so that failJobOnPbsExceptionProvider can be changed for different tests
  private static boolean failJobOnPbsException = false;

  // Under test
  @Inject private ConcurrentAggregationProcessor processor;

  private static void assertJobResultsEqualsIgnoreReturnMessage(
      JobResult actual, JobResult expected) {
    JobResult actualNoReturnMessage =
        actual.toBuilder()
            .setResultInfo(actual.resultInfo().toBuilder().setReturnMessage("").build())
            .build();
    JobResult expectedNoReturnMessage =
        expected.toBuilder()
            .setResultInfo(expected.resultInfo().toBuilder().setReturnMessage("").build())
            .build();

    assertThat(actualNoReturnMessage).isEqualTo(expectedNoReturnMessage);
  }

  private static void assertJobResultsEqualsReturnCode(JobResult actual, JobResult expected) {
    assertThat(actual.resultInfo().getReturnCode())
        .isEqualTo(expected.resultInfo().getReturnCode());
  }

  private EncryptedReport generateEncryptedReport(int param) {
    String keyId = UUID.randomUUID().toString();
    Report report = FakeReportGenerator.generateWithParam(param, /* reportVersion */ "");
    String sharedInfoString1 = sharedInfoSerdes.reverse().convert(Optional.of(report.sharedInfo()));
    try {
      ByteSource firstReportBytes =
          fakeDecryptionKeyService.generateCiphertext(
              keyId,
              payloadSerdes.reverse().convert(Optional.of(report.payload())),
              sharedInfoString1);
      return EncryptedReport.builder()
          .setPayload(firstReportBytes)
          .setKeyId(keyId)
          .setSharedInfo(sharedInfoString1)
          .build();
    } catch (Exception ex) {
      // return null to fail test
      return null;
    }
  }

  @Before
  public void setUp() throws Exception {
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        new UnlimitedPrivacyBudgetingServiceBridge());

    outputDomainDirectory = testWorkingDir.getRoot().toPath().resolve("output_domain");
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");

    Files.createDirectory(outputDomainDirectory);
    Files.createDirectory(reportsDirectory);

    ctx = FakeJobGenerator.generateBuilder("foo").build();

    // Add the debugPrivacyBudgetLimit to the job (stored RequestInfo)
    RequestInfo requestInfo = ctx.requestInfo();
    RequestInfo newRequestInfo =
        requestInfo.toBuilder()
            .putAllJobParameters(requestInfo.getJobParameters())
            // Simulating 2 shards of input.
            .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
            .setInputDataBlobPrefix("")
            .build();

    ctx = ctx.toBuilder().setRequestInfo(newRequestInfo).build();

    resultInfoBuilder =
        ResultInfo.newBuilder()
            .setReturnCode(SUCCESS.name())
            .setReturnMessage(RESULT_SUCCESS_MESSAGE)
            .setFinishedAt(ProtoUtil.toProtoTimestamp(FIXED_TIME))
            .setErrorSummary(ErrorSummary.getDefaultInstance());

    expectedJobResult = makeExpectedJobResult();

    EncryptedReport firstReport = generateEncryptedReport(1);
    EncryptedReport secondReport = generateEncryptedReport(2);

    // thirdReport is same as firstReport but has new report id
    EncryptedReport thirdReport = generateEncryptedReport(1);
    // fourthReport is same as secondReport but has new report id
    EncryptedReport fourthReport = generateEncryptedReport(2);

    encryptedReports1 = ImmutableList.of(firstReport, secondReport);
    encryptedReports2 = ImmutableList.of(thirdReport, fourthReport);
    // 2 shards of same contents.
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports2);
  }

  @Test
  public void aggregate() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 2, 2L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 8, 8L));
  }

  @Test
  public void aggregate_noOutputDomain_thresholding() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding() throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "2"); // 1 is not in output domain, so thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilon() throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "2"); // 1 is not in output domain, so thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_PRIVACY_EPSILON,
            "0.5",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    // Set output domain location and debug_privacy_epsilon in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();

    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilonMalformedValue()
      throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "2"); // 1 is not in output domain, so thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();

    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_PRIVACY_EPSILON,
            "",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    // Set output domain location and debug_privacy_epsilon in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();

    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilonOutOfRange() throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "2"); // 1 is not in output domain, so thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_PRIVACY_EPSILON,
            "0",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    // Set output domain location and debug_privacy_epsilon in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();

    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(INVALID_JOB);
  }

  @Test
  public void aggregate_noOutputDomain_thresholding_withoutDebugRun() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);
    ResultLogException exception =
        assertThrows(
            ResultLogException.class, () -> resultLogger.getMaterializedDebugAggregationResults());

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 5, /* unnoisedMetric= */ 8L));
    assertThat(exception).hasCauseThat().isInstanceOf(IllegalStateException.class);
    assertThat(exception)
        .hasMessageThat()
        .contains("MaterializedAggregations is null. Maybe results did not get logged.");
  }

  @Test
  public void aggregate_noOutputDomain_thresholding_withDebugRun() throws Exception {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JOB_PARAM_DEBUG_RUN, "true");
    // Set debug run params in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 5, /* unnoisedMetric= */ 8L));
    assertThat(resultLogger.getMaterializedDebugAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1),
                /* metric= */ -1,
                /* unnoisedMetric= */ 2L,
                /* debugAnnotations= */ List.of(DebugBucketAnnotation.IN_REPORTS)),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2),
                /* metric= */ 5,
                /* unnoisedMetric= */ 8L,
                /* debugAnnotations= */ List.of(DebugBucketAnnotation.IN_REPORTS)));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_withDebugRun() throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "2",
        "3"); // 1 is not in output domain, so thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_RUN,
            "true",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    // Set output domain location and debug run in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();

    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 5, /* unnoisedMetric= */ 8L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ -3, /* unnoisedMetric= */ 0L));
    assertThat(resultLogger.getMaterializedDebugAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1),
                /* metric= */ -1,
                /* unnoisedMetric= */ 2L,
                /* debugAnnotations= */ List.of(DebugBucketAnnotation.IN_REPORTS)),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2),
                /* metric= */ 5,
                /* unnoisedMetric= */ 8L,
                /* debugAnnotations= */ List.of(
                    DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN)),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3),
                /* metric= */ -3,
                /* unnoisedMetric= */ 0L,
                /* debugAnnotations= */ List.of(DebugBucketAnnotation.IN_DOMAIN)));
  }

  @Test
  public void aggregate_withOutputDomain_noThresholding() throws Exception {
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_1.txt"),
        "1"); // 1 is in output domain, so no thresholding applies
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ -1, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 5, /* unnoisedMetric= */ 8L));
  }

  @Test
  public void aggregate_withOutputDomain_addKeys() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("output_domain_1.txt"), "3");
    writeOutputDomain(outputDomainDirectory.resolve("output_domain_2.txt"), "1", "2");
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    // Key 3 added as an extra key from the output domain.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 2, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ 0, /* unnoisedMetric= */ 0L));
  }

  @Test
  public void aggregate_withOutputDomain_addKeysAndExtra() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("output_domain_1.txt"), "3");
    writeOutputDomain(
        outputDomainDirectory.resolve("output_domain_2.txt"),
        "2",
        "3"); // 3 is intentionally duplicate.
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    // Key 3 added as an extra key from the output domain.
    // Key is filtered out because it is not in the output domain.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 2, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ 0, /* unnoisedMetric= */ 0L));
  }

  @Test
  public void aggregate_withOutputDomain_domainNotReadable() throws Exception {
    // Intentionally skipping the output domain generation here.
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading domain input data.");
  }

  @Test
  public void aggregate_withNoise() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 12, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 18, /* unnoisedMetric= */ 8L));
  }

  @Test
  public void process_withValidationErrors() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(true, false, false, false).iterator());
    // Since 1st report has validation errors, only facts in 2nd report are noised.
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(GENERAL_ERROR.name())
                                        .setCount(1L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(NUM_REPORTS_WITH_ERRORS.name())
                                        .setCount(1L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 1, /* unnoisedMetric= */ 1L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L));
  }

  @Test
  public void process_withValidationErrors_allReportsFail() throws Exception {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    // Throw validation errors for all reports
    fakeValidator.setNextShouldReturnError(ImmutableList.of(true, true, true, true).iterator());

    JobResult jobResultProcessor = processor.process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(GENERAL_ERROR.name())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(NUM_REPORTS_WITH_ERRORS.name())
                                        .setCount(4L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .isEmpty();
    // Check that no calls were made to PBS
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isEmpty();
    assertThat(fakePrivacyBudgetingServiceBridge.getLastAttributionReportToSent()).isEmpty();
  }

  @Test
  public void process_inputReadFailedCodeWhenBadShardThrows() throws Exception {
    Path badDataShard = reportsDirectory.resolve("reports_bad.avro");
    Files.writeString(badDataShard, "Bad data", US_ASCII, WRITE, CREATE);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading reports input data.");
  }

  @Test
  public void process_outputWriteFailedCodeWhenResultLoggerThrows() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    resultLogger.setShouldThrow(true);
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(RESULT_LOGGING_ERROR);
  }

  @Test
  public void process_decryptionKeyFetchFailedWithPermissionDeniedReason() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    fakeDecryptionKeyService.setShouldThrowPermissionException(true);
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(PERMISSION_ERROR);
  }

  @Test
  public void process_decryptionKeyFetchFailedOtherReasons() throws Exception {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    fakeDecryptionKeyService.setShouldThrow(true);

    JobResult actualJobResult = processor.process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder.setReturnCode(SUCCESS.name()).setReturnMessage("").build())
            .build();
    assertJobResultsEqualsReturnCode(actualJobResult, expectedJobResult);
  }

  @Test
  public void processingWithWrongSharedInfo() throws Exception {
    String keyId = UUID.randomUUID().toString();
    Report report = FakeReportGenerator.generateWithParam(1, /* reportVersion */ "");
    // Encrypt with a different sharedInfo than what is provided with the report so that decryption
    // fails
    String sharedInfoForEncryption = "foobarbaz";
    String sharedInfoWithReport =
        sharedInfoSerdes.reverse().convert(Optional.of(report.sharedInfo()));
    ByteSource reportBytes =
        fakeDecryptionKeyService.generateCiphertext(
            keyId,
            payloadSerdes.reverse().convert(Optional.of(report.payload())),
            sharedInfoForEncryption);
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(reportBytes)
            .setKeyId(keyId)
            .setSharedInfo(sharedInfoWithReport)
            .build();
    encryptedReports1 = ImmutableList.of(encryptedReport, encryptedReport);
    // 2 shards of same contents.
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports1);
    writeOutputDomain(outputDomainDirectory.resolve("output_domain_1.txt"), "1");
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());

    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParameters(), jobParams))
                    .build())
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 0, 0L));
  }

  @Test
  public void aggregate_withPrivacyBudgeting_noBudget() throws Exception {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    // No budget given, i.e. all the budgets are depleted for this test.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_EXHAUSTED);
  }

  @Test
  public void aggregate_withPrivacyBudgeting() throws Exception {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("1", Instant.ofEpochMilli(0)), 1);
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("2", Instant.ofEpochMilli(0)), 1);
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));

    JobResult jobResultProcessor = processor.process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 2, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L));
    // Check that the right attributionReportTo and debugPrivacyBudgetLimit were sent to the bridge
    assertThat(fakePrivacyBudgetingServiceBridge.getLastAttributionReportToSent())
        .hasValue(ctx.requestInfo().getJobParameters().get(JOB_PARAM_ATTRIBUTION_REPORT_TO));
  }

  /**
   * Test that the worker fails the job if an exception occurs when consuming privacy budget and the
   * worker is configured to fail on PBS exceptions.
   */
  @Test
  public void aggregate_withPrivacyBudgeting_exception_failJobOnPbsException() throws Exception {
    failJobOnPbsException = true; // Configure worker to fail if a PBS exception occurs
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    fakePrivacyBudgetingServiceBridge.setShouldThrow();
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_ERROR);
  }

  @Test
  public void aggregate_withPrivacyBudgeting_oneBudgetMissing() throws Exception {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("1", Instant.ofEpochMilli(0)), 1);
    // Missing budget for the second report.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_EXHAUSTED);
  }

  /**
   * Test that the worker fails the job if an exception occurs when the reports bucket is
   * nonexistent.
   */
  @Test
  public void aggregate_withNonExistentBucket() throws Exception {
    Path nonExistentReportsDirectory =
        testWorkingDir.getRoot().toPath().resolve("nonExistentBucket");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .setInputDataBucketName(nonExistentReportsDirectory.toAbsolutePath().toString())
                    .setInputDataBlobPrefix("")
                    .build())
            .build();
    // TODO(b/258078789): Passing nonexistent reports folder should throw
    // TODO(b/258082317): Add assertion on return message.
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
  }

  /**
   * Test that the worker fails the job if an exception occurs when the report file path is
   * nonexistent.
   */
  @Test
  public void aggregate_withNonExistentReportFile() throws Exception {
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
                    .setInputDataBlobPrefix("nonExistentReport.avro")
                    .build())
            .build();
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("No report shards found for location");
  }

  private void writeOutputDomain(Path outputDomainPath, String... keys) throws IOException {
    Files.write(outputDomainPath, ImmutableList.copyOf(keys), US_ASCII, WRITE, CREATE);
  }

  private JobResult makeExpectedJobResult() {
    // Can't use FakeJobResultGenerator since values are different
    return JobResult.builder()
        .setJobKey(ctx.jobKey())
        .setResultInfo(resultInfoBuilder.build())
        .build();
  }

  private void writeReports(Path reportsPath, ImmutableList<EncryptedReport> encryptedReports)
      throws IOException {
    try (OutputStream avroStream = Files.newOutputStream(reportsPath, CREATE, TRUNCATE_EXISTING);
        AvroReportWriter reportWriter = reportWriterFactory.create(avroStream)) {
      reportWriter.writeRecords(
          /* metadata= */ ImmutableList.of(),
          encryptedReports.stream().map(avroConverter.reverse()).collect(toImmutableList()));
    }
  }

  private DataLocation getOutputDomainLocation() {
    return DataLocation.ofBlobStoreDataLocation(
        BlobStoreDataLocation.create(
            /* bucket= */ outputDomainDirectory.toAbsolutePath().toString(), /* key= */ ""));
  }

  /**
   * Proxy implementation for the privacy budgeting service that passes the call to the wrapped
   * budgeting bridge: this enables the testing to dynamically swap out implementations instead of
   * just statically assembling the implementation with Acai.
   */
  private static class ProxyPrivacyBudgetingServiceBridge implements PrivacyBudgetingServiceBridge {

    private PrivacyBudgetingServiceBridge wrappedImpl;

    private void setPrivacyBudgetingServiceBridgeImpl(PrivacyBudgetingServiceBridge impl) {
      wrappedImpl = impl;
    }

    @Override
    public ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
        ImmutableList<PrivacyBudgetUnit> budgetsToConsume, String attributionReportTo)
        throws PrivacyBudgetingServiceBridgeException {
      return wrappedImpl.consumePrivacyBudget(budgetsToConsume, attributionReportTo);
    }
  }

  private ImmutableMap<String, String> combineJobParams(
      Map<String, String> currentJobParams, Map<String, String> additionalJobParams) {
    Map<String, String> map = Maps.newHashMap();
    map.putAll(currentJobParams);
    map.putAll(additionalJobParams);
    return ImmutableMap.copyOf(map);
  }

  // TODO: these setup steps could be consolidated with the SimpleAggregationProcessorTest TestEnv.
  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(ObjectMapper.class).to(TimeObjectMapper.class);

      // Report reading
      install(new FSBlobStorageClientModule());
      bind(FileSystem.class).toInstance(FileSystems.getDefault());

      // decryption
      bind(FakeDecryptionKeyService.class).in(TestScoped.class);
      bind(DecryptionKeyService.class).to(FakeDecryptionKeyService.class);
      install(new HybridDecryptionModule());
      bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class);
      bind(PayloadSerdes.class).to(CborPayloadSerdes.class);

      // report validation.
      bind(FakeValidator.class).in(TestScoped.class);
      Multibinder<ReportValidator> reportValidatorMultibinder =
          Multibinder.newSetBinder(binder(), ReportValidator.class);
      reportValidatorMultibinder.addBinding().to(FakeValidator.class);

      // noising
      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(NoisedAggregationRunner.class).to(NoisedAggregationRunnerImpl.class);

      // loggers.
      bind(InMemoryResultLogger.class).in(TestScoped.class);
      bind(ResultLogger.class).to(InMemoryResultLogger.class);

      // Stopwatches
      bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);

      // Privacy budgeting
      bind(ProxyPrivacyBudgetingServiceBridge.class).in(TestScoped.class);
      bind(PrivacyBudgetingServiceBridge.class).to(ProxyPrivacyBudgetingServiceBridge.class);

      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
      // TODO(b/227210339) Add a test with false value for domainOptional
      bind(Boolean.class).annotatedWith(DomainOptional.class).toInstance(true);
      bind(OutputDomainProcessor.class).to(TextOutputDomainProcessor.class);
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }

    @Provides
    Supplier<PrivacyParameters> providePrivacyParamConfig(PrivacyParametersSupplier supplier) {
      return () -> supplier.get().toBuilder().setDelta(1e-5).build();
    }

    @Provides
    @Threshold
    Supplier<Double> provideThreshold() {
      return () -> 0.0;
    }

    @Provides
    Clock provideClock() {
      return Clock.fixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Provides
    Ticker provideTimingTicker() {
      return Ticker.systemTicker();
    }

    @Provides
    @NonBlockingThreadPool
    ListeningExecutorService provideNonBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    AggregationEngine provideAggregationEngine() {
      return AggregationEngine.create();
    }

    @Provides
    @FailJobOnPbsException
    Boolean prodvideFailJobOnPbsException() {
      return failJobOnPbsException;
    }
  }
}
