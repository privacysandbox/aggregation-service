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
import com.google.aggregate.adtech.worker.LocalFileToCloudStorageLogger.ResultWorkingDirectory;
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
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.adtech.worker.validation.SimulationValidationModule;
import com.google.aggregate.adtech.worker.validation.ValidationModule;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.AwsStopwatchExporter.StopwatchBucketName;
import com.google.aggregate.perf.export.AwsStopwatchExporter.StopwatchKeyName;
import com.google.aggregate.perf.export.PlainFileStopwatchExporter;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.privacysandbox.otel.Annotations.GrpcOtelCollectorEndpoint;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3EndpointOverrideBinding;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.cpio.configclient.Annotations.CoordinatorARegionBindingOverride;
import com.google.scp.operator.cpio.configclient.Annotations.CoordinatorBRegionBindingOverride;
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
import com.google.scp.operator.cpio.cryptoclient.aws.Annotations.KmsEndpointOverride;
import com.google.scp.operator.cpio.cryptoclient.local.LocalFileDecryptionKeyServiceModule.DecryptionKeyFilePath;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.aws.AwsPbsClientModule;
import com.google.scp.operator.cpio.jobclient.aws.AwsJobHandlerModule.DdbEndpointOverrideBinding;
import com.google.scp.operator.cpio.jobclient.aws.AwsJobHandlerModule.SqsEndpointOverrideBinding;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerResultPath;
import com.google.scp.operator.cpio.jobclient.local.LocalFileJobHandlerModule.LocalFileJobParameters;
import com.google.scp.operator.cpio.lifecycleclient.aws.AwsLifecycleModule.AutoScalingEndpointOverrideBinding;
import com.google.scp.operator.cpio.metricclient.aws.AwsMetricModule.CloudwatchEndpointOverrideBinding;
import com.google.scp.shared.clients.configclient.Annotations.ApplicationRegionBindingOverride;
import com.google.scp.shared.clients.configclient.aws.AwsClientConfigModule.AwsCredentialAccessKey;
import com.google.scp.shared.clients.configclient.aws.AwsClientConfigModule.AwsCredentialSecretKey;
import com.google.scp.shared.clients.configclient.aws.AwsClientConfigModule.AwsEc2MetadataEndpointOverride;
import com.google.scp.shared.clients.configclient.aws.AwsParameterModule.Ec2EndpointOverrideBinding;
import com.google.scp.shared.clients.configclient.aws.AwsParameterModule.SsmEndpointOverrideBinding;
import com.google.scp.shared.clients.configclient.aws.StsClientModule;
import com.google.scp.shared.clients.configclient.aws.StsClientModule.StsEndpointOverrideBinding;
import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import javax.inject.Singleton;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

public final class AggregationWorkerModule extends AbstractModule {

  private final AggregationWorkerArgs args;

  public AggregationWorkerModule(AggregationWorkerArgs args) {
    this.args = args;
  }

