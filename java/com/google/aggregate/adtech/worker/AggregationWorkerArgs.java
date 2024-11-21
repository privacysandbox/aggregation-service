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

import com.beust.jcommander.Parameter;
import com.google.aggregate.adtech.worker.selector.BlobStorageClientSelector;
import com.google.aggregate.adtech.worker.selector.ClientConfigSelector;
import com.google.aggregate.adtech.worker.selector.DecryptionKeyClientSelector;
import com.google.aggregate.adtech.worker.selector.JobClientSelector;
import com.google.aggregate.adtech.worker.selector.LifecycleClientSelector;
import com.google.aggregate.adtech.worker.selector.MetricClientSelector;
import com.google.aggregate.adtech.worker.selector.ParameterClientSelector;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.privacysandbox.otel.OTelExporterSelector;
import java.net.URI;

/**
 * Worker args are runtime flags that are set when building an image or as CLI args when running a
 * standalone binary and set by the Aggregation Service team. They differ from aggregation job
 * params, which are set in the Job Request when requesting an aggregation report. For available job
 * parameters see <a
 * href="https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md">API docs</a>.
 *
 * <p>
 *
 * <p>
 *
 * <p>To add a new worker arg: declare a new parameter in this class and its getter function, update
 * the {@link AggregationWorkerModule} to inject it to the appropriate location, and set the param
 * in the BUILD rules.
 *
 * <p>
 *
 * <p>
 *
 * <p>Use the following convention for naming the new param:
 *
 * <ul>
 *   <li>Use "lower_underscore" style for the 'names' attribute.
 *   <li>Prefer "long_descriptive_names" over "short_names" and noun phrases.
 *   <li>For Boolean flags:
 *       <ul>
 *         <li>Use positive or neutral terms (--foo_enabled rather than --foo_disabled).
 *         <li>Param name should be "feature_name_enabled"
 *         <li>Variable name should be "featureNameEnabled"
 *         <li>Getter name should be "isFeatureNameEnabled(...)"
 *       </ul>
 * </ul>
 */
public final class AggregationWorkerArgs {

  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();

  @Parameter(names = "--client_config_env", description = "Selects client config environment")
  private ClientConfigSelector clientConfigSelector = ClientConfigSelector.AWS;

  @Parameter(names = "--job_client", description = "Job handler client implementation")
  private JobClientSelector jobClient = JobClientSelector.LOCAL_FILE;

  @Parameter(names = "--blob_storage_client", description = "Blob storage client implementation")
  private BlobStorageClientSelector blobStorageClientSelector =
      BlobStorageClientSelector.AWS_S3_CLIENT;

  @Parameter(names = "--param_client", description = "Parameter client implementation")
  private ParameterClientSelector paramClient = ParameterClientSelector.ARGS;

  @Parameter(names = "--lifecycle_client", description = "Lifecycle client implementation")
  private LifecycleClientSelector lifecycleClient = LifecycleClientSelector.LOCAL;

  @Parameter(names = "--metric_client", description = "Metric client implementation")
  private MetricClientSelector metricClient = MetricClientSelector.LOCAL;

  @Parameter(names = "--record_reader", description = "Encrypted record reader implementation")
  private RecordReaderSelector recordReader = RecordReaderSelector.LOCAL_NIO_AVRO;

  @Parameter(names = "--decryption", description = "Decryption implementation")
  private DecryptionModuleSelector decryptionModuleSelector = DecryptionModuleSelector.HYBRID;

  @Parameter(names = "--privacy_budgeting", description = "Implementation of privacy budgeting")
  private PrivacyBudgetingSelector privacyBudgeting = PrivacyBudgetingSelector.UNLIMITED;

  @Parameter(names = "--noising", description = "Noising implementation to use")
  private NoisingSelector noisingSelector = NoisingSelector.DP_NOISING;

  @Parameter(names = "--result_logger", description = "How to log aggregation results")
  private ResultLoggerModuleSelector resultLoggerModuleSelector =
      ResultLoggerModuleSelector.IN_MEMORY;

  @Parameter(
      names = "--local_file_single_puller_path",
      description =
          "Path to the local file provided by the local file single puller(flag makes "
              + "sense only if that puller is used")
  private String localFileSinglePullerPath = "";

