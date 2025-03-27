/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.aggregation.concurrent;

import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngineFactory;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionModule;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeReportWriter;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.aggregate.adtech.worker.validation.ReportVersionValidator;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.privacysandbox.otel.OtlpJsonLoggingOTelConfigurationModule;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.protos.shared.backend.ErrorCountProto;
import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportAggregatorTest {

  private static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00Z");

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();

  @Inject private FakeReportWriter fakeReportWriter;
  @Inject private AggregationEngineFactory aggregationEngineFactory;
  @Inject private ReportAggregator reportAggregator;
  private Path reportsDirectory;

  @Before
  public void before() throws Exception {
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");
    Files.createDirectory(reportsDirectory);
  }

  @Test
  public void process_withQueriedZeroFilteringId_queriesZeroOrNullIds() throws Exception {
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    Report reportWithoutId =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutId, nullFact1), VERSION_0_1);

    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("11111"))
            .setValue(11)
            .setId(UnsignedLong.ZERO)
            .build();
    // The below fact should be excluded from aggregation as it corresponds to non-queried filtering
    // id.
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("33333"))
            .setValue(33)
            .setId(UnsignedLong.valueOf(12))
            .build();
    Fact nullFact2 =
        Fact.builder().setBucket(new BigInteger("0")).setValue(0).setId(UnsignedLong.ZERO).build();
    Report reportWithIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithDefaultId, factWithId, nullFact2), VERSION_1_0);

    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    AtomicLong reportCounter = new AtomicLong();
    AggregationEngine keyAggregationEngine =
        aggregationEngineFactory.createKeyAggregationEngine(ImmutableSet.of(UnsignedLong.ZERO));
    ErrorSummaryAggregator errorSummaryAggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(
            /* totalReportCount= */ Optional.empty(), /* errorPercentageThreshold= */ 10);
    Job job = createJob();
    reportAggregator.processReports(
        reportCounter, job, keyAggregationEngine, errorSummaryAggregator);

    // Only the bucket 11111 corresponds to queried filtering ids in the reports and is included in
    // the aggregation.
    AggregatedFact expectedFact =
        AggregatedFact.create(/* bucket= */ new BigInteger("11111"), /* metric= */ 22);

    assertThat(keyAggregationEngine.makeAggregation().values()).containsExactly(expectedFact);
    assertThat(reportCounter.get()).isEqualTo(2);
  }

  @Test
  public void processReports_withQueriedFilteringId_filtersForTheGivenIds() throws Exception {
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    Report reportWithoutId =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutId, nullFact1), VERSION_0_1);
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
    Report reportWithIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithDefaultId, factWithId, nullFact2), "1.0");

    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(reportWithoutId));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    AtomicLong reportCounter = new AtomicLong();
    AggregationEngine keyAggregationEngine =
        aggregationEngineFactory.createKeyAggregationEngine(
            ImmutableSet.of(UnsignedLong.valueOf(12), UnsignedLong.valueOf(13)));
    ErrorSummaryAggregator errorSummaryAggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(
            /* totalReportCount= */ Optional.empty(), /* errorPercentageThreshold= */ 10);
    Job job = createJob();
    reportAggregator.processReports(
        reportCounter, job, keyAggregationEngine, errorSummaryAggregator);

    // Only the bucket 33333 corresponds to queried filtering id 12 in the reports and is included
    // in the aggregation.
    AggregatedFact expectedFact =
        AggregatedFact.create(/* bucket= */ new BigInteger("33333"), /* metric= */ 33);
    assertThat(keyAggregationEngine.makeAggregation().values()).containsExactly(expectedFact);
    // Privacy budget is consumed for 13 as well even though there are no contributions with this
    // id.
    assertThat(keyAggregationEngine.getPrivacyBudgetUnits().size()).isEqualTo(4);
    assertThat(reportCounter.get()).isEqualTo(2);
  }

  @Test
  public void processReports_withValidationErrors() throws Exception {
    Fact factWithoutId = Fact.builder().setBucket(new BigInteger("11111")).setValue(11).build();
    Fact nullFact1 = Fact.builder().setBucket(new BigInteger("0")).setValue(0).build();
    Report invalidVersionReport =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutId, nullFact1), "VERSION_0_1");
    // Aggregation is done only for contributions corresponding to ids = 12, 13.
    Fact factWithDefaultId =
        Fact.builder()
            .setBucket(new BigInteger("11111"))
            .setValue(11)
            .setId(UnsignedLong.valueOf(13))
            .build();
    Fact factWithId =
        Fact.builder()
            .setBucket(new BigInteger("33333"))
            .setValue(33)
            .setId(UnsignedLong.valueOf(12))
            .build();
    Fact nullFact2 =
        Fact.builder().setBucket(new BigInteger("0")).setValue(0).setId(UnsignedLong.ZERO).build();
    Report reportWithIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithDefaultId, factWithId, nullFact2), "1.0");

    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_1.avro"), ImmutableList.of(invalidVersionReport));
    fakeReportWriter.writeReports(
        reportsDirectory.resolve("reports_2.avro"), ImmutableList.of(reportWithIds));

    AtomicLong reportCounter = new AtomicLong();
    AggregationEngine aggregationEngine =
        aggregationEngineFactory.createKeyAggregationEngine(
            ImmutableSet.of(UnsignedLong.valueOf(12), UnsignedLong.valueOf(13)));
    ErrorSummaryAggregator errorSummaryAggregator =
        ErrorSummaryAggregator.createErrorSummaryAggregator(
            /* totalReportCount= */ Optional.empty(), /* errorPercentageThreshold= */ 10);
    Job job = createJob();
    reportAggregator.processReports(reportCounter, job, aggregationEngine, errorSummaryAggregator);

    assertThat(errorSummaryAggregator.createErrorSummary())
        .isEqualTo(
            ErrorSummaryProto.ErrorSummary.newBuilder()
                .addAllErrorCounts(
                    ImmutableList.of(
                        ErrorCountProto.ErrorCount.newBuilder()
                            .setCategory(ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION.name())
                            .setDescription(
                                ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION.getDescription())
                            .setCount(1L)
                            .build(),
                        ErrorCountProto.ErrorCount.newBuilder()
                            .setCategory(ErrorCounter.NUM_REPORTS_WITH_ERRORS.name())
                            .setDescription(ErrorCounter.NUM_REPORTS_WITH_ERRORS.getDescription())
                            .setCount(1L)
                            .build()))
                .build());
    assertThat(reportCounter.get()).isEqualTo(2);
  }

  private Job createJob() {
    Map<String, String> jobParameters = new HashMap<>();
    jobParameters.put("report_error_threshold_percentage", "100");
    Job job =
        FakeJobGenerator.generate("job_id").toBuilder()
            .setRequestInfo(
                RequestInfoProto.RequestInfo.newBuilder()
                    .putAllJobParameters(jobParameters)
                    // Simulating shards of input.
                    .setInputDataBucketName(reportsDirectory.toAbsolutePath().toString())
                    .setInputDataBlobPrefix("")
                    .build())
            .build();
    return job;
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
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
      Multibinder<ReportValidator> reportValidatorMultibinder =
          Multibinder.newSetBinder(binder(), ReportValidator.class);
      reportValidatorMultibinder.addBinding().to(ReportVersionValidator.class);

      // Stopwatches
      bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);

      // Otel collector
      install(new OtlpJsonLoggingOTelConfigurationModule());
      bind(boolean.class)
          .annotatedWith(com.google.privacysandbox.otel.Annotations.EnableOTelLogs.class)
          .toInstance(false);
      bind(double.class)
          .annotatedWith(Annotations.ReportErrorThresholdPercentage.class)
          .toInstance(10.0);

      install(new PrivacyBudgetKeyGeneratorModule());
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
    @Annotations.NonBlockingThreadPool
    ListeningExecutorService provideNonBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @Singleton
    @Annotations.BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @Singleton
    @Annotations.CustomForkJoinThreadPool
    ListeningExecutorService provideCustomForkJoinThreadPool() {
      return newDirectExecutorService();
    }
  }
}