  @Override
  protected void configure() {
    bind(Boolean.class).annotatedWith(DomainOptional.class).toInstance(args.isDomainOptional());
    bind(Boolean.class)
        .annotatedWith(EnableThresholding.class)
        .toInstance(args.isEnableThresholding());
    bind(OutputDomainProcessor.class).to(args.getDomainFileFormat().getDomainProcessorClass());

    install(new WorkerModule());
    install(args.getClientConfigSelector().getClientConfigGuiceModule());
    install(args.getJobClient().getPullerGuiceModule());
    bind(SdkHttpClient.class).toInstance(ApacheHttpClient.builder().build());

    switch (args.getBlobStorageClientSelector()) {
      case AWS_S3_CLIENT:
        bind(URI.class)
            .annotatedWith(S3EndpointOverrideBinding.class)
            .toInstance(args.getS3EndpointOverride());
        OptionalBinder.newOptionalBinder(
                binder(), Key.get(Boolean.class, S3UsePartialRequests.class))
            .setBinding()
            .toInstance(true);
        break;
      case LOCAL_FS_CLIENT:
        bind(FileSystem.class).toInstance(FileSystems.getDefault());
    }
    install(args.getBlobStorageClientSelector().getBlobStorageClientSelectorModule());
    // Binding/installing puller-specific classes and objects, mainly based on the CLI arguments.
    // Ideally this would happen in the relevant modules, but since they cannot have access to the
    // CLI args, it is done here.
    switch (args.getJobClient()) {
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
        bind(ObjectMapper.class).to(TimeObjectMapper.class);
        break;
    }

    install(args.getParamClient().getParameterClientModule());
    // Binding/installing parameter store values when providing them from cli args.
    // Ideally this would happen in the relevant modules, but since they cannot have access to the
    // CLI args, it is done here.
    switch (args.getParamClient()) {
      case ARGS:
        bind(String.class)
            .annotatedWith(SqsJobQueueUrlParameter.class)
            .toInstance(args.getAwsSqsQueueUrl());
        bind(String.class)
            .annotatedWith(DdbJobMetadataTableNameParameter.class)
            .toInstance(args.getAwsMetadatadbTableName());
        bind(String.class)
            .annotatedWith(MaxJobNumAttemptsParameter.class)
            .toInstance(args.getMaxJobNumAttempts());
        bind(String.class)
            .annotatedWith(MaxJobProcessingTimeSecondsParameter.class)
            .toInstance(args.getMessageVisibilityTimeoutSeconds());
        bind(String.class)
            .annotatedWith(CoordinatorARoleArn.class)
            .toInstance(args.getCoordinatorARoleArn());
        bind(String.class)
            .annotatedWith(CoordinatorBRoleArn.class)
            .toInstance(args.getCoordinatorBRoleArn());
        bind(String.class)
            .annotatedWith(CoordinatorKmsArnParameter.class)
            .toInstance(args.getKmsSymmetricKey());
        bind(String.class)
            .annotatedWith(ScaleInHookParameter.class)
            .toInstance(args.getScaleInHook());
        break;
      case AWS:
        break;
    }

    bind(String.class).annotatedWith(AwsCredentialAccessKey.class).toInstance(args.getAccessKey());
    bind(String.class).annotatedWith(AwsCredentialSecretKey.class).toInstance(args.getAccessKey());
    bind(String.class)
        .annotatedWith(AwsEc2MetadataEndpointOverride.class)
        .toInstance(args.getAwsMetadataEndpointOverride());
    bind(URI.class)
        .annotatedWith(CloudwatchEndpointOverrideBinding.class)
        .toInstance(args.getCloudwatchEndpointOverride());
    bind(URI.class)
        .annotatedWith(Ec2EndpointOverrideBinding.class)
        .toInstance(args.getEc2EndpointOverride());
    bind(URI.class)
        .annotatedWith(SqsEndpointOverrideBinding.class)
        .toInstance(args.getSqsEndpointOverride());
    bind(URI.class)
        .annotatedWith(SsmEndpointOverrideBinding.class)
        .toInstance(args.getSsmEndpointOverride());
    bind(URI.class)
        .annotatedWith(DdbEndpointOverrideBinding.class)
        .toInstance(args.getDdbEndpointOverride());
    bind(URI.class)
        .annotatedWith(StsEndpointOverrideBinding.class)
        .toInstance(args.getStsEndpointOverride());
    bind(URI.class)
        .annotatedWith(KmsEndpointOverride.class)
        .toInstance(args.getKmsEndpointOverride());
    bind(String.class)
        .annotatedWith(ApplicationRegionBindingOverride.class)
        .toInstance(args.getAdtechRegionOverride());
    bind(String.class)
        .annotatedWith(CoordinatorARegionBindingOverride.class)
        .toInstance(args.getCoordinatorARegionOverride());
    bind(String.class)
        .annotatedWith(CoordinatorBRegionBindingOverride.class)
        .toInstance(args.getCoordinatorBRegionOverride());
    bind(URI.class)
        .annotatedWith(AutoScalingEndpointOverrideBinding.class)
        .toInstance(args.getAutoScalingEndpointOverride());
    install(new StsClientModule());
    install(args.getLifecycleClient().getLifecycleModule());

    install(args.getMetricClient().getMetricModule());

    // Dependencies for aggregation worker processor.
    bind(RecordReaderFactory.class).to(args.getEncryptedRecordReader().getReaderFactoryClass());

    // Dependencies for decryption and deserialization.
    bind(String.class)
        .annotatedWith(CoordinatorAEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getCoordinatorAEncryptionKeyServiceBaseUrl());
    bind(String.class)
        .annotatedWith(CoordinatorBEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getCoordinatorBEncryptionKeyServiceBaseUrl());

    install(args.getDecryptionModuleSelector().getDecryptionModule());
    bind(PayloadSerdes.class).to(CborPayloadSerdes.class); // CBOR is the only allowed report format
    bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class);