  @Parameter(
      names = "--local_output_domain_path",
      description =
          "Path to the local file where output domain is store. Only used with local job client.")
  private String localOutputDomainPath = "";

  @Parameter(
      names = "--local_file_job_info_path",
      description = "Path to the local file to dump the job info to in local mode (empty for none)")
  private String localFileJobInfoPath = "";

  @Parameter(names = "--decryption_key_service", description = "How to read the decryption keys")
  private DecryptionKeyClientSelector decryptionKeyServiceSelector =
      DecryptionKeyClientSelector.LOCAL_FILE_DECRYPTION_KEY_SERVICE;

  @Parameter(
      names = "--local_file_decryption_key_path",
      description =
          "Path to the Tink KeysetHandle used for decryption."
              + " This is used only for the LocalFileDecryptionKeyService.")
  private String localFileDecryptionKeyPath = "";

  @Parameter(
      names = "--coordinator_a_encryption_key_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of the primary (Party A)"
              + " encryption key service base url service, used only for multi-coordinator key"
              + " hosting service. Do not include trailing slash")
  private String coordinatorAEncryptionKeyServiceBaseUrl = "";

  @Parameter(
      names = "--coordinator_b_encryption_key_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of the secondary (Party B)"
              + " encryption key service base url service, used only for multi-coordinator key"
              + " hosting service. Do not include trailing slash")
  private String coordinatorBEncryptionKeyServiceBaseUrl = "";

  @Parameter(
      names = "--coordinator_a_assume_role_arn",
      description =
          "AWS ARN of the role assumed both when authenticating against single party coordinator"
              + " services or the role to assume when communicating with the primary coordinator in"
              + " a distributed coordinator configuration. ARGS param client should be selected to"
              + " use this flag. Unused unless configured to use an AWS coordinator.")
  private String coordinatorARoleArn = "";

  @Parameter(
      names = "--coordinator_b_assume_role_arn",
      description =
          "ARN of the second role assumed when using a distributed coordinator -- unused for single"
              + " coordinator configurations. ARGS param client should be selected to use this"
              + " flag. Unused unless configured to use an AWS coordinator.")
  private String coordinatorBRoleArn = "";

  @Parameter(
      names = "--kms_endpoint_override",
      description = "KMS Endpoint used by worker in AwsKmsV2Client for PK decryption. ")
  private String kmsEndpointOverride = "";

  @Parameter(
      names = "--kms_symmetric_key",
      description = "KMS SymmetricKey ARN. ARGS param client should be selected to use this flag.")
  private String kmsSymmetricKey = "";

  @Parameter(
      names = "--aws_sqs_queue_url",
      description =
          "(Optional) Queue url for AWS SQS, if AWS job client is used, if AWS job client is used"
              + " and ARGS param client is used.")
  private String awsSqsQueueUrl = "";

  @Parameter(
      names = "--max_job_num_attempts",
      description =
          "(Optional) Maximum number of times the job can be picked up by workers, if AWS job"
              + " client is used and ARGS param client is used.")
  private String maxJobNumAttempts = "5";

  @Parameter(
      names = "--jobqueue_message_visibility_timeout_seconds",
      description =
          "(Optional) Job queue message visibility timeout (in seconds), if AWS job client"
              + " is used and ARGS param client is used.")
  private String messageVisibilityTimeoutSeconds = "60";

  @Parameter(
      names = "--scale-in-hook",
      description = "(Optional) Scale in hook used for scaling in the instance.")
  private String scaleInHook = "";

  @Parameter(
      names = "--aws_metadata_endpoint_override",
      description =
          "Optional ec2 metadata endpoint override URI. This is used to get EC2 metadata"
              + " information including tags, and profile credentials. If this parameter is set,"
              + " the instance credentials provider will be used.")
  private String awsMetadataEndpointOverride = "";

  @Parameter(
      names = "--ec2_endpoint_override",
      description = "Optional EC2 service endpoint override URI")
  private String ec2EndpointOverride = "";

  @Parameter(
      names = "--autoscaling_endpoint_override",
      description = "Optional auto scaling service endpoint override URI")
  private String autoScalingEndpointOverride = "";

  @Parameter(names = "--sqs_endpoint_override", description = "Optional Sqs Endpoint override URI")
  private String sqsEndpointOverride = "";

  @Parameter(names = "--ssm_endpoint_override", description = "Optional Ssm Endpoint override URI")
  private String ssmEndpointOverride = "";

