/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.adtech.worker.gcp;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableParallelSummaryUpload;
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
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.LocalFileToCloudStorageLogger.ResultWorkingDirectory;
import com.google.aggregate.adtech.worker.PrivacyBudgetingSelector;
import com.google.aggregate.adtech.worker.ResultLoggerModuleSelector;
import com.google.aggregate.adtech.worker.WorkerModule;
import com.google.aggregate.adtech.worker.aggregation.concurrent.ConcurrentAggregationProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.decryption.DeserializingReportDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.validation.SimulationValidationModule;
import com.google.aggregate.adtech.worker.validation.ValidationModule;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.privacysandbox.otel.Annotations.GrpcOtelCollectorEndpoint;
import com.google.scp.operator.cpio.blobstorageclient.gcp.Annotations.GcsEndpointUrl;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorARoleArn;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorBRoleArn;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorKmsArnParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.DdbJobMetadataTableNameParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.MaxJobNumAttemptsParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.MaxJobProcessingTimeSecondsParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.ScaleInHookParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.SqsJobQueueUrlParameter;
import com.google.scp.operator.cpio.cryptoclient.Annotations.CoordinatorAEncryptionKeyServiceBaseUrl;
import com.google.scp.operator.cpio.cryptoclient.Annotations.CoordinatorBEncryptionKeyServiceBaseUrl;
import com.google.scp.operator.cpio.cryptoclient.Annotations.DecrypterCacheEntryTtlSec;
import com.google.scp.operator.cpio.cryptoclient.Annotations.ExceptionCacheEntryTtlSec;
import com.google.scp.operator.cpio.cryptoclient.gcp.GcpKmsDecryptionKeyServiceConfig;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.jobclient.gcp.GcpJobHandlerConfig;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerResultPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobParameters;
import com.google.scp.operator.worker.decryption.hybrid.HybridDecryptionModule;
import com.google.scp.operator.worker.decryption.hybrid.HybridDeserializingReportDecrypter;
import com.google.scp.operator.worker.model.serdes.ReportSerdes;
import com.google.scp.operator.worker.model.serdes.proto.ProtoReportSerdes;
import com.google.scp.operator.worker.reader.RecordReaderFactory;
import com.google.scp.operator.worker.reader.avro.LocalNioPathAvroReaderFactory;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpInstanceIdOverride;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpInstanceNameOverride;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpProjectIdOverride;
import com.google.scp.shared.clients.configclient.gcp.Annotations.GcpZoneOverride;
import com.google.scp.shared.clients.configclient.gcp.GcpOperatorClientConfig;
import com.google.scp.shared.clients.configclient.model.WorkerParameter;
import com.google.scp.shared.mapper.TimeObjectMapper;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import javax.inject.Singleton;

public final class AggregationWorkerModule extends AbstractModule {

  private final AggregationWorkerArgs args;

  public AggregationWorkerModule(AggregationWorkerArgs args) {
    this.args = args;
  }

