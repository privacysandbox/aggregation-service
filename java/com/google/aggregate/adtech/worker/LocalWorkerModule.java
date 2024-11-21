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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnablePrivacyBudgetKeyFiltering;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.OutputShardFileSizeBytes;
import com.google.aggregate.adtech.worker.Annotations.ParallelAggregatedFactNoising;
import com.google.aggregate.adtech.worker.Annotations.ReportErrorThresholdPercentage;
import com.google.aggregate.adtech.worker.Annotations.StreamingOutputDomainProcessing;
import com.google.aggregate.adtech.worker.Annotations.SupportedApis;
import com.google.aggregate.adtech.worker.LibraryAnnotations.LocalOutputDirectory;
import com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.local.LocalBlobStorageClientModule;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.adtech.worker.validation.SimulationValidationModule;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.DpNoisedAggregationModule;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.privacysandbox.otel.OtlpJsonLoggingOTelConfigurationModule;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerResultPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobParameters;
import com.google.scp.operator.cpio.metricclient.local.LocalMetricModule;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public final class LocalWorkerModule extends AbstractModule {

  private final LocalWorkerArgs localWorkerArgs;

  public LocalWorkerModule(LocalWorkerArgs localWorkerArgs) {
    this.localWorkerArgs = localWorkerArgs;
  }

  @Override
  protected void configure() {
    bind(Boolean.class)
        .annotatedWith(DomainOptional.class)
        .toInstance(localWorkerArgs.isSkipDomain());
    // Thresholding is disabled for local worker as it is used to aggregate unencrypted data
    bind(Boolean.class).annotatedWith(EnableThresholding.class).toInstance(false);
    bind(OutputDomainProcessor.class)
        .to(localWorkerArgs.getDomainFileFormat().getDomainProcessorClass());

    bind(FileSystem.class).toInstance(FileSystems.getDefault());
    install(new LocalFileJobHandlerModule());
    install(new LocalBlobStorageClientModule());
    install(new SimulationValidationModule());
    install(new LocalMetricModule());
    install(DecryptionModuleSelector.NOOP.getDecryptionModule());

    if (localWorkerArgs.isJsonOutput()) {
      install(new LocalJsonResultLoggerModule());
    } else {
      install(new LocalAvroResultLoggerModule());
    }
    install(new WorkerModule());
    install(new OtlpJsonLoggingOTelConfigurationModule());
    bind(PrivacyBudgetingServiceBridge.class).to(PrivacyBudgetingSelector.UNLIMITED.getBridge());
    install(new PrivacyBudgetKeyGeneratorModule());
    bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);
    bind(PayloadSerdes.class).to(CborPayloadSerdes.class);
    bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class);
    bind(ObjectMapper.class).to(TimeObjectMapper.class);
    bind(JobProcessor.class).to(ConcurrentAggregationProcessor.class);
    bind(Boolean.class)
        .annotatedWith(StreamingOutputDomainProcessing.class)
        .toInstance(localWorkerArgs.isStreamingOutputDomainProcessingEnabled());
    bind(boolean.class).annotatedWith(BenchmarkMode.class).toInstance(false);
    bind(Path.class)
        .annotatedWith(LocalFileJobHandlerPath.class)
        .toInstance(Path.of(localWorkerArgs.getInputDataAvroFile()).toAbsolutePath());
    bind(Path.class)
        .annotatedWith(LocalOutputDirectory.class)
        .toInstance(Path.of(localWorkerArgs.getOutputDirectory()).toAbsolutePath());
    bind(new TypeLiteral<Optional<Path>>() {})
        .annotatedWith(LocalFileJobHandlerResultPath.class)
        .toInstance(
            Optional.of(
                Path.of(localWorkerArgs.getOutputDirectory())
                    .resolve("result_info.json")
                    .toAbsolutePath()));
    // Privacy parameters
    bind(Distribution.class)
        .annotatedWith(NoisingDistribution.class)
        .toInstance(Distribution.LAPLACE);
    bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(localWorkerArgs.getEpsilon());
    bind(long.class)
        .annotatedWith(NoisingL1Sensitivity.class)
        .toInstance(localWorkerArgs.getL1Sensitivity());
    bind(double.class).annotatedWith(NoisingDelta.class).toInstance(localWorkerArgs.getDelta());
    bind(boolean.class)
        .annotatedWith(ParallelAggregatedFactNoising.class)
        .toInstance(localWorkerArgs.isParallelAggregatedFactNoisingEnabled());

    if (localWorkerArgs.isNoNoising()) {
      install(new ConstantNoiseModule());
    } else {
      install(new DpNoisedAggregationModule());
    }

    // Response related flags
    bind(boolean.class)
        .annotatedWith(EnableStackTraceInResponse.class)
        .toInstance(localWorkerArgs.isEnableReturningStackTraceInResponse());
    bind(int.class)
        .annotatedWith(MaxDepthOfStackTrace.class)
        .toInstance(localWorkerArgs.getMaximumDepthOfStackTrace());
    bind(double.class)
        .annotatedWith(ReportErrorThresholdPercentage.class)
        .toInstance(localWorkerArgs.getReportErrorThresholdPercentage());
    bind(long.class)
        .annotatedWith(OutputShardFileSizeBytes.class)
        .toInstance(localWorkerArgs.getOutputShardFileSizeBytes());

    bind(boolean.class)
        .annotatedWith(EnablePrivacyBudgetKeyFiltering.class)
        .toInstance(localWorkerArgs.isLabeledPrivacyBudgetKeysEnabled());
  }

  @Provides
  @SupportedApis
  ImmutableSet<String> providesSupportedApis() {
    if (localWorkerArgs.isAttributionReportingDebugApiEnabled()) {
      return ImmutableSet.of(
          ATTRIBUTION_REPORTING_API,
          ATTRIBUTION_REPORTING_DEBUG_API,
          PROTECTED_AUDIENCE_API,
          SHARED_STORAGE_API);
    } else {
      return ImmutableSet.of(ATTRIBUTION_REPORTING_API, PROTECTED_AUDIENCE_API, SHARED_STORAGE_API);
    }
  }

  @Provides
  @LocalFileJobParameters
  Supplier<ImmutableMap<String, String>> providesLocalFileJobParameters() {
    ImmutableMap.Builder<String, String> jobParametersBuilder = ImmutableMap.builder();
    if (localWorkerArgs.getDomainAvroFile() != null) {
      Path localOutputDomainPath = Paths.get(localWorkerArgs.getDomainAvroFile());
      jobParametersBuilder
          .put(
              "output_domain_bucket_name",
              localOutputDomainPath.getParent() == null
                  ? ""
                  : localOutputDomainPath.getParent().toAbsolutePath().toString())
          .put("output_domain_blob_prefix", localOutputDomainPath.getFileName().toString());
    }
    if (localWorkerArgs.isDebugRun()) {
      jobParametersBuilder.put("debug_run", "true");
    }
    if (!Strings.isNullOrEmpty(localWorkerArgs.getFilteringIds())) {
      jobParametersBuilder.put(JobUtils.JOB_PARAM_FILTERING_IDS, localWorkerArgs.getFilteringIds());
    }
    return () -> (jobParametersBuilder.build());
  }

  @Provides
  @Singleton
  @NonBlockingThreadPool
  ListeningExecutorService provideNonBlockingThreadPool() {
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(localWorkerArgs.getNonBlockingThreadPoolSize()));
  }

  @Provides
  @Singleton
  @BlockingThreadPool
  ListeningExecutorService provideBlockingThreadPool() {
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(localWorkerArgs.getBlockingThreadPoolSize()));
  }

  @Provides
  @Singleton
  @CustomForkJoinThreadPool
  ListeningExecutorService provideCustomForkJoinThreadPool() {
    return MoreExecutors.listeningDecorator(
        new ForkJoinPool(localWorkerArgs.getNonBlockingThreadPoolSize()));
  }
}