  @Parameter(names = "--sts_endpoint_override", description = "Optional STS Endpoint override URI")
  private String stsEndpointOverride = "";

  @Parameter(
      names = "--aws_metadatadb_table_name",
      description =
          "(Optional) Table name for AWS Dynamodb storing job metadata, if AWS job client"
              + " is used, if AWS job client is used and ARGS param client is used.")
  private String awsMetadatadbTableName = "";

  @Parameter(names = "--ddb_endpoint_override", description = "Optional ddb endpoint override URI")
  private String ddbEndpointOverride = "";

  @Parameter(
      names = "--cloudwatch_endpoint_override",
      description = "Optional cloudwatch endpoint override URI")
  private String cloudwatchEndpointOverride = "";

  @Parameter(
      names = "--result_s3_bucket_name",
      description = "Name of the s3 bucket to write results to")
  private String resultS3BucketName = "";

  @Parameter(
      names = "--adtech_region_override",
      description = "Overrides the region of the compute instance.")
  private String adtechRegionOverride = "";

  // TODO(b/241266079): remove trusted_party_region_override after giving migration time.
  @Parameter(
      names = {"--trusted_party_region_override", "--coordinator_a_region_override"},
      description = "Overrides the region of coordinator A services.")
  // TODO(b/241266079): set default to us-east-1 once services move there.
  private String coordinatorARegionOverride = "us-west-2";

  @Parameter(
      names = "--coordinator_b_region_override",
      description = "Overrides the region of coordinator B services.")
  // TODO(b/241266079): set default to us-east-1 once services move there.
  private String coordinatorBRegionOverride = "us-west-2";

  @Parameter(
      names = "--result_working_directory_path",
      description =
          "Path to a directory on the local filesystem to use as a working directory for writing"
              + " results before uploading to s3")
  private String resultWorkingDirectoryPath = "/";

  @Parameter(
      names = "--simulation_inputs",
      description =
          "Set to true if running the aggregation worker on input from"
              + " java.com.aggregate.simulation. Note this should only be done in a test"
              + " environment")
  private boolean simulationInputs = false;

  @Parameter(names = "--s3_endpoint_override", description = "Optional S3 Endpoint override URI")
  private String s3EndpointOverride = "";

  @Parameter(
      names = "--access_key",
      description =
          "Optional access key for AWS credentials. If this parameter (and --secret_key) is set,"
              + " the static credentials provider will be used.")
  private String accessKey = "";

  @Parameter(
      names = "--secret_key",
      description =
          "Optional secret key for AWS credentials.  If this parameter (and --access_key) is set,"
              + " the static credentials provider will be used.")
  private String secretKey = "";

  @Parameter(names = "--timer_exporter", description = "Selector for stopwatch timer exporter")
  private StopwatchExportSelector stopwatchExportSelector = StopwatchExportSelector.NO_OP;

  @Parameter(
      names = "--nonblocking_thread_pool_size",
      description = "Size of the non-blocking thread pool")
  // Changing this value would affect process thread pool size in ConcurrentAggregationProcessor.
  private int nonBlockingThreadPoolSize = Math.max(1, NUM_CPUS);

  @Parameter(
      names = "--blocking_thread_pool_size",
      description = "Size of the blocking thread pool")
  // Blocking thread is for I/O which is faster than non-IO operation in aggregation service.
  // Therefore, the thread pool size default is set to be smaller than nonBlockingThreadPool size.
  // Changing this value would affect read thread pool size in ConcurrentAggregationProcessor.
  private int blockingThreadPoolSize = Math.max(1, NUM_CPUS / 2);

  @Parameter(
      names = "--timer_exporter_file_path",
      description =
          "Path to the file to export stopwatches to (relevant if plain file exporter is used)")
  private String localPlainStopwatchFile = "";

  @Parameter(
      names = "--stopwatch_bucket_name",
      description = "S3 Bucket to write stopwatches to (relevant iff aws exporter is used)")
  private String stopwatchBucketName = "";

  @Parameter(
      names = "--stopwatch_key_name",
      description = "Stopwatch key name for S3 bucket (relevant if aws exporter is used)")
  private String stopwatchKeyName = "";

  @Parameter(names = "--benchmark", description = "Set to true to run in benchmark mode.")
  private boolean benchmark = false;