    // Dependencies for the decryption key service.
    install(args.getDecryptionServiceSelector().getDecryptionKeyClientModule());
    // Determines how/where to read the decryption key.
    switch (args.getDecryptionServiceSelector()) {
      case LOCAL_FILE_DECRYPTION_KEY_SERVICE:
        bind(Path.class)
            .annotatedWith(DecryptionKeyFilePath.class)
            .toInstance(Paths.get(args.getLocalFileDecryptionKeyPath()));
        break;
    }

    // Validations.
    if (args.isSimulationInputs()) {
      install(new SimulationValidationModule());
    } else {
      install(new ValidationModule());
    }

    // Dependency for the aggregation processor.
    bind(JobProcessor.class).to(ConcurrentAggregationProcessor.class);

    // Noising module.
    install(args.getNoisingSelector().getNoisingModule());
    bind(boolean.class)
        .annotatedWith(ParallelAggregatedFactNoising.class)
        .toInstance(args.isParallelAggregatedFactNoisingEnabled());

    // Result logger module.
    install(args.resultLoggerModuleSelector().getResultLoggerModule());
    switch (args.resultLoggerModuleSelector()) {
      case LOCAL_TO_CLOUD:
        bind(Path.class)
            .annotatedWith(ResultWorkingDirectory.class)
            .toInstance(Paths.get(args.getResultWorkingDirectoryPathString()));
        break;
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

    // Dependencies for privacy budgeting.
    bind(PrivacyBudgetingServiceBridge.class).to(args.getPrivacyBudgeting().getBridge());
    if (args.getPrivacyBudgeting() == PrivacyBudgetingSelector.HTTP) {
      bind(String.class)
          .annotatedWith(CoordinatorAPrivacyBudgetServiceBaseUrl.class)
          .toInstance(args.getCoordinatorAPrivacyBudgetingEndpoint());
      bind(String.class)
          .annotatedWith(CoordinatorBPrivacyBudgetServiceBaseUrl.class)
          .toInstance(args.getCoordinatorBPrivacyBudgetingEndpoint());
      bind(String.class)
          .annotatedWith(CoordinatorAPrivacyBudgetServiceAuthEndpoint.class)
          .toInstance(args.getCoordinatorAPrivacyBudgetServiceAuthEndpoint());
      bind(String.class)
          .annotatedWith(CoordinatorBPrivacyBudgetServiceAuthEndpoint.class)
          .toInstance(args.getCoordinatorBPrivacyBudgetServiceAuthEndpoint());
      install(new AwsPbsClientModule());
    }
    install(new PrivacyBudgetKeyGeneratorModule());

    // Benchmark Mode for Perf tests.
    bind(boolean.class).annotatedWith(BenchmarkMode.class).toInstance(args.getBenchmarkMode());

    // Stopwatch exporting.
    bind(StopwatchExporter.class).to(args.getStopwatchExportSelector().getExporterClass());
    switch (args.getStopwatchExportSelector()) {
      case NO_OP:
        break;
      case PLAIN_FILE:
        bind(Path.class)
            .annotatedWith(PlainFileStopwatchExporter.StopwatchExporterFileLocation.class)
            .toInstance(Paths.get(args.getLocalPlainStopwatchFile()));
        break;
      case AWS:
        bind(String.class)
            .annotatedWith(StopwatchBucketName.class)
            .toInstance(args.getStopwatchBucketName());
        bind(String.class)
            .annotatedWith(StopwatchKeyName.class)
            .toInstance(args.getStopwatchKeyName());
        break;
    }

    // Privacy parameters.
    bind(Distribution.class)
        .annotatedWith(NoisingDistribution.class)
        .toInstance(args.getNoisingDistribution());
    bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(args.getNoisingEpsilon());
    bind(long.class)
        .annotatedWith(NoisingL1Sensitivity.class)
        .toInstance(args.getNoisingL1Sensitivity());
    bind(double.class).annotatedWith(NoisingDelta.class).toInstance(args.getNoisingDelta());

    // Otel exporter.
    switch (args.getOTelExporterSelector()) {
        // Specifying CollectorEndpoint is required for GRPC exporter because aggregation service
        // would send metric to the CollectorEndpoint and thus collector/exporter could collect.
      case GRPC:
        bind(String.class)
            .annotatedWith(GrpcOtelCollectorEndpoint.class)
            .toInstance(args.getGrpcCollectorEndpoint());
        break;
      default:
        break;
    }
    install(args.getOTelExporterSelector().getOTelConfigurationModule());

    // Response related flags.
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
  @LocalFileJobParameters
  Supplier<ImmutableMap<String, String>> providesLocalFileJobParameters() {
    ImmutableMap.Builder<String, String> jobParametersBuilder = ImmutableMap.builder();
    if (!args.getLocalOutputDomainPath().isEmpty()) {
      Path localOutputDomainPath = Paths.get(args.getLocalOutputDomainPath());
      File domainFile = new File(args.getLocalOutputDomainPath());
      if (domainFile.isFile()) {
        jobParametersBuilder
            .put(
                "output_domain_bucket_name",
                localOutputDomainPath.getParent() == null
                    ? ""
                    : localOutputDomainPath.getParent().toAbsolutePath().toString())
            .put("output_domain_blob_prefix", localOutputDomainPath.getFileName().toString());
      } else {
        // if the domain file is a directory
        jobParametersBuilder
            .put("output_domain_bucket_name", localOutputDomainPath.toAbsolutePath().toString())
            .put("output_domain_blob_prefix", "");
      }
    }
    if (args.isDebugRun()) {
      jobParametersBuilder.put("debug_run", "true");
    }
    if (!Strings.isNullOrEmpty(args.getFilteringIds())) {
      jobParametersBuilder.put(JobUtils.JOB_PARAM_FILTERING_IDS, args.getFilteringIds());
    }
    return () -> (jobParametersBuilder.build());
  }

  @Provides
  @Singleton
  @NonBlockingThreadPool
  ListeningExecutorService provideNonBlockingThreadPool() {
    // TODO(b/281572881): Investigate on optimal value for nonBlockingThreadPool size.
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(args.getNonBlockingThreadPoolSize()));
  }

  @Provides
  @Singleton
  @BlockingThreadPool
  ListeningExecutorService provideBlockingThreadPool() {
    // TODO(b/281572881): Investigate on optimal value for blockingThreadPool size.
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(args.getBlockingThreadPoolSize()));
  }

  @Provides
  @Singleton
  @CustomForkJoinThreadPool
  ListeningExecutorService provideCustomForkJoinThreadPool() {
    return MoreExecutors.listeningDecorator(new ForkJoinPool(args.getNonBlockingThreadPoolSize()));
  }
}