  @Override
  protected void configure() {
    bind(OutputDomainProcessor.class).to(args.getDomainFileFormat().getDomainProcessorClass());
    install(new WorkerModule());
    install(args.getClientConfigSelector().getClientConfigGuiceModule());

    // Thresholding is disabled for local worker as it is used to aggregate unencrypted data
    bind(Boolean.class).annotatedWith(EnableThresholding.class).toInstance(false);

    switch (args.getBlobStorageClientSelector()) {
      case GCP_CS_CLIENT:
        bind(new TypeLiteral<Optional<String>>() {})
            .annotatedWith(GcsEndpointUrl.class)
            .toInstance(args.getGcsEndpoint());
        break;
      case LOCAL_FS_CLIENT:
        bind(FileSystem.class).toInstance(FileSystems.getDefault());
    }
    install(args.getBlobStorageClientSelector().getBlobStorageClientSelectorModule());

    switch (args.getJobClientSelector()) {
      case LOCAL_FILE:
        bind(Path.class)
            .annotatedWith(LocalFileJobHandlerPath.class)
            .toInstance(Paths.get(args.getLocalFileSinglePullerPath()));
        Optional<Path> localJobInfoPath = Optional.empty();
        if (!args.getLocalFileJobInfoPath().isEmpty()) {
          localJobInfoPath = Optional.of(Paths.get(args.getLocalFileJobInfoPath()));
        }
        bind(new TypeLiteral<Optional<Path>>() {})
            .annotatedWith(LocalFileJobHandlerResultPath.class)
            .toInstance(localJobInfoPath);
        bind(new TypeLiteral<Supplier<ImmutableMap<String, String>>>() {})
            .annotatedWith(LocalFileJobParameters.class)
            .toInstance(Suppliers.ofInstance(ImmutableMap.of()));
        break;
      case GCP:
        GcpJobHandlerConfig config =
            GcpJobHandlerConfig.builder()
                .setGcpProjectId(args.getGcpProjectId())
                .setPubSubMaxMessageSizeBytes(1000)
                .setPubSubMessageLeaseSeconds(600)
                .setMaxNumAttempts(5)
                .setPubSubTopicId(args.getPubSubTopicId())
                .setPubSubSubscriptionId(args.getPubSubSubscriptionId())
                .setSpannerInstanceId(args.getSpannerInstanceId())
                .setSpannerDbName(args.getSpannerDbName())
                .setSpannerEndpoint(args.getSpannerEndpoint())
                .setPubSubEndpoint(args.getPubSubEndpoint())
                .build();
        bind(GcpJobHandlerConfig.class).toInstance(config);
        break;
    }
    bind(ObjectMapper.class).to(TimeObjectMapper.class);
    install(args.getJobClientSelector().getPullerGuiceModule());
    install(args.getParamClientSelector().getParameterClientModule());

    switch (args.getParamClientSelector()) {
      case ARGS:
        // These are required because LocalOperatorParameterModule needs these to be injected.
        bind(String.class).annotatedWith(SqsJobQueueUrlParameter.class).toInstance("");
        bind(String.class).annotatedWith(DdbJobMetadataTableNameParameter.class).toInstance("");
        bind(String.class).annotatedWith(MaxJobNumAttemptsParameter.class).toInstance("5");
        bind(String.class)
            .annotatedWith(MaxJobProcessingTimeSecondsParameter.class)
            .toInstance("60");
        bind(String.class).annotatedWith(CoordinatorARoleArn.class).toInstance("");
        bind(String.class).annotatedWith(CoordinatorBRoleArn.class).toInstance("");
        bind(String.class).annotatedWith(CoordinatorKmsArnParameter.class).toInstance("");
        bind(String.class).annotatedWith(ScaleInHookParameter.class).toInstance("");
        break;
      case AWS:
        break;
      case GCP:
        break;
    }

    bind(String.class).annotatedWith(GcpProjectIdOverride.class).toInstance(args.getGcpProjectId());
    bind(String.class)
        .annotatedWith(GcpInstanceIdOverride.class)
        .toInstance(args.getGcpInstanceIdOverride());
    bind(String.class)
        .annotatedWith(GcpInstanceNameOverride.class)
        .toInstance(args.getGcpInstanceNameOverride());
    bind(String.class).annotatedWith(GcpZoneOverride.class).toInstance(args.getGcpZoneOverride());
    GcpOperatorClientConfig.Builder clientConfigBuilder =
        GcpOperatorClientConfig.builder()
            .setCoordinatorAServiceAccountToImpersonate(args.getCoordinatorAServiceAccount())
            .setCoordinatorAWipProvider(args.getCoordinatorAWipProvider())
            .setUseLocalCredentials(args.getCoordinatorAWipProvider().isEmpty())
            .setCoordinatorAEncryptionKeyServiceBaseUrl(
                args.getPrimaryEncryptionKeyServiceBaseUrl())
            .setCoordinatorAEncryptionKeyServiceCloudfunctionUrl(
                args.getPrimaryEncryptionKeyServiceCloudfunctionUrl());
    GcpOperatorClientConfig.builder()
        .setCoordinatorAServiceAccountToImpersonate(args.getCoordinatorAServiceAccount())
        .setCoordinatorAWipProvider(args.getCoordinatorAWipProvider())
        .setUseLocalCredentials(args.getCoordinatorAWipProvider().isEmpty());
    if (!args.getCoordinatorBWipProvider().isEmpty()) {
      clientConfigBuilder.setCoordinatorBWipProvider(
          Optional.of(args.getCoordinatorBWipProvider()));
      clientConfigBuilder
          .setCoordinatorBServiceAccountToImpersonate(
              Optional.of(args.getCoordinatorBServiceAccount()))
          .setCoordinatorBEncryptionKeyServiceBaseUrl(
              Optional.of(args.getSecondaryEncryptionKeyServiceBaseUrl()))
          .setCoordinatorBEncryptionKeyServiceCloudfunctionUrl(
              args.getSecondaryEncryptionKeyServiceCloudfunctionUrl());
    }
    bind(GcpOperatorClientConfig.class).toInstance(clientConfigBuilder.build());
    install(args.getLifeCycleClientSelector().getLifecycleModule());
    install(args.getMetricCycleClientSelector().getMetricModule());

    // Dependencies for aggregation worker processor
    bind(RecordReaderFactory.class).to(LocalNioPathAvroReaderFactory.class);

    // decryption and deserialization
    bind(String.class)
        .annotatedWith(CoordinatorAEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getPrimaryEncryptionKeyServiceBaseUrl());
    bind(String.class)
        .annotatedWith(CoordinatorBEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getSecondaryEncryptionKeyServiceBaseUrl());

    install(new HybridDecryptionModule());
    bind(ReportSerdes.class).to(ProtoReportSerdes.class);
    bind(com.google.scp.operator.worker.decryption.RecordDecrypter.class)
        .to(HybridDeserializingReportDecrypter.class);

    // determines how/where to read the decryption key.
    GcpKmsDecryptionKeyServiceConfig.Builder decryptionConfigBuilder =
        GcpKmsDecryptionKeyServiceConfig.builder()
            .setCoordinatorAKmsKeyUri(args.getCoodinatorAKmsKey())
            .setCoordinatorAEncodedKeysetHandle(args.getTestEncodedKeysetHandle())
            .setCoordinatorBEncodedKeysetHandle(args.getTestCoordinatorBEncodedKeysetHandle());

    if (!args.getCoodinatorBKmsKey().isEmpty()) {
      decryptionConfigBuilder.setCoordinatorBKmsKeyUri(Optional.of(args.getCoodinatorBKmsKey()));
    }
    bind(GcpKmsDecryptionKeyServiceConfig.class).toInstance(decryptionConfigBuilder.build());

    // installs the appropriate pbs client module
    bind(PrivacyBudgetingServiceBridge.class).to(args.getPrivacyBudgeting().getBridge());
    if (args.getPrivacyBudgeting() == PrivacyBudgetingSelector.HTTP) {
      bind(String.class)
          .annotatedWith(CoordinatorAPrivacyBudgetServiceBaseUrl.class)
          .toInstance(args.getCoordinatorAPrivacyBudgetingServiceUrl());
      bind(String.class)
          .annotatedWith(CoordinatorBPrivacyBudgetServiceBaseUrl.class)
          .toInstance(args.getCoordinatorBPrivacyBudgetingServiceUrl());
      bind(String.class)
          .annotatedWith(CoordinatorAPrivacyBudgetServiceAuthEndpoint.class)
          .toInstance(args.getCoordinatorAPrivacyBudgetingServiceAuthEndpoint());
      bind(String.class)
          .annotatedWith(CoordinatorBPrivacyBudgetServiceAuthEndpoint.class)
          .toInstance(args.getCoordinatorBPrivacyBudgetingServiceAuthEndpoint());
      install(args.getPbsclientSelector().getDistributedPrivacyBudgetClientModule());
    }
    install(new PrivacyBudgetKeyGeneratorModule());

    // decryption key service.
    install(args.getDecryptionModuleSelector().getDecryptionModule());
    bind(PayloadSerdes.class).to(CborPayloadSerdes.class); // CBOR is the only allowed report format
    bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class); // validations
    install(args.getDecryptionKeyClientSelector().getDecryptionKeyClientModule());