  @Parameter(
      names = "--coordinator_a_privacy_budgeting_endpoint",
      description = "Coordinator A's HTTP endpoint for privacy budgeting.")
  // TODO(b/218508112): Better default value
  private String coordinatorAPrivacyBudgetingEndpoint = "https://foo.com/v1";

  @Parameter(
      names = "--coordinator_a_privacy_budget_service_auth_endpoint",
      description = "Coordinator A's Auth endpoint for privacy budgeting service.")
  // TODO(b/218508112): Better default value
  private String coordinatorAPrivacyBudgetServiceAuthEndpoint = "https://foo.com/auth";

  @Parameter(
      names = "--coordinator_b_privacy_budgeting_endpoint",
      description = "Coordinator B's HTTP endpoint for privacy budgeting.")
  // TODO(b/218508112): Better default value
  private String coordinatorBPrivacyBudgetingEndpoint = "https://bar.com/v1";

  @Parameter(
      names = "--coordinator_b_privacy_budget_service_auth_endpoint",
      description = "Coordinator B's Auth endpoint for privacy budgeting service.")
  // TODO(b/218508112): Better default value
  private String coordinatorBPrivacyBudgetServiceAuthEndpoint = "https://bar.com/auth";

  @Parameter(names = "--noising_distribution", description = "Distribution to use for noising.")
  private Distribution noisingDistribution = Distribution.LAPLACE;

  @Parameter(names = "--noising_epsilon", description = "Epsilon value for noising.")
  private double noisingEpsilon = 10;

  @Parameter(names = "--noising_l1_sensitivity", description = "L1 sensitivity for noising.")
  private long noisingL1Sensitivity = 65536; // 2^16

  @Parameter(names = "--noising_delta", description = "Delta value for noising.")
  private double noisingDelta = 1e-5;

  @Parameter(
      names = "--domain_optional",
      description =
          "If set, option to threshold when output domain is not provided is enabled. This feature"
              + " is currently not enabled for use in Aggregation Service jobs.")
  private boolean domainOptional = false;

  @Parameter(names = "--domain_file_format", description = "Format of the domain generation file.")
  private DomainFormatSelector domainFileFormat = DomainFormatSelector.AVRO;

  @Parameter(
      names = "--debug_run",
      description = "If set, the service will generate summary and debug results")
  private boolean debugRun = false;

  @Parameter(names = "--otel_exporter", description = "Otel exporter implementation.")
  private OTelExporterSelector oTelExporterSelector = OTelExporterSelector.GRPC;

  @Parameter(
      names = "--grpc_collector_endpoint",
      description =
          "Endpoint for GRPC OTel collector. Format: {protocol}://{host}:{port}, eg."
              + " http://localhost:4317")
  private String grpcCollectorEndpoint = "http://localhost:4317";

  @Parameter(
      names = "--return_stack_trace",
      description =
          "Flag to allow stackTrace to be added to the resultInfo if there are any exceptions.")
  private boolean enableReturningStackTraceInResponse = false;

  @Parameter(
      names = "--max_depth_of_stack_trace",
      description =
          "Maximum depth of stack trace to return in response. The return_stack_trace flag needs to"
              + " be enabled for this to take effect.")
  private int maximumDepthOfStackTrace = 3;

  @Parameter(
      names = "--report_error_threshold_percentage",
      description =
          "The percentage of total input reports, if excluded from aggregation due to an"
              + " error, will fail the job. This can be overridden in job request.")
  private double reportErrorThresholdPercentage = 10.0;

  @Parameter(
      names = "--output_shard_file_size_bytes",
      description =
          "Size of one shard of the output file. The default value is 100,000,000. (100MB)")
  private long outputShardFileSizeBytes = 100_000_000L; // 100MB

  @Parameter(
      names = "--parallel_summary_upload_enabled",
      description = "Flag to enable parallel upload of the sharded summary reports.")
  private boolean parallelSummaryUploadEnabled = false;

  @Parameter(
      names = "--decrypter_cache_entry_ttl_sec",
      description =
          "Flag to set the private key cache time to live. Flag exposed for testing only.")
  private long decrypterCacheEntryTtlSec = 28800; // 8 hours.

  @Parameter(
      names = "--exception_cache_entry_ttl_sec",
      description = "Flag to set the exception cache time to live.")
  private long exceptionCacheEntryTtlSec = 10; // 10 seconds.

