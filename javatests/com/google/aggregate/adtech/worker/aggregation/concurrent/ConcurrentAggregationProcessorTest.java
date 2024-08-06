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
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INTERNAL_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PERMISSION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHENTICATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_AUTHORIZATION_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.RESULT_WRITE_ERROR;
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.UNSUPPORTED_REPORT_VERSION;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_ATTRIBUTION_REPORT_TO;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_DEBUG_PRIVACY_EPSILON;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_DEBUG_RUN;
import static com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor.JOB_PARAM_REPORTING_SITE;
import static com.google.aggregate.adtech.worker.model.ErrorCounter.NUM_REPORTS_WITH_ERRORS;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_SUCCESS_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobResultHelper.RESULT_SUCCESS_WITH_ERRORS_MESSAGE;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_FILTERING_IDS;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_INPUT_REPORT_COUNT;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnablePrivacyBudgetKeyFiltering;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.ParallelAggregatedFactNoising;
import com.google.aggregate.adtech.worker.Annotations.ReportErrorThresholdPercentage;
import com.google.aggregate.adtech.worker.Annotations.StreamingOutputDomainProcessing;
import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.aggregation.domain.AvroOutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.TextOutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngineFactory;
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
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeValidator;
import com.google.aggregate.adtech.worker.testing.InMemoryResultLogger;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.util.ReportingOriginUtils;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.aggregate.adtech.worker.validation.ReportVersionValidator;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.StopwatchRegistry;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.budgeting.bridge.FakePrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetingServiceBridgeException;
import com.google.aggregate.privacy.budgeting.bridge.UnlimitedPrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.NoisedAggregationRunnerImpl;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.aggregate.protocol.avro.AvroOutputDomainReaderFactory;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriter;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.privacysandbox.otel.OtlpJsonLoggingOTelConfigurationModule;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.StatusCode;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.scp.operator.protos.shared.backend.JobKeyProto.JobKey;
import com.google.scp.operator.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.scp.shared.proto.ProtoUtil;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class ConcurrentAggregationProcessorTest {

  private static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00Z");
  private static final Instant REQUEST_RECEIVED_AT = Instant.parse("2019-10-01T08:25:24.00Z");
  private static final Instant REQUEST_PROCESSING_STARTED_AT =
      Instant.parse("2019-10-01T08:29:24.00Z");
  private static final Instant REQUEST_UPDATED_AT = Instant.parse("2019-10-01T08:29:24.00Z");

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
  @Inject AvroOutputDomainWriterFactory domainWriterFactory;
  @Inject OutputDomainProcessorHelper outputDomainProcessorHelper;
  @Inject private PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;
  @Inject private FeatureFlagHelper featureFlagHelper;
  private Path outputDomainDirectory;
  private Path reportsDirectory;
  private Path invalidReportsDirectory;
  private Job ctx;
  private Job ctxInvalidReport;
  private JobResult expectedJobResult;
  private ResultInfo.Builder resultInfoBuilder;
  private ImmutableList<EncryptedReport> encryptedReports1;
  private ImmutableList<EncryptedReport> encryptedReports2;
  private final String reportId1 = String.valueOf(UUID.randomUUID());
  private final String reportId2 = String.valueOf(UUID.randomUUID());
  private final String reportId3 = String.valueOf(UUID.randomUUID());
  private final String reportId4 = String.valueOf(UUID.randomUUID());
  private final String reportId5 = String.valueOf(UUID.randomUUID());

  // Run all tests with streamingOutputDomain enabled and disabled.
  @TestParameter boolean streamingOutputDomainTestParam;

  // Under test.
  @Inject private Provider<ConcurrentAggregationProcessor> processor;

  @Before
  public void setUpFlags() {
    outputDomainProcessorHelper.setAvroOutputDomainProcessor(true);
    outputDomainProcessorHelper.setDomainOptional(true);
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
    fakeValidator.setReportIdShouldReturnError(ImmutableSet.of());

    outputDomainProcessorHelper.setStreamingOutputDomainProcessing(streamingOutputDomainTestParam);
  }

  private EncryptedReport generateEncryptedReportWithVersion(
      int param, String reportId, String version) {
    Report report =
        FakeReportGenerator.generateWithFixedReportId(param, reportId, /* reportVersion */ version);
    return getEncryptedReport(report);
  }

  private EncryptedReport getEncryptedReport(Report unencryptedreport) {
    try {
      String sharedInfoString =
          sharedInfoSerdes.reverse().convert(Optional.of(unencryptedreport.sharedInfo()));
      String keyId = UUID.randomUUID().toString();
      ByteSource firstReportBytes =
          fakeDecryptionKeyService.generateCiphertext(
              keyId,
              payloadSerdes.reverse().convert(Optional.of(unencryptedreport.payload())),
              sharedInfoString);
      return EncryptedReport.builder()
          .setPayload(firstReportBytes)
          .setKeyId(keyId)
          .setSharedInfo(sharedInfoString)
          .build();
    } catch (Exception ex) {
      // return null to fail test
      return null;
    }
  }

  private EncryptedReport generateEncryptedReport(int param, String reportId) {
    return generateEncryptedReportWithVersion(param, reportId, LATEST_VERSION);
  }

  private EncryptedReport generateInvalidVersionEncryptedReport(
      int param, String reportId, String invalidVersion) {
    return generateEncryptedReportWithVersion(param, reportId, invalidVersion);
  }

  @Before
  public void setUpInputData() throws Exception {
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        new UnlimitedPrivacyBudgetingServiceBridge());

    outputDomainDirectory = testWorkingDir.getRoot().toPath().resolve("output_domain");
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");
    invalidReportsDirectory = testWorkingDir.getRoot().toPath().resolve("invalid-reports");

    Files.createDirectory(outputDomainDirectory);
    Files.createDirectory(reportsDirectory);
    Files.createDirectory(invalidReportsDirectory);

    ctx = generateJob("foo", Optional.of("https://example.foo.com"), Optional.empty());
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                getRequestInfoWithInputDataBucketName(ctx.requestInfo(), reportsDirectory))
            .build();
    resultInfoBuilder =
        ResultInfo.newBuilder()
            .setReturnCode(AggregationWorkerReturnCode.SUCCESS.name())
            .setReturnMessage(RESULT_SUCCESS_MESSAGE)
            .setFinishedAt(ProtoUtil.toProtoTimestamp(FIXED_TIME))
            .setErrorSummary(ErrorSummary.getDefaultInstance());
    expectedJobResult = makeExpectedJobResult();

    // Job context for job with invalid version input report.
    ctxInvalidReport = generateJob("bar", Optional.of("https://example.foo.com"), Optional.empty());
    ctxInvalidReport =
        ctxInvalidReport.toBuilder()
            .setRequestInfo(
                getRequestInfoWithInputDataBucketName(
                    ctxInvalidReport.requestInfo(), invalidReportsDirectory))
            .build();

    EncryptedReport firstReport = generateEncryptedReport(1, reportId1);
    EncryptedReport secondReport = generateEncryptedReport(2, reportId2);

    // thirdReport is same as firstReport but has new report id
    EncryptedReport thirdReport = generateEncryptedReport(1, reportId3);
    // fourthReport is same as secondReport but has new report id
    EncryptedReport fourthReport = generateEncryptedReport(2, reportId4);

    encryptedReports1 = ImmutableList.of(firstReport, secondReport);
    encryptedReports2 = ImmutableList.of(thirdReport, fourthReport);
    // 2 shards of same contents.
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports2);

    EncryptedReport invalidEncryptedReport =
        generateInvalidVersionEncryptedReport(1, reportId5, "55.0");
    writeReports(
        invalidReportsDirectory.resolve("invalid_reports.avro"),
        ImmutableList.of(invalidEncryptedReport));
  }

  @Test
  public void aggregate_domainOptionalAndNoOutputDomain() throws Exception {
    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 2, 2L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 8, 8L));
  }

  @Test
  public void aggregate_skipZeroSizedBlobs() throws Exception {
    // Write an empty report.
    writeReports(reportsDirectory.resolve("reports_6.avro"), ImmutableList.of());
    // Followed by two additional non-empty reports.
    EncryptedReport testReport1 = generateEncryptedReport(3, String.valueOf(UUID.randomUUID()));
    EncryptedReport testReport2 = generateEncryptedReport(4, String.valueOf(UUID.randomUUID()));
    writeReports(
        reportsDirectory.resolve("reports_7.avro"), ImmutableList.of(testReport1, testReport2));
    JobResult jobResultProcessor = processor.get().process(ctx);

    // Check if empty report is skipped and output is processed without errors.
    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 2, 2L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 8, 8L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(3), /* metric= */ 9, 9L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(4), /* metric= */ 16, 16L));
  }

  @Test
  public void aggregate_invalidVersionReport() {
    AggregationJobProcessException ex =
        assertThrows(
            AggregationJobProcessException.class, () -> processor.get().process(ctxInvalidReport));
    assertThat(ex.getCode()).isEqualTo(UNSUPPORTED_REPORT_VERSION);
    assertThat(ex.getMessage())
        .contains(
            "Current Aggregation Service deployment does not support Aggregatable reports with"
                + " shared_info.version");
  }

  @Test
  public void aggregate_noOutputDomain_thresholding() throws Exception {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_reportingSiteProvided() throws Exception {
    ctx = generateJob("foo", Optional.empty(), Optional.of("https://foo.com"));
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                getRequestInfoWithInputDataBucketName(ctx.requestInfo(), reportsDirectory))
            .build();
    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 2, 2L),
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 8, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_overlappingDomainKeysInResults() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(false);

    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "1");
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_2.avro"), "2");

    ctx = addOutputDomainToJob();
    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    // Key 3 added as an extra key from the output domain.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 2, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L));
  }

  @Test
  public void aggregate_withOutputDomain_noOverlappingDomainKeys() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(false);

    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "3");
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_2.avro"), "4");

    ctx = addOutputDomainToJob();
    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    // Key 3 added as an extra key from the output domain.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ 0, /* unnoisedMetric= */ 0L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(4), /* metric= */ 0, /* unnoisedMetric= */ 0L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(true);
    // Key 1 is not in output domain, so thresholding applies.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2");
    ctx = addOutputDomainToJob();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilon() throws Exception {
    // Key 1 is not in output domain, so thresholding applies.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2");
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilonMalformedValue()
      throws Exception {
    // Key 1 is not in output domain, so thresholding applies.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2");
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(2), /* metric= */ 5, 8L));
  }

  @Test
  public void aggregate_withOutputDomain_thresholding_debugEpsilonOutOfRange() throws Exception {
    writeOutputDomainAvroFile(
        outputDomainDirectory.resolve("output_domain_1.avro"),
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(INVALID_JOB);
  }

  @Test
  public void aggregate_noOutputDomain_thresholding_withoutDebugRun() throws Exception {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);
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
  public void aggregate_debugRunDomainOptional_resultsInSameDebugFacts() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(true);
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextValueNoiseToAdd(List.of(1L, 2L, 3L, 4L, 5L, 6L).iterator())
            .build());

    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2", "3");

    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_RUN,
            "true",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    Map<BigInteger, AggregatedFact> resultFacts =
        resultLogger.getMaterializedAggregationResults().getMaterializedAggregations().stream()
            .collect(Collectors.toMap(AggregatedFact::getBucket, Function.identity()));
    assertThat(resultFacts).hasSize(3);

    assertThat(resultLogger.getMaterializedDebugAggregationResults().getMaterializedAggregations())
        .hasSize(3);
    Map<BigInteger, AggregatedFact> debugFacts =
        resultLogger.getMaterializedDebugAggregationResults().getMaterializedAggregations().stream()
            .collect(Collectors.toMap(AggregatedFact::getBucket, Function.identity()));
    assertThat(debugFacts).hasSize(3);

    // Key 1 is in the report only, key 2 overlaps both sets, and key 3 is in the domain only.
    compareDebugFactByKey(
        resultFacts, debugFacts, createBucketFromInt(1), List.of(DebugBucketAnnotation.IN_REPORTS));
    compareDebugFactByKey(
        resultFacts,
        debugFacts,
        createBucketFromInt(2),
        List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN));
    compareDebugFactByKey(
        resultFacts, debugFacts, createBucketFromInt(3), List.of(DebugBucketAnnotation.IN_DOMAIN));
  }

  @Test
  public void aggregate_debugRunWithOutputDomain_resultsInSameDebugFacts() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(false);
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextValueNoiseToAdd(List.of(1L, 2L, 3L, 4L, 5L, 6L).iterator())
            .build());

    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2", "3");

    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_DEBUG_RUN,
            "true",
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());

    Map<BigInteger, AggregatedFact> resultFacts =
        resultLogger.getMaterializedAggregationResults().getMaterializedAggregations().stream()
            .collect(Collectors.toMap(AggregatedFact::getBucket, Function.identity()));
    assertThat(resultFacts).hasSize(2);

    Map<BigInteger, AggregatedFact> debugFacts =
        resultLogger.getMaterializedDebugAggregationResults().getMaterializedAggregations().stream()
            .collect(Collectors.toMap(AggregatedFact::getBucket, Function.identity()));
    assertThat(debugFacts).hasSize(3);

    // Key 2 is in both domain and reports; key 3 is in the domain only.
    compareDebugFactByKey(
        resultFacts,
        debugFacts,
        createBucketFromInt(2),
        List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN));
    compareDebugFactByKey(
        resultFacts, debugFacts, createBucketFromInt(3), List.of(DebugBucketAnnotation.IN_DOMAIN));

    // Key 1 is in the report only but should be present in the debug facts.
    assertThat(resultFacts).doesNotContainKey(createBucketFromInt(1));
    assertThat(debugFacts).containsKey(createBucketFromInt(1));
    assertThat(debugFacts.get(createBucketFromInt(1)).getDebugAnnotations()).isPresent();
    assertThat(debugFacts.get(createBucketFromInt(1)).getDebugAnnotations().get())
        .containsExactly(DebugBucketAnnotation.IN_REPORTS);
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    // Confirm correct success code is returned, and not an alternate debug mode success code
    assertThat(jobResultProcessor.resultInfo().getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
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
    // Key 1 is not in output domain, so thresholding applies.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "2", "3");
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

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
    // Key 1 is in output domain, so no thresholding applies.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "1");
    ctx = addOutputDomainToJob();
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(-3));

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(makeExpectedJobResult());
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ -1, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 5, /* unnoisedMetric= */ 8L));
  }

  @Test
  public void aggregation_withOutputDomainAndDomainRequired_addKeys() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(false);

    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "3");
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_2.avro"), "1", "2");

    ctx = addOutputDomainToJob();
    JobResult jobResultProcessor = processor.get().process(ctx);

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
  public void aggregate_withOutputDomainAndDomainOptional_addKeys() throws Exception {
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "3");
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_2.avro"), "1", "2");

    ctx = addOutputDomainToJob();
    JobResult jobResultProcessor = processor.get().process(ctx);

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
  public void aggregate_withOutputDomain_addKeysWithDuplicates() throws Exception {
    // 3 is duplicate in the output domain.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "3");
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_2.avro"), "2", "3");
    ctx = addOutputDomainToJob();
    JobResult jobResultProcessor = processor.get().process(ctx);

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
  public void aggregate_nonExistentOutputDomainLocationSpecified_throwsException() {
    // Domain is optional but output domain location is specified.
    // Skip the output domain generation.
    ctx = addOutputDomainToJob();

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading domain input data.");
  }

  @Test
  public void aggregate_emptyOutputDomainFiles_throwsException() throws Exception {
    // Write an empty output domain file.
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"));
    ctx = addOutputDomainToJob();

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading domain input data.");
    assertThat(ex.getCause().getMessage()).contains("No output domain provided in the location");
  }

  @Test
  public void aggregate_withOutputDomain_avroDomainNotReadable() throws Exception {
    // Skip the output domain generation, specifying an empty file.
    Path badDataShard = outputDomainDirectory.resolve("domain_bad.avro");
    writeOutputDomainTextFile(badDataShard, "bad shard");
    ctx = addOutputDomainToJob();

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading domain input data.");
  }

  @Test
  public void aggregate_withOutputDomain_textDomainNotReadable() throws Exception {
    outputDomainProcessorHelper.setAvroOutputDomainProcessor(false);
    Path badDataShard = outputDomainDirectory.resolve("domain_bad.txt");
    writeOutputDomainTextFile(badDataShard, "abcdabcdabcdabcdabcdabcdabcdabcd");
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading domain input data.");
  }

  @Test
  public void aggregate_withNoise() throws Exception {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    JobResult jobResultProcessor = processor.get().process(ctx);

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
    fakeValidator.setReportIdShouldReturnError(ImmutableSet.of(reportId1));
    // Since 1st report has validation errors, only facts in 2nd report are noised.
    JobResult jobResultProcessor = processor.get().process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                    .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                        .setDescription(
                                            ErrorCounter.DECRYPTION_ERROR.getDescription())
                                        .setCount(1L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(
                                            ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
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
    fakeValidator.setReportIdShouldReturnError(
        ImmutableSet.of(reportId1, reportId2, reportId3, reportId4));

    JobResult jobResultProcessor = processor.get().process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                    .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                        .setDescription(
                                            ErrorCounter.DECRYPTION_ERROR.getDescription())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(
                                            ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
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

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading reports input data.");
  }

  @Test
  public void process_withEmptyReportsAndDomainOptional_returnsSuccess() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(true);
    Path pathToEmptyReports = testWorkingDir.getRoot().toPath().resolve("empty_reports_dir");
    Files.createDirectory(pathToEmptyReports);
    writeReports(pathToEmptyReports.resolve("reports_1.avro"), ImmutableList.of());

    Job emptyReportsCtx =
        ctx.toBuilder()
            .setRequestInfo(
                getRequestInfoWithInputDataBucketName(ctx.requestInfo(), pathToEmptyReports))
            .build();

    JobResult result = processor.get().process(emptyReportsCtx);
    assertThat(result.resultInfo().getReturnCode()).contains("SUCCESS");
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .isEmpty();
  }

  @Test
  public void process_withEmptyReportsWithDomain_returnsNoisedDomain() throws Exception {
    outputDomainProcessorHelper.setDomainOptional(false);
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(10));

    Path dirToEmptyReports = testWorkingDir.getRoot().toPath().resolve("empty_reports_dir");
    Files.createDirectory(dirToEmptyReports);

    writeReports(dirToEmptyReports.resolve("reports_1.avro"), ImmutableList.of());
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "3");

    ctx = addOutputDomainToJob();
    Job emptyReportsCtx =
        ctx.toBuilder()
            .setRequestInfo(
                getRequestInfoWithInputDataBucketName(ctx.requestInfo(), dirToEmptyReports))
            .build();

    JobResult result = processor.get().process(emptyReportsCtx);
    assertThat(result.resultInfo().getReturnCode()).contains("SUCCESS");

    // No reports and one key specified in the domain so a single aggregated fact is expected.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ 10, /* unnoisedMetric= */ 0L));
  }

  @Test
  public void process_outputWriteFailedCodeWhenResultLoggerThrows() {
    resultLogger.setShouldThrow(true);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(RESULT_WRITE_ERROR);
  }

  @Test
  public void process_decryptionKeyFetchFailedWithPermissionDeniedReason() {
    fakeDecryptionKeyService.setShouldThrow(true, ErrorReason.PERMISSION_DENIED);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(PERMISSION_ERROR);
  }

  @Test
  public void process_decryptionKeyFetchServiceUnavailable_throwsInternal() {
    fakeDecryptionKeyService.setShouldThrow(true, ErrorReason.KEY_SERVICE_UNAVAILABLE);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));

    assertThat(ex.getCode()).isEqualTo(INTERNAL_ERROR);
  }

  @Test
  public void process_decryptionKeyFetchFailedOtherReasons() throws Exception {
    fakeDecryptionKeyService.setShouldThrow(true);

    JobResult actualJobResult = processor.get().process(ctx);

    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                    .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.INTERNAL_ERROR.name())
                                        .setDescription(ErrorCounter.INTERNAL_ERROR.getDescription())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(NUM_REPORTS_WITH_ERRORS.getDescription())
                                        .setCount(4L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
  }

  @Test
  public void process_errorCountExceedsThreshold_quitsEarly() throws Exception {
    ImmutableList<EncryptedReport> encryptedReports1 =
        ImmutableList.of(
            generateEncryptedReport(1, reportId1),
            generateEncryptedReport(2, reportId2),
            generateEncryptedReport(3, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(4, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(5, reportId3));
    ImmutableList<EncryptedReport> encryptedReports2 =
        ImmutableList.of(
            generateEncryptedReport(6, reportId4),
            generateEncryptedReport(7, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(8, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(9, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(10, String.valueOf(UUID.randomUUID())));
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports2);
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setReportIdShouldReturnError(
        ImmutableSet.of(reportId1, reportId2, reportId3, reportId4));
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, "20");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult actualJobResult = processor.get().process(ctx);

    // Job quits on error count 4 > threshold 2 (20% threshold of 10 reports)
    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(
                        AggregationWorkerReturnCode.REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD.name())
                    .setReturnMessage(RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                        .setDescription(
                                            ErrorCounter.DECRYPTION_ERROR.getDescription())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(NUM_REPORTS_WITH_ERRORS.getDescription())
                                        .setCount(4L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isEmpty();
    assertFalse(resultLogger.hasLogged());
  }

  @Test
  public void process_withInputReportCountInRequest_errorCountExceedsThreshold_quitsEarly()
      throws Exception {
    ImmutableList<EncryptedReport> encryptedReports1 =
        ImmutableList.of(
            generateEncryptedReport(1, reportId1),
            generateEncryptedReport(2, reportId2),
            generateEncryptedReport(3, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(4, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(5, reportId3));
    ImmutableList<EncryptedReport> encryptedReports2 =
        ImmutableList.of(
            generateEncryptedReport(6, reportId4),
            generateEncryptedReport(7, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(8, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(9, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(10, String.valueOf(UUID.randomUUID())));
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports2);
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setReportIdShouldReturnError(
        ImmutableSet.of(reportId1, reportId2, reportId3, reportId4));
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, "20", JOB_PARAM_INPUT_REPORT_COUNT, "10");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult actualJobResult = processor.get().process(ctx);

    // Job quits on error count 4 > threshold 2 (20% threshold of 10 reports)
    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(
                        AggregationWorkerReturnCode.REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD.name())
                    .setReturnMessage(RESULT_REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                        .setDescription(
                                            ErrorCounter.DECRYPTION_ERROR.getDescription())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(NUM_REPORTS_WITH_ERRORS.getDescription())
                                        .setCount(4L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isEmpty();
    assertFalse(resultLogger.hasLogged());
  }

  @Test
  public void process_errorCountWithinThreshold_succeedsWithErrors() throws Exception {
    ImmutableList<EncryptedReport> encryptedReports1 =
        ImmutableList.of(
            generateEncryptedReport(1, reportId1),
            generateEncryptedReport(2, reportId2),
            generateEncryptedReport(3, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(4, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(5, reportId3));
    ImmutableList<EncryptedReport> encryptedReports2 =
        ImmutableList.of(
            generateEncryptedReport(6, reportId4),
            generateEncryptedReport(7, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(8, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(9, String.valueOf(UUID.randomUUID())),
            generateEncryptedReport(10, String.valueOf(UUID.randomUUID())));
    writeReports(reportsDirectory.resolve("reports_1.avro"), encryptedReports1);
    writeReports(reportsDirectory.resolve("reports_2.avro"), encryptedReports2);

    fakeValidator.setReportIdShouldReturnError(
        ImmutableSet.of(reportId1, reportId2, reportId3, reportId4));
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_REPORT_ERROR_THRESHOLD_PERCENTAGE, "50.0", JOB_PARAM_INPUT_REPORT_COUNT, "");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult actualJobResult = processor.get().process(ctx);

    // Job succeeds because error count 4 < threshold 5
    JobResult expectedJobResult =
        this.expectedJobResult.toBuilder()
            .setResultInfo(
                resultInfoBuilder
                    .setReturnCode(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name())
                    .setReturnMessage(RESULT_SUCCESS_WITH_ERRORS_MESSAGE)
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .addAllErrorCounts(
                                ImmutableList.of(
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.DECRYPTION_ERROR.name())
                                        .setDescription(
                                            ErrorCounter.DECRYPTION_ERROR.getDescription())
                                        .setCount(4L)
                                        .build(),
                                    ErrorCount.newBuilder()
                                        .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                                        .setDescription(NUM_REPORTS_WITH_ERRORS.getDescription())
                                        .setCount(4L)
                                        .build()))
                            .build())
                    .build())
            .build();
    assertThat(actualJobResult).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(3), /* metric= */ 9, /* unnoisedMetric= */ 9L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(4), /* metric= */ 16, /* unnoisedMetric= */ 16L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(7), /* metric= */ 49, /* unnoisedMetric= */ 49L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(8), /* metric= */ 64, /* unnoisedMetric= */ 64L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(9), /* metric= */ 81, /* unnoisedMetric= */ 81L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(10),
                /* metric= */ 100,
                /* unnoisedMetric= */ 100L));
  }

  @Test
  public void process_withNoQueriedFilteringId_filteringNotEnabled_queries0OrNullIds()
      throws Exception {
    featureFlagHelper.setEnablePrivacyBudgetKeyFiltering(false);
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    EncryptedReport reportWithoutId =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithoutId, nullFact1), VERSION_0_1));

    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("11111"))
            .setValue(11)
            .setId(UnsignedLong.ZERO)
            .build();
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("33333"))
            .setValue(33)
            .setId(UnsignedLong.valueOf(12))
            .build();
    Fact nullFact2 =
        Fact.builder().setBucket(new BigInteger("0")).setValue(0).setId(UnsignedLong.ZERO).build();
    EncryptedReport reportWithIds =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithDefaultId, factWithId, nullFact2), VERSION_1_0));

    writeReports(reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    writeReports(reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    processor.get().process(ctx);

    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ new BigInteger("11111"),
                /* metric= */ 22,
                /* unnoisedMetric= */ 22L));
  }

  @Test
  public void process_withQueriedFilteringId_filteringNotEnabled_queries0OrNullIds()
      throws Exception {
    featureFlagHelper.setEnablePrivacyBudgetKeyFiltering(false);
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    EncryptedReport reportWithoutId =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithoutId, nullFact1), VERSION_0_1));

    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("11111"))
            .setValue(11)
            .setId(UnsignedLong.ZERO)
            .build();
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("33333"))
            .setValue(33)
            .setId(UnsignedLong.valueOf(12))
            .build();
    Fact nullFact2 =
        Fact.builder().setBucket(new BigInteger("0")).setValue(0).setId(UnsignedLong.ZERO).build();
    EncryptedReport reportWithIds =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithDefaultId, factWithId, nullFact2), "1.0"));

    writeReports(reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    writeReports(reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    ImmutableMap<String, String> jobParams = ImmutableMap.of(JOB_PARAM_FILTERING_IDS, "12");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    processor.get().process(ctx);

    // Even though the job queries for the id = 12, the aggregation is done for id = 0 or null since
    // the feature flag is not enabled.
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ new BigInteger("11111"),
                /* metric= */ 22,
                /* unnoisedMetric= */ 22L));
  }

  @Test
  public void process_withQueriedFilteringId_filteringEnabled_filtersForTheGivenIds()
      throws Exception {
    featureFlagHelper.setEnablePrivacyBudgetKeyFiltering(true);
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    EncryptedReport reportWithoutId =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithoutId, nullFact1), VERSION_0_1));
    // Aggregation is done only for contributions corresponding to ids = 12, 13.
    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("11111"))
            .setValue(11)
            .setId(UnsignedLong.ZERO)
            .build();
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("33333"))
            .setValue(33)
            .setId(UnsignedLong.valueOf(12))
            .build();
    Fact nullFact2 =
        Fact.builder().setBucket(new BigInteger("0")).setValue(0).setId(UnsignedLong.ZERO).build();
    EncryptedReport reportWithIds =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithDefaultId, factWithId, nullFact2), "1.0"));

    writeReports(reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    writeReports(reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));
    // Privacy budget is consumed for 13 as well even though there are no contributions with this
    // id.
    ImmutableSet<PrivacyBudgetUnit> expectedPrivacyBudgetUnits =
        ImmutableSet.of(
            getPrivacyBudgetUnit(reportWithoutId, /* filteringIds= */ UnsignedLong.valueOf(12)),
            getPrivacyBudgetUnit(reportWithIds, /* filteringIds= */ UnsignedLong.valueOf(12)),
            getPrivacyBudgetUnit(reportWithoutId, /* filteringIds= */ UnsignedLong.valueOf(13)),
            getPrivacyBudgetUnit(reportWithIds, /* filteringIds= */ UnsignedLong.valueOf(13)));
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    expectedPrivacyBudgetUnits.forEach(
        pbu -> fakePrivacyBudgetingServiceBridge.setPrivacyBudget(pbu, /* budget= */ 1));
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    ImmutableMap<String, String> jobParams = ImmutableMap.of(JOB_PARAM_FILTERING_IDS, "12,13");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();
    processor.get().process(ctx);

    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ new BigInteger("33333"),
                /* metric= */ 33,
                /* unnoisedMetric= */ 33L));
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isPresent();
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent().get())
        .containsExactlyElementsIn(expectedPrivacyBudgetUnits);
  }

  @Test
  public void process_withConsecutiveJobsAndSameFilteringIds_throwsPrivacyExhausted()
      throws Exception {
    featureFlagHelper.setEnablePrivacyBudgetKeyFiltering(true);
    Fact factWithoutId1 = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    EncryptedReport reportWithoutId =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithoutId1), VERSION_0_1));

    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("22222"))
            .setValue(11)
            .setId(UnsignedLong.valueOf(12))
            .build();
    EncryptedReport reportWithIds =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(ImmutableList.of(factWithId), "1.0"));
    writeReports(reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    writeReports(reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    UnsignedLong filteringIdJob = UnsignedLong.valueOf(1963698);

    ImmutableSet<PrivacyBudgetUnit> expectedPrivacyBudgetUnitsJobs =
        ImmutableSet.of(
            getPrivacyBudgetUnit(reportWithoutId, filteringIdJob),
            getPrivacyBudgetUnit(reportWithIds, filteringIdJob));
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    expectedPrivacyBudgetUnitsJobs.stream()
        .forEach(pbu -> fakePrivacyBudgetingServiceBridge.setPrivacyBudget(pbu, /* budget= */ 1));
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    Job job =
        getJobWithGivenJobParams(
            /* jobParams= */ ImmutableMap.of(JOB_PARAM_FILTERING_IDS, filteringIdJob.toString()));
    processor.get().process(job);
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isPresent();
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent().get())
        .containsExactlyElementsIn(expectedPrivacyBudgetUnitsJobs);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(job));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_EXHAUSTED);
  }

  @Test
  public void process_withConsecutiveJobsAndDifferentFilteringIds_budgetingSucceeds()
      throws Exception {
    featureFlagHelper.setEnablePrivacyBudgetKeyFiltering(true);
    Fact factWithoutId1 = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact factWithoutId2 = Fact.builder().setBucket(new BigInteger("11111")).setValue(22).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    EncryptedReport reportWithoutId =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithoutId1, factWithoutId2, nullFact1), VERSION_0_1));

    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("22222"))
            .setValue(11)
            .setId(UnsignedLong.ZERO)
            .build();
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("22222"))
            .setValue(11)
            .setId(UnsignedLong.valueOf(12))
            .build();
    EncryptedReport reportWithIds =
        getEncryptedReport(
            FakeReportGenerator.generateWithFactList(
                ImmutableList.of(factWithDefaultId, factWithId), "1.0"));
    writeReports(reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    writeReports(reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    UnsignedLong filteringIdJob1 = UnsignedLong.ZERO;
    UnsignedLong filteringIdJob2 = UnsignedLong.valueOf(12);

    ImmutableSet<PrivacyBudgetUnit> expectedPrivacyBudgetUnitsJob1 =
        ImmutableSet.of(
            getPrivacyBudgetUnit(reportWithoutId, filteringIdJob1),
            getPrivacyBudgetUnit(reportWithIds, filteringIdJob1));
    ImmutableSet<PrivacyBudgetUnit> expectedPrivacyBudgetUnitsJob2 =
        ImmutableSet.of(
            getPrivacyBudgetUnit(reportWithoutId, filteringIdJob2),
            getPrivacyBudgetUnit(reportWithIds, filteringIdJob2));
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    Stream.concat(expectedPrivacyBudgetUnitsJob1.stream(), expectedPrivacyBudgetUnitsJob2.stream())
        .forEach(pbu -> fakePrivacyBudgetingServiceBridge.setPrivacyBudget(pbu, /* budget= */ 1));
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    Job job1 =
        getJobWithGivenJobParams(
            /* jobParams= */ ImmutableMap.of(JOB_PARAM_FILTERING_IDS, filteringIdJob1.toString()));
    processor.get().process(job1);
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isPresent();
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent().get())
        .containsExactlyElementsIn(expectedPrivacyBudgetUnitsJob1);

    Job job2 =
        getJobWithGivenJobParams(
            /* jobParams= */ ImmutableMap.of(JOB_PARAM_FILTERING_IDS, filteringIdJob2.toString()));
    processor.get().process(job2);
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent()).isPresent();
    assertThat(fakePrivacyBudgetingServiceBridge.getLastBudgetsToConsumeSent().get())
        .containsExactlyElementsIn(expectedPrivacyBudgetUnitsJob2);
  }

  @Test
  public void processingWithWrongSharedInfo() throws Exception {
    String keyId = UUID.randomUUID().toString();
    Report report =
        FakeReportGenerator.generateWithParam(
            1, /* reportVersion */ LATEST_VERSION, "https://example.foo.com");
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
    writeOutputDomainAvroFile(outputDomainDirectory.resolve("output_domain_1.avro"), "1");
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
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(/* bucket= */ createBucketFromInt(1), /* metric= */ 0, 0L));
  }

  @Test
  public void aggregate_withPrivacyBudgeting_noBudget() {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    // No budget given, i.e. all the budgets are depleted for this test.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_EXHAUSTED);
  }

  @Test
  public void aggregate_withPrivacyBudgeting() throws Exception {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    PrivacyBudgetUnit privacyBudgetUnit1 = getPrivacyBudgetUnit(encryptedReports1.get(0));
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(privacyBudgetUnit1, 1);
    PrivacyBudgetUnit privacyBudgetUnit2 = getPrivacyBudgetUnit(encryptedReports1.get(1));
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(privacyBudgetUnit2, 1);
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    JobResult jobResultProcessor = processor.get().process(ctx);

    assertThat(jobResultProcessor).isEqualTo(expectedJobResult);
    assertThat(resultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(1), /* metric= */ 2, /* unnoisedMetric= */ 2L),
            AggregatedFact.create(
                /* bucket= */ createBucketFromInt(2), /* metric= */ 8, /* unnoisedMetric= */ 8L));
    // Check that the right attributionReportTo and debugPrivacyBudgetLimit were sent to the bridge
    String claimedIdentity =
        ReportingOriginUtils.convertReportingOriginToSite(
            ctx.requestInfo().getJobParametersMap().get(JOB_PARAM_ATTRIBUTION_REPORT_TO));
    assertThat(fakePrivacyBudgetingServiceBridge.getLastAttributionReportToSent())
        .hasValue(claimedIdentity);
  }

  @Test
  public void aggregate_withPrivacyBudgeting_unauthenticatedException_failJob() {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();

    fakePrivacyBudgetingServiceBridge.setException(
        new PrivacyBudgetingServiceBridgeException(
            StatusCode.PRIVACY_BUDGET_CLIENT_UNAUTHENTICATED, new IllegalStateException("fake")));
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_AUTHENTICATION_ERROR);
  }

  @Test
  public void aggregate_withPrivacyBudgeting_unauthorizedException_failJob() {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();

    fakePrivacyBudgetingServiceBridge.setException(
        new PrivacyBudgetingServiceBridgeException(
            StatusCode.PRIVACY_BUDGET_CLIENT_UNAUTHORIZED, new IllegalStateException("fake")));
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_AUTHORIZATION_ERROR);
  }

  @Test
  public void aggregate_withPrivacyBudgeting_invalidReportingOriginException_failJob() {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();

    Map<String, String> jobParameters = new HashMap<>(ctx.requestInfo().getJobParametersMap());
    jobParameters.put(JOB_PARAM_ATTRIBUTION_REPORT_TO, "https://subdomain.coordinator.test");
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParameters))
                    .build())
            .build();
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> processor.get().process(ctx));
    assertThat(ex.getMessage())
        .isEqualTo(
            "Invalid reporting origin found while consuming budget, this should not happen as job"
                + " validations ensure the reporting origin is always valid.");
  }

  @Test
  public void aggregate_withPrivacyBudgeting_oneBudgetMissing() {
    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("1", Instant.ofEpochMilli(0), "https://example.foo.com"), 1);
    // Missing budget for the second report.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);

    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(PRIVACY_BUDGET_EXHAUSTED);
  }

  /**
   * Test that the worker fails the job if an exception occurs when the reports bucket is
   * nonexistent.
   */
  @Test
  public void aggregate_withNonExistentBucket() {
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
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("Exception while reading reports input data.");
  }

  /**
   * Test that the worker fails the job if an exception occurs when the report file path is
   * nonexistent.
   */
  @Test
  public void aggregate_withNonExistentReportFile() {
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
                    .setInputDataBlobPrefix("nonExistentReport.avro")
                    .build())
            .build();
    AggregationJobProcessException ex =
        assertThrows(AggregationJobProcessException.class, () -> processor.get().process(ctx));
    assertThat(ex.getCode()).isEqualTo(INPUT_DATA_READ_FAILED);
    assertThat(ex.getMessage()).contains("No report shards found for location");
  }

  @Test
  public void aggregate_withDebugRunAndPrivacyBudgetFailure_succeedsWithErrorCode()
      throws Exception {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JOB_PARAM_DEBUG_RUN, "true");
    // Set debug run params in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    // Privacy Budget failure via thrown exception
    fakePrivacyBudgetingServiceBridge.setShouldThrow();
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("1", Instant.ofEpochMilli(0), "https://example.foo.com"), 1);
    // Missing budget for the second report.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());

    JobResult result = processor.get().process(ctx);

    // Return code should be SUCCESS, return message should match the would-be error
    assertThat(result.resultInfo().getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_ERROR.name());
  }

  /** Test that worker completes with success if debug run despite Privacy Budget exhausted */
  @Test
  public void aggregateDebug_withPrivacyBudgetExhausted() throws Exception {
    ImmutableMap<String, String> jobParams = ImmutableMap.of(JOB_PARAM_DEBUG_RUN, "true");
    // Set debug run params in job.
    ctx =
        ctx.toBuilder()
            .setRequestInfo(
                ctx.requestInfo().toBuilder()
                    .putAllJobParameters(
                        combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                    .build())
            .build();

    FakePrivacyBudgetingServiceBridge fakePrivacyBudgetingServiceBridge =
        new FakePrivacyBudgetingServiceBridge();
    fakePrivacyBudgetingServiceBridge.setPrivacyBudget(
        PrivacyBudgetUnit.create("1", Instant.ofEpochMilli(0), "https://example.foo.com"), 1);
    // Missing budget for the second report.
    privacyBudgetingServiceBridge.setPrivacyBudgetingServiceBridgeImpl(
        fakePrivacyBudgetingServiceBridge);
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, false, false, false).iterator());

    JobResult result = processor.get().process(ctx);

    // Return code should be SUCCESS, return message should match the would-be error
    assertThat(result.resultInfo().getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
  }

  private RequestInfo getRequestInfoWithInputDataBucketName(
      RequestInfo requestInfo, Path inputReportDirectory) {
    Map<String, String> jobParameters = new HashMap<>(requestInfo.getJobParametersMap());
    jobParameters.put("report_error_threshold_percentage", "100");
    return requestInfo.toBuilder()
        .putAllJobParameters(jobParameters)
        // Simulating shards of input.
        .setInputDataBucketName(inputReportDirectory.toAbsolutePath().toString())
        .setInputDataBlobPrefix("")
        .build();
  }

  private void compareDebugFactByKey(
      Map<BigInteger, AggregatedFact> resultFacts,
      Map<BigInteger, AggregatedFact> debugFacts,
      BigInteger key,
      List<DebugBucketAnnotation> expectedAnnotation) {
    assertThat(resultFacts).containsKey(key);
    assertThat(debugFacts).containsKey(key);
    compareDebugFact(resultFacts.get(key), debugFacts.get(key));
    assertThat(debugFacts.get(key).getDebugAnnotations()).isPresent();
    assertThat(debugFacts.get(key).getDebugAnnotations().get())
        .containsExactlyElementsIn(expectedAnnotation);
  }

  private void compareDebugFact(AggregatedFact resultFact, AggregatedFact debugFact) {
    assertEquals(resultFact.getBucket(), debugFact.getBucket());
    assertEquals(resultFact.getMetric(), debugFact.getMetric());

    assertThat(resultFact.getUnnoisedMetric()).isPresent();
    assertThat(debugFact.getUnnoisedMetric()).isPresent();
    assertEquals(resultFact.getUnnoisedMetric().get(), debugFact.getUnnoisedMetric().get());
  }

  private void writeOutputDomainTextFile(Path outputDomainPath, String... keys) throws IOException {
    Files.write(outputDomainPath, ImmutableList.copyOf(keys), US_ASCII, WRITE, CREATE);
  }

  private void writeOutputDomainAvroFile(Path domainFile, String... keys) throws IOException {
    try (OutputStream outputAvroStream = Files.newOutputStream(domainFile, CREATE);
        AvroOutputDomainWriter outputDomainWriter = domainWriterFactory.create(outputAvroStream)) {
      ImmutableList<AvroOutputDomainRecord> domain =
          Arrays.stream(keys)
              .map(NumericConversions::createBucketFromString)
              .map(AvroOutputDomainRecord::create)
              .collect(toImmutableList());
      outputDomainWriter.writeRecords(ImmutableList.of(), domain);
    }
  }

  private JobResult makeExpectedJobResult() {
    // Can't use FakeJobResultGenerator since values are different
    return JobResult.builder()
        .setJobKey(ctx.jobKey())
        .setResultInfo(resultInfoBuilder.build())
        .build();
  }

  private PrivacyBudgetUnit getPrivacyBudgetUnit(EncryptedReport encryptedReport) {
    return getPrivacyBudgetUnit(
        encryptedReport,
        /** filteringId = */
        UnsignedLong.ZERO);
  }

  private PrivacyBudgetUnit getPrivacyBudgetUnit(
      EncryptedReport encryptedReport, UnsignedLong filteringId) {
    SharedInfo sharedInfo = sharedInfoSerdes.convert(encryptedReport.sharedInfo()).get();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setFilteringId(filteringId)
            .setSharedInfo(sharedInfo)
            .build();
    PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput).get();
    PrivacyBudgetUnit privacyBudgetUnit =
        PrivacyBudgetUnit.create(
            privacyBudgetKeyGenerator.generatePrivacyBudgetKey(
                PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
                    .setFilteringId(filteringId)
                    .setSharedInfo(sharedInfo)
                    .build()),
            Instant.ofEpochMilli(0),
            sharedInfo.reportingOrigin());
    return privacyBudgetUnit;
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

  private Job addOutputDomainToJob() {
    DataLocation outputDomainLocation = getOutputDomainLocation();
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME,
            outputDomainLocation.blobStoreDataLocation().bucket(),
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX,
            outputDomainLocation.blobStoreDataLocation().key());
    return ctx.toBuilder()
        .setRequestInfo(
            ctx.requestInfo().toBuilder()
                .putAllJobParameters(
                    combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                .build())
        .build();
  }

  private Job getJobWithGivenJobParams(ImmutableMap<String, String> jobParams) {
    return ctx.toBuilder()
        .setRequestInfo(
            ctx.requestInfo().toBuilder()
                .putAllJobParameters(
                    combineJobParams(ctx.requestInfo().getJobParametersMap(), jobParams))
                .build())
        .build();
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

  private static class OutputDomainProcessorHelper {

    boolean isAvroOutputDomainProcessor = true;
    boolean streamingOutputDomainProcessing = false;
    boolean domainOptional = true;

    public boolean isDomainOptional() {
      return domainOptional;
    }

    public void setDomainOptional(boolean domainOptional) {
      this.domainOptional = domainOptional;
    }

    public boolean isStreamingOutputDomainProcessing() {
      return streamingOutputDomainProcessing;
    }

    public void setStreamingOutputDomainProcessing(boolean streamingProcessing) {
      this.streamingOutputDomainProcessing = streamingProcessing;
    }

    void setAvroOutputDomainProcessor(Boolean flag) {
      isAvroOutputDomainProcessor = flag;
    }

    boolean isAvroOutputDomainProcessor() {
      return isAvroOutputDomainProcessor;
    }
  }

  private static class FeatureFlagHelper {

    boolean enablePrivacyBudgetKeyFiltering = true;

    void setEnablePrivacyBudgetKeyFiltering(boolean enablePrivacyBudgetKeyFiltering) {
      this.enablePrivacyBudgetKeyFiltering = enablePrivacyBudgetKeyFiltering;
    }
  }

  public static Job generateJob(
      String id, Optional<String> attributionReportTo, Optional<String> reportingSite) {
    if (attributionReportTo.isEmpty() && reportingSite.isEmpty()) {
      throw new RuntimeException(
          "At least one of attributionReportTo and reportingSite should be provided");
    }
    RequestInfo.Builder requestInfoBuilder =
        RequestInfo.newBuilder()
            .setJobRequestId(id)
            .setInputDataBlobPrefix("dataHandle")
            .setInputDataBucketName("bucket")
            .setOutputDataBlobPrefix("dataHandle")
            .setOutputDataBucketName("bucket")
            .setPostbackUrl("http://postback.com");
    RequestInfo requestInfo;
    if (attributionReportTo.isPresent()) {
      requestInfo =
          requestInfoBuilder
              .putAllJobParameters(
                  ImmutableMap.of(JOB_PARAM_ATTRIBUTION_REPORT_TO, attributionReportTo.get()))
              .build();
    } else {
      requestInfo =
          requestInfoBuilder
              .putAllJobParameters(ImmutableMap.of(JOB_PARAM_REPORTING_SITE, reportingSite.get()))
              .build();
    }
    return Job.builder()
        .setJobKey(JobKey.newBuilder().setJobRequestId(id).build())
        .setJobProcessingTimeout(Duration.ofSeconds(3600))
        .setRequestInfo(requestInfo)
        .setCreateTime(REQUEST_RECEIVED_AT)
        .setUpdateTime(REQUEST_UPDATED_AT)
        .setProcessingStartTime(Optional.of(REQUEST_PROCESSING_STARTED_AT))
        .setJobStatus(JobStatus.IN_PROGRESS)
        .setNumAttempts(0)
        .build();
  }

  private static final class TestEnv extends AbstractModule {

    OutputDomainProcessorHelper helper = new OutputDomainProcessorHelper();

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
      reportValidatorMultibinder.addBinding().to(ReportVersionValidator.class);

      // noising
      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(NoisedAggregationRunner.class).to(NoisedAggregationRunnerImpl.class);
      bind(boolean.class).annotatedWith(ParallelAggregatedFactNoising.class).toInstance(true);

      // loggers.
      bind(InMemoryResultLogger.class).in(TestScoped.class);
      bind(ResultLogger.class).to(InMemoryResultLogger.class);

      // Stopwatches
      bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);

      // Privacy budgeting
      bind(ProxyPrivacyBudgetingServiceBridge.class).in(TestScoped.class);
      bind(PrivacyBudgetingServiceBridge.class).to(ProxyPrivacyBudgetingServiceBridge.class);
      install(new PrivacyBudgetKeyGeneratorModule());

      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);

      // Otel collector
      install(new OtlpJsonLoggingOTelConfigurationModule());
      bind(Boolean.class).annotatedWith(EnableStackTraceInResponse.class).toInstance(true);
      bind(Integer.class).annotatedWith(MaxDepthOfStackTrace.class).toInstance(3);
      bind(double.class).annotatedWith(ReportErrorThresholdPercentage.class).toInstance(10.0);
      bind(OutputDomainProcessorHelper.class).toInstance(helper);

      bind(FeatureFlagHelper.class).toInstance(new FeatureFlagHelper());
    }

    @Provides
    @EnableThresholding
    boolean providesEnableThresholding() {
      return helper.isDomainOptional();
    }

    @Provides
    @StreamingOutputDomainProcessing
    boolean providesStreamingOutputDomainProcessing() {
      return helper.isStreamingOutputDomainProcessing();
    }

    @Provides
    @DomainOptional
    boolean providesDomainOptional() {
      return helper.isDomainOptional();
    }

    @Provides
    OutputDomainProcessor provideDomainProcess(
        @BlockingThreadPool ListeningExecutorService blockingThreadPool,
        @NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
        BlobStorageClient blobStorageClient,
        StopwatchRegistry stopwatchRegistry,
        AvroOutputDomainReaderFactory avroOutputDomainReaderFactory,
        @EnableThresholding Boolean enableThresholding,
        @DomainOptional Boolean domainOptional) {
      return helper.isAvroOutputDomainProcessor()
          ? new AvroOutputDomainProcessor(
          blockingThreadPool,
          nonBlockingThreadPool,
          blobStorageClient,
          avroOutputDomainReaderFactory,
          stopwatchRegistry,
          domainOptional,
          enableThresholding)
          : new TextOutputDomainProcessor(
              blockingThreadPool,
              nonBlockingThreadPool,
              blobStorageClient,
              stopwatchRegistry,
              domainOptional,
              enableThresholding);
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
    @Singleton
    @NonBlockingThreadPool
    ListeningExecutorService provideNonBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @Singleton
    @BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @Singleton
    @CustomForkJoinThreadPool
    ListeningExecutorService provideCustomForkJoinThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    AggregationEngine provideAggregationEngine(AggregationEngineFactory aggregationEngineFactory) {
      return aggregationEngineFactory.create();
    }

    @Provides
    @EnablePrivacyBudgetKeyFiltering
    Boolean provideEnableBudgetKeyFiltering(FeatureFlagHelper featureFlagHelper) {
      return featureFlagHelper.enablePrivacyBudgetKeyFiltering;
    }
  }
}
