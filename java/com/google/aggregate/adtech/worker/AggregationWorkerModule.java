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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
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
import com.google.aggregate.adtech.worker.validation.SimulationValidationModule;
import com.google.aggregate.adtech.worker.validation.ValidationModule;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.AwsStopwatchExporter.StopwatchBucketName;
import com.google.aggregate.perf.export.AwsStopwatchExporter.StopwatchKeyName;
import com.google.aggregate.perf.export.PlainFileStopwatchExporter;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3EndpointOverrideBinding;
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
import com.google.scp.operator.cpio.cryptoclient.HttpPrivateKeyFetchingService.PrivateKeyServiceBaseUrl;
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

    // Dependencies for aggregation worker processor

    bind(RecordReaderFactory.class).to(args.getEncryptedRecordReader().getReaderFactoryClass());

    // decryption and deserialization
    bind(String.class)
        .annotatedWith(PrivateKeyServiceBaseUrl.class)
        .toInstance(args.getPrivateKeyServiceBaseUrl());
    bind(String.class)
        .annotatedWith(CoordinatorAEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getCoordinatorAEncryptionKeyServiceBaseUrl());
    bind(String.class)
        .annotatedWith(CoordinatorBEncryptionKeyServiceBaseUrl.class)
        .toInstance(args.getCoordinatorBEncryptionKeyServiceBaseUrl());

    install(args.getDecryptionModuleSelector().getDecryptionModule());
    bind(PayloadSerdes.class).to(CborPayloadSerdes.class); // CBOR is the only allowed report format
    bind(RecordDecrypter.class).to(DeserializingReportDecrypter.class);
    // decryption key service.
    install(args.getDecryptionServiceSelector().getDecryptionKeyClientModule());
    // determines how/where to read the decryption key.
    switch (args.getDecryptionServiceSelector()) {
      case LOCAL_FILE_DECRYPTION_KEY_SERVICE:
        bind(Path.class)
            .annotatedWith(DecryptionKeyFilePath.class)
            .toInstance(Paths.get(args.getLocalFileDecryptionKeyPath()));
        break;
    }

    // validations
    if (args.isSimulationInputs()) {
      install(new SimulationValidationModule());
    } else {
      install(new ValidationModule());
    }

    // Aggregation processor
    bind(JobProcessor.class).to(ConcurrentAggregationProcessor.class);

    // noising
    install(args.getNoisingSelector().getNoisingModule());

    // result logger
    install(args.resultLoggerModuleSelector().getResultLoggerModule());
    switch (args.resultLoggerModuleSelector()) {
      case LOCAL_TO_CLOUD:
        bind(Path.class)
            .annotatedWith(ResultWorkingDirectory.class)
            .toInstance(Paths.get(args.getResultWorkingDirectoryPathString()));
        break;
    }

    // Privacy budgeting.
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

    // Benchmark Mode for Perftests
    bind(boolean.class).annotatedWith(BenchmarkMode.class).toInstance(args.getBenchmarkMode());

    // Stopwatch exporting
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

    // Privacy parameters
    bind(Distribution.class)
        .annotatedWith(NoisingDistribution.class)
        .toInstance(args.getNoisingDistribution());
    bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(args.getNoisingEpsilon());
    bind(long.class)
        .annotatedWith(NoisingL1Sensitivity.class)
        .toInstance(args.getNoisingL1Sensitivity());
    bind(double.class).annotatedWith(NoisingDelta.class).toInstance(args.getNoisingDelta());

    // Response related flags
    bind(boolean.class)
        .annotatedWith(EnableStackTraceInResponse.class)
        .toInstance(args.isEnableReturningStackTraceInResponse());
    bind(int.class)
        .annotatedWith(MaxDepthOfStackTrace.class)
        .toInstance(args.getMaximumDepthOfStackTrace());
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
    return () -> (jobParametersBuilder.build());
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
}