  @Parameter(
      names = "--streaming_output_domain_processing_enabled",
      description = "Flag to enable RxJava streaming based output domain processing.")
  private boolean streamingOutputDomainProcessingEnabled = false;

  @Parameter(
      names = "--labeled_privacy_budget_keys_enabled",
      description =
          "Flag to allow filtering of labeled payload contributions. If enabled, only contributions"
              + " corresponding to queried labels/ids are included in aggregation.")
  private boolean labeledPrivacyBudgetKeysEnabled = false;

  @Parameter(
      names = "--local_job_params_input_filtering_ids",
      description =
          "Filtering Id to be added in Job Params to filter the labeled payload contributions. To"
              + " be used only in Local mode.")
  private String filteringIds = null;

  @Parameter(
      names = "--attribution_reporting_debug_api_enabled",
      description = "Flag to enable support for Attribution Reporting Debug API.")
  private boolean attributionReportingDebugApiEnabled = true;

  @Parameter(
      names = "--parallel_fact_noising_enabled",
      description = "Flag to enable parallel aggregated fact noising.")
  private boolean parallelAggregatedFactNoisingEnabled = false;

  ClientConfigSelector getClientConfigSelector() {
    return clientConfigSelector;
  }

  JobClientSelector getJobClient() {
    return jobClient;
  }

  ParameterClientSelector getParamClient() {
    return paramClient;
  }

  LifecycleClientSelector getLifecycleClient() {
    return lifecycleClient;
  }

  MetricClientSelector getMetricClient() {
    return metricClient;
  }

  RecordReaderSelector getEncryptedRecordReader() {
    return recordReader;
  }

  String getScaleInHook() {
    return scaleInHook;
  }

  ResultLoggerModuleSelector resultLoggerModuleSelector() {
    return resultLoggerModuleSelector;
  }

  String getLocalFileSinglePullerPath() {
    return localFileSinglePullerPath;
  }

  String getLocalOutputDomainPath() {
    return localOutputDomainPath;
  }

  String getLocalFileJobInfoPath() {
    return localFileJobInfoPath;
  }

  String getAwsSqsQueueUrl() {
    return awsSqsQueueUrl;
  }

  String getMaxJobNumAttempts() {
    return maxJobNumAttempts;
  }

  String getMessageVisibilityTimeoutSeconds() {
    return messageVisibilityTimeoutSeconds;
  }

  String getAwsMetadatadbTableName() {
    return awsMetadatadbTableName;
  }

  public URI getCloudwatchEndpointOverride() {
    return URI.create(cloudwatchEndpointOverride);
  }

  public URI getDdbEndpointOverride() {
    return URI.create(ddbEndpointOverride);
  }

  public String getAwsMetadataEndpointOverride() {
    return awsMetadataEndpointOverride;
  }

  public URI getEc2EndpointOverride() {
    return URI.create(ec2EndpointOverride);
  }

  public URI getAutoScalingEndpointOverride() {
    return URI.create(autoScalingEndpointOverride);
  }

  public URI getSqsEndpointOverride() {
    return URI.create(sqsEndpointOverride);
  }

  public URI getSsmEndpointOverride() {
    return URI.create(ssmEndpointOverride);
  }

  public URI getStsEndpointOverride() {
    return URI.create(stsEndpointOverride);
  }

  public URI getKmsEndpointOverride() {
    return URI.create(kmsEndpointOverride);
  }