    // Benchmark Mode for Perftests
    bind(boolean.class).annotatedWith(BenchmarkMode.class).toInstance(args.getBenchmarkMode());

    // Stopwatch exporting
    bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);

    // Privacy parameters
    bind(Distribution.class)
        .annotatedWith(NoisingDistribution.class)
        .toInstance(args.getNoisingDistribution());
    bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(args.getNoisingEpsilon());
    bind(long.class)
        .annotatedWith(NoisingL1Sensitivity.class)
        .toInstance(args.getNoisingL1Sensitivity());
    bind(double.class).annotatedWith(NoisingDelta.class).toInstance(args.getNoisingDelta());

    // validations
    if (args.isSimulationInputs()) {
      install(new SimulationValidationModule());
    } else {
      install(new ValidationModule());
    }

    // processor
    bind(JobProcessor.class).to(ConcurrentAggregationProcessor.class);

    // noising
    install(args.getNoisingSelector().getNoisingModule());
    bind(boolean.class)
        .annotatedWith(ParallelAggregatedFactNoising.class)
        .toInstance(args.isParallelAggregatedFactNoisingEnabled());

    // result logger
    install(args.resultLoggerModuleSelector().getResultLoggerModule());
    if (args.resultLoggerModuleSelector() == ResultLoggerModuleSelector.LOCAL_TO_CLOUD) {
      bind(Path.class)
          .annotatedWith(ResultWorkingDirectory.class)
          .toInstance(Paths.get(args.getResultWorkingDirectoryPathString()));
    }

    // Feature flags.
    bind(boolean.class)
        .annotatedWith(EnableParallelSummaryUpload.class)
        .toInstance(args.isParallelSummaryUploadEnabled());
    bind(boolean.class)
        .annotatedWith(EnablePrivacyBudgetKeyFiltering.class)
        .toInstance(args.isLabeledPrivacyBudgetKeysEnabled());
    bind(boolean.class)
        .annotatedWith(StreamingOutputDomainProcessing.class)
        .toInstance(args.isStreamingOutputDomainProcessingEnabled());

    // Parameter to set key cache. This is a test only flag.
    bind(Long.class)
        .annotatedWith(DecrypterCacheEntryTtlSec.class)
        .toInstance(args.getDecrypterCacheEntryTtlSec());

    // Parameter to set exception cache. This is a test only flag.
    bind(Long.class)
            .annotatedWith(ExceptionCacheEntryTtlSec.class)
            .toInstance(args.getExceptionCacheEntryTtlSec());

    // Response related flags
    bind(boolean.class)
        .annotatedWith(EnableStackTraceInResponse.class)
        .toInstance(args.isEnableReturningStackTraceInResponse());
    bind(int.class)
        .annotatedWith(MaxDepthOfStackTrace.class)
        .toInstance(args.getMaximumDepthOfStackTrace());
    bind(double.class)
        .annotatedWith(ReportErrorThresholdPercentage.class)
        .toInstance(args.getReportErrorThresholdPercentage());
    bind(long.class)
        .annotatedWith(OutputShardFileSizeBytes.class)
        .toInstance(args.getOutputShardFileSizeBytes());

    // Otel exporter
    install(args.getOTelExporterSelector().getOTelConfigurationModule());
  }

  @Provides
  @SupportedApis
  ImmutableSet<String> providesSupportedApis() {
    if (args.isAttributionReportingDebugApiEnabled()) {
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
  @Singleton
  @NonBlockingThreadPool
  ListeningExecutorService provideNonBlockingThreadPool() {
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(args.getNonBlockingThreadPoolSize()));
  }

  @Provides
  @Singleton
  @BlockingThreadPool
  ListeningExecutorService provideBlockingThreadPool() {
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(args.getBlockingThreadPoolSize()));
  }

  @Provides
  @DomainOptional
  Boolean provideDomainOptional() {
    return args.isDomainOptional();
  }

  @Provides
  @GrpcOtelCollectorEndpoint
  String provideGRPCEndpoint(ParameterClient parameterClient) throws ParameterClientException {
    if (parameterClient.getParameter(WorkerParameter.GRPC_COLLECTOR_ENDPOINT.name()).isPresent()) {
      return "http://"
          + parameterClient.getParameter(WorkerParameter.GRPC_COLLECTOR_ENDPOINT.name()).get()
          + ":4317";
    } else {
      return args.getGrpcCollectorEndpoint();
    }
  }

  @Provides
  @Singleton
  @CustomForkJoinThreadPool
  ListeningExecutorService provideCustomForkJoinThreadPool() {
    return MoreExecutors.listeningDecorator(new ForkJoinPool(args.getNonBlockingThreadPoolSize()));
  }
}