  public String getKmsSymmetricKey() {
    return kmsSymmetricKey;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  DecryptionModuleSelector getDecryptionModuleSelector() {
    return decryptionModuleSelector;
  }

  PrivacyBudgetingSelector getPrivacyBudgeting() {
    return privacyBudgeting;
  }

  NoisingSelector getNoisingSelector() {
    return noisingSelector;
  }

  public DecryptionKeyClientSelector getDecryptionServiceSelector() {
    return decryptionKeyServiceSelector;
  }

  String getLocalFileDecryptionKeyPath() {
    return localFileDecryptionKeyPath;
  }

  String getCoordinatorAEncryptionKeyServiceBaseUrl() {
    return coordinatorAEncryptionKeyServiceBaseUrl;
  }

  String getCoordinatorBEncryptionKeyServiceBaseUrl() {
    return coordinatorBEncryptionKeyServiceBaseUrl;
  }

  String getCoordinatorARoleArn() {
    return coordinatorARoleArn;
  }

  String getCoordinatorBRoleArn() {
    return coordinatorBRoleArn;
  }

  String getAdtechRegionOverride() {
    return adtechRegionOverride;
  }

  String getCoordinatorARegionOverride() {
    return coordinatorARegionOverride;
  }

  String getCoordinatorBRegionOverride() {
    return coordinatorBRegionOverride;
  }

  String getResultWorkingDirectoryPathString() {
    return resultWorkingDirectoryPath;
  }

  public boolean isSimulationInputs() {
    return simulationInputs;
  }

  BlobStorageClientSelector getBlobStorageClientSelector() {
    return blobStorageClientSelector;
  }

  public URI getS3EndpointOverride() {
    return URI.create(s3EndpointOverride);
  }

  public StopwatchExportSelector getStopwatchExportSelector() {
    return stopwatchExportSelector;
  }

  public String getLocalPlainStopwatchFile() {
    return localPlainStopwatchFile;
  }

  public String getStopwatchBucketName() {
    return stopwatchBucketName;
  }

  public String getStopwatchKeyName() {
    return stopwatchKeyName;
  }

  public boolean getBenchmarkMode() {
    return benchmark;
  }

  public String getCoordinatorAPrivacyBudgetingEndpoint() {
    return coordinatorAPrivacyBudgetingEndpoint;
  }

  public String getCoordinatorAPrivacyBudgetServiceAuthEndpoint() {
    return coordinatorAPrivacyBudgetServiceAuthEndpoint;
  }

  public String getCoordinatorBPrivacyBudgetingEndpoint() {
    return coordinatorBPrivacyBudgetingEndpoint;
  }

  public String getCoordinatorBPrivacyBudgetServiceAuthEndpoint() {
    return coordinatorBPrivacyBudgetServiceAuthEndpoint;
  }

  public int getNonBlockingThreadPoolSize() {
    return nonBlockingThreadPoolSize;
  }

  public int getBlockingThreadPoolSize() {
    return blockingThreadPoolSize;
  }

  public Distribution getNoisingDistribution() {
    return noisingDistribution;
  }

  public double getNoisingEpsilon() {
    return noisingEpsilon;
  }

  public long getNoisingL1Sensitivity() {
    return noisingL1Sensitivity;
  }

  public double getNoisingDelta() {
    return noisingDelta;
  }

  public boolean isDomainOptional() {
    return domainOptional;
  }

  public boolean isEnableThresholding() {
    // Enable thresholding when domain input is optional, i.e. all report keys are considered for
    // aggregated result.
    return domainOptional;
  }

  public DomainFormatSelector getDomainFileFormat() {
    return domainFileFormat;
  }

  public boolean isDebugRun() {
    return debugRun;
  }

  OTelExporterSelector getOTelExporterSelector() {
    return oTelExporterSelector;
  }

  String getGrpcCollectorEndpoint() {
    return grpcCollectorEndpoint;
  }

  public boolean isEnableReturningStackTraceInResponse() {
    return enableReturningStackTraceInResponse;
  }

  double getReportErrorThresholdPercentage() {
    return reportErrorThresholdPercentage;
  }

  public int getMaximumDepthOfStackTrace() {
    return maximumDepthOfStackTrace;
  }

  public long getOutputShardFileSizeBytes() {
    return outputShardFileSizeBytes;
  }

  public boolean isParallelSummaryUploadEnabled() {
    return parallelSummaryUploadEnabled;
  }

  public long getDecrypterCacheEntryTtlSec() {
    return decrypterCacheEntryTtlSec;
  }

  public long getExceptionCacheEntryTtlSec() {
    return exceptionCacheEntryTtlSec;
  }

  public boolean isStreamingOutputDomainProcessingEnabled() {
    return streamingOutputDomainProcessingEnabled;
  }

  boolean isLabeledPrivacyBudgetKeysEnabled() {
    return labeledPrivacyBudgetKeysEnabled;
  }

  String getFilteringIds() {
    return filteringIds;
  }

  boolean isAttributionReportingDebugApiEnabled() {
    return attributionReportingDebugApiEnabled;
  }

  public boolean isParallelAggregatedFactNoisingEnabled() {
    return parallelAggregatedFactNoisingEnabled;
  }
}
