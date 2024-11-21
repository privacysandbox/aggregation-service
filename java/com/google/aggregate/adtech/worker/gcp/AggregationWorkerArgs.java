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

import com.beust.jcommander.Parameter;
import com.google.aggregate.adtech.worker.DecryptionModuleSelector;
import com.google.aggregate.adtech.worker.DomainFormatSelector;
import com.google.aggregate.adtech.worker.NoisingSelector;
import com.google.aggregate.adtech.worker.PrivacyBudgetingSelector;
import com.google.aggregate.adtech.worker.ResultLoggerModuleSelector;
import com.google.aggregate.adtech.worker.selector.BlobStorageClientSelector;
import com.google.aggregate.adtech.worker.selector.ClientConfigSelector;
import com.google.aggregate.adtech.worker.selector.DecryptionKeyClientSelector;
import com.google.aggregate.adtech.worker.selector.JobClientSelector;
import com.google.aggregate.adtech.worker.selector.LifecycleClientSelector;
import com.google.aggregate.adtech.worker.selector.MetricClientSelector;
import com.google.aggregate.adtech.worker.selector.ParameterClientSelector;
import com.google.aggregate.adtech.worker.selector.PrivacyBudgetClientSelector;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.common.annotations.Beta;
import com.google.privacysandbox.otel.OTelExporterSelector;
import java.util.Optional;

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
public class AggregationWorkerArgs {

  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();

  @Parameter(names = "--client_config_env", description = "Selects client config environment")
  private ClientConfigSelector clientConfigSelector = ClientConfigSelector.GCP;

  @Parameter(names = "--job_client", description = "Job handler client implementation")
  private JobClientSelector jobClient = JobClientSelector.LOCAL_FILE;

  @Parameter(names = "--blob_storage_client", description = "Data client implementation")
  private BlobStorageClientSelector blobStorageClientSelector =
      BlobStorageClientSelector.LOCAL_FS_CLIENT;

  @Parameter(names = "--decryption_key_service", description = "How to read the decryption keys")
  private DecryptionKeyClientSelector decryptionKeyClientSelector =
      DecryptionKeyClientSelector.LOCAL_FILE_DECRYPTION_KEY_SERVICE;

  @Parameter(names = "--lifecycle_client", description = "Lifecycle client implementation")
  private LifecycleClientSelector lifecycleClient = LifecycleClientSelector.LOCAL;

  @Parameter(names = "--metric_client", description = "Metric client implementation")
  private MetricClientSelector metricClient = MetricClientSelector.LOCAL;

  @Parameter(names = "--param_client", description = "Parameter client implementation")
  private ParameterClientSelector paramClient = ParameterClientSelector.GCP;

  @Parameter(names = "--result_logger", description = "How to log aggregation results")
  private ResultLoggerModuleSelector resultLoggerModuleSelector =
      ResultLoggerModuleSelector.LOCAL_TO_CLOUD;

  @Parameter(names = "--privacy_budgeting", description = "Implementation of privacy budgeting")
  private PrivacyBudgetingSelector privacyBudgeting = PrivacyBudgetingSelector.UNLIMITED;

  @Parameter(
      names = "--primary_encryption_key_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of the primary (Party A)"
              + " encryption key service base url service, used only for multi-party key hosting"
              + " service. Do not include trailing slash")
  private String primaryEncryptionKeyServiceBaseUrl = "";

  @Parameter(
      names = "--secondary_encryption_key_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of the secondary (Party B)"
              + " encryption key service base url service, used only for multi-party key hosting"
              + " service. Do not include trailing slash")
  private String secondaryEncryptionKeyServiceBaseUrl = "";

  @Parameter(
      names = "--primary_encryption_key_service_cloudfunction_url",
      description =
          "Full URL of the primary (Party A) encryption key service cloudfunction service, used"
              + " only as audience for GCP authentication. This is temporary and will be replaced"
              + " by encryption key service url in the future. ")
  private String primaryEncryptionKeyServiceCloudfunctionUrl = "";

  @Parameter(
      names = "--secondary_encryption_key_service_cloudfunction_url",
      description =
          "Full URL of the secondary (Party B) encryption key service cloudfunction service, used"
              + " only as audience for GCP authentication.This is temporary and will be replaced by"
              + " encryption key service url in the future. ")
  private String secondaryEncryptionKeyServiceCloudfunctionUrl = "";

  @Parameter(names = "--gcp_project_id", description = "Project ID. ")
  private String gcpProjectId = "";

  @Parameter(names = "--pubsub_topic_id", description = "GCP PubSub topic ID. ")
  private String pubSubTopicId = "aggregate-service-jobqueue";

  @Parameter(
      names = "--pubsub_endpoint",
      description =
          "GCP pubsub endpoint URL to override the default value. Empty value is ignored.")
  private String pubSubEndpoint = "";

  @Parameter(names = "--pubsub_subscription_id", description = "GCP PubSub subscription ID. ")
  private String pubSubSubscriptionId = "aggregate-service-jobqueue-sub";

  @Parameter(names = "--spanner_instance_id", description = "GCP Spanner instance ID. ")
  private String spannerInstanceId = "jobmetadatainstance";

  @Parameter(names = "--spanner_db_name", description = "GCP Spanner Database Name. ")
  private String spannerDbName = "jobmetadatadb";

  @Parameter(
      names = "--spanner_endpoint",
      description =
          "GCP Spanner endpoint URL to override the default value. Values that do not start with"
              + " \"https://\" are assumed to be emulators for testing. Empty value is ignored.")
  private String spannerEndpoint = "";

  @Parameter(
      names = "--gcs_endpoint",
      description = "GCS endpoint URL; defaults to using Production GCS if empty.")
  private String gcsEndpoint = "";

  @Parameter(
      names = "--coordinator_a_wip_provider",
      description = "Workload identity pool provider id. ")
  private String coordinatorAWipProvider = "";

  @Parameter(
      names = "--coordinator_a_sa",
      description = "Coordinator service account used for impersonation.")
  private String coordinatorAServiceAccount = "";

  @Parameter(
      names = "--coordinator_b_wip_provider",
      description = "Workload identity pool provider id. ")
  private String coordinatorBWipProvider = "";

  @Parameter(
      names = "--coordinator_b_sa",
      description = "Coordinator service account used for impersonation.")
  private String coordinatorBServiceAccount = "";

  @Parameter(names = "--coordinator_a_kms_key", description = "KMS SymmetricKey ARN.")
  private String coodinatorAKmsKey = "";

  @Parameter(names = "--coordinator_b_kms_key", description = "KMS SymmetricKey ARN.")
  private String coodinatorBKmsKey = "";

  @Parameter(
      names = "--result_working_directory_path",
      description =
          "Path to a directory on the local filesystem to use as a working directory for writing"
              + " results before uploading to s3")
  private String resultWorkingDirectoryPath = "";

  @Parameter(
      names = "--simulation_inputs",
      description =
          "Set to true if running the aggregation worker on input from"
              + " java.com.aggregate.simulation. Note this should only be done in a test"
              + " environment")
  private boolean simulationInputs = false;

  @Parameter(
      names = "--gcp_instance_id_override",
      description = "Optional instance id for gce instance in GCP. Only for metric client use.")
  private String gcpInstanceIdOverride = "";

  @Parameter(
      names = "--gcp_instance_name_override",
      description =
          "Optional instance name for gce instance in GCP. Only for lifecycle client use.")
  private String gcpInstanceNameOverride = "";

  @Parameter(
      names = "--gcp_zone_override",
      description = "Optional GCP zone for gce instance. Only for metric client use.")
  private String gcpZoneOverride = "";

  @Parameter(
      names = "--coordinator_a_privacy_budgeting_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of coordinator A's privacy"
              + " budgeting service. Do not include trailing slash")
  private String coordinatorAPrivacyBudgetingServiceUrl = null;

  @Parameter(
      names = "--coordinator_a_privacy_budgeting_service_auth_endpoint",
      description = "Auth endpoint of coordinator A's privacy budgeting service.")
  private String coordinatorAPrivacyBudgetingServiceAuthEndpoint = null;

  @Parameter(
      names = "--coordinator_b_privacy_budgeting_service_base_url",
      description =
          "Full URL (including protocol and api version path fragment) of coordinator B's privacy"
              + " budgeting service. Do not include trailing slash")
  private String coordinatorBPrivacyBudgetingServiceUrl = null;

  @Parameter(
      names = "--coordinator_b_privacy_budgeting_service_auth_endpoint",
      description = "Auth endpoint of coordinator B's privacy budgeting service.")
  private String coordinatorBPrivacyBudgetingServiceAuthEndpoint = null;

  @Parameter(names = "--pbs_client", description = "PBS client implementation")
  private PrivacyBudgetClientSelector pbsclient = PrivacyBudgetClientSelector.LOCAL;

  @Parameter(
      names = "--local_file_single_puller_path",
      description =
          "Path to the local file provided by the local file single puller(flag makes "
              + "sense only if that puller is used")
  private String localFileSinglePullerPath = "";

  @Parameter(
      names = "--local_file_job_info_path",
      description = "Path to the local file to dump the job info to in local mode (empty for none)")
  private String localFileJobInfoPath = "";

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
      description = "If set, option to threshold when output domain is not provided is enabled.")
  private boolean domainOptional = false;

  @Parameter(names = "--domain_file_format", description = "Format of the domain generation file.")
  private DomainFormatSelector domainFileFormat = DomainFormatSelector.AVRO;

  @Parameter(
      names = "--fail_job_on_pbs_errors",
      description =
          "If set, the jobs will fail when an error occurs reaching the PBS. Otherwise failures are"
              + " ignored")
  private boolean failJobOnPbsErrors = false;

  @Parameter(
      names = "--nonblocking_thread_pool_size",
      description = "Size of the non-blocking thread pool")
  private int nonBlockingThreadPoolSize = Math.max(1, NUM_CPUS);

  @Parameter(
      names = "--blocking_thread_pool_size",
      description = "Size of the blocking thread pool")
  // Blocking thread is for I/O which is faster than non-IO operation in aggregation service.
  // Therefore, the thread pool size default is set to be smaller than nonBlockingThreadPool size.
  private int blockingThreadPoolSize = Math.max(1, NUM_CPUS / 2);

  @Parameter(names = "--benchmark", description = "Set to true to run in benchmark mode.")
  private boolean benchmark = false;

  @Parameter(names = "--noising", description = "Noising implementation to use")
  private NoisingSelector noisingSelector = NoisingSelector.DP_NOISING;

  @Parameter(names = "--decryption", description = "Decryption implementation")
  private DecryptionModuleSelector decryptionModuleSelector = DecryptionModuleSelector.HYBRID;

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
      names = "--test_encoded_keyset_handle",
      description =
          "Optional base64 encoded string that represents the keyset handle to retrieve an Aead."
              + " This is for coordinatorA if multi-party.")
  private String testEncodedKeysetHandle = "";

  @Parameter(
      names = "--test_coordinator_b_encoded_keyset_handle",
      description =
          "Optional base64 encoded string that represents the keyset handle to retrieve an Aead for"
              + " coordinatorB.")
  private String testCoordinatorBEncodedKeysetHandle = "";

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
      names = "--attribution_reporting_debug_api_enabled",
      description = "Flag to enable support for Attribution Reporting Debug API.")
  private boolean attributionReportingDebugApiEnabled = true;

  @Parameter(
      names = "--parallel_fact_noising_enabled",
      description = "Flag to enable parallel aggregated fact noising.")
  private boolean parallelAggregatedFactNoisingEnabled = false;

  ResultLoggerModuleSelector resultLoggerModuleSelector() {
    return resultLoggerModuleSelector;
  }

  DecryptionModuleSelector getDecryptionModuleSelector() {
    return decryptionModuleSelector;
  }

  public String getGcpProjectId() {
    return gcpProjectId;
  }

  public String getGcpInstanceIdOverride() {
    return gcpInstanceIdOverride;
  }

  public String getGcpInstanceNameOverride() {
    return gcpInstanceNameOverride;
  }

  public String getGcpZoneOverride() {
    return gcpZoneOverride;
  }

  public String getPubSubTopicId() {
    return pubSubTopicId;
  }

  public String getPubSubSubscriptionId() {
    return pubSubSubscriptionId;
  }

  public String getSpannerInstanceId() {
    return spannerInstanceId;
  }

  public String getSpannerDbName() {
    return spannerDbName;
  }

  public Optional<String> getSpannerEndpoint() {
    return Optional.ofNullable(spannerEndpoint).filter(endpoint -> !endpoint.isEmpty());
  }

  public Optional<String> getPubSubEndpoint() {
    return Optional.ofNullable(pubSubEndpoint).filter(endpoint -> !endpoint.isEmpty());
  }

  public Optional<String> getGcsEndpoint() {
    return Optional.ofNullable(gcsEndpoint).filter(endpoint -> !endpoint.isEmpty());
  }

  public String getCoordinatorAWipProvider() {
    return coordinatorAWipProvider;
  }

  public String getCoordinatorAServiceAccount() {
    return coordinatorAServiceAccount;
  }

  public String getCoordinatorBWipProvider() {
    return coordinatorBWipProvider;
  }

  public String getCoordinatorBServiceAccount() {
    return coordinatorBServiceAccount;
  }

  public String getCoodinatorAKmsKey() {
    return coodinatorAKmsKey;
  }

  public String getCoodinatorBKmsKey() {
    return coodinatorBKmsKey;
  }

  String getPrimaryEncryptionKeyServiceBaseUrl() {
    return primaryEncryptionKeyServiceBaseUrl;
  }

  String getSecondaryEncryptionKeyServiceBaseUrl() {
    return secondaryEncryptionKeyServiceBaseUrl;
  }

  Optional<String> getPrimaryEncryptionKeyServiceCloudfunctionUrl() {
    return Optional.ofNullable(primaryEncryptionKeyServiceCloudfunctionUrl)
        .filter(id -> !id.isEmpty());
  }

  Optional<String> getSecondaryEncryptionKeyServiceCloudfunctionUrl() {
    return Optional.ofNullable(secondaryEncryptionKeyServiceCloudfunctionUrl)
        .filter(id -> !id.isEmpty());
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

  String getCoordinatorAPrivacyBudgetingServiceUrl() {
    return coordinatorAPrivacyBudgetingServiceUrl;
  }

  String getCoordinatorAPrivacyBudgetingServiceAuthEndpoint() {
    return coordinatorAPrivacyBudgetingServiceAuthEndpoint;
  }

  String getCoordinatorBPrivacyBudgetingServiceUrl() {
    return coordinatorBPrivacyBudgetingServiceUrl;
  }

  String getCoordinatorBPrivacyBudgetingServiceAuthEndpoint() {
    return coordinatorBPrivacyBudgetingServiceAuthEndpoint;
  }

  PrivacyBudgetClientSelector getPbsclientSelector() {
    return pbsclient;
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

  PrivacyBudgetingSelector getPrivacyBudgeting() {
    return privacyBudgeting;
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

  public DomainFormatSelector getDomainFileFormat() {
    return domainFileFormat;
  }

  public boolean isFailJobOnPbsErrors() {
    return failJobOnPbsErrors;
  }

  public boolean getBenchmarkMode() {
    return benchmark;
  }

  NoisingSelector getNoisingSelector() {
    return noisingSelector;
  }

  public LifecycleClientSelector getLifeCycleClientSelector() {
    return lifecycleClient;
  }

  public MetricClientSelector getMetricCycleClientSelector() {
    return metricClient;
  }

  public ClientConfigSelector getClientConfigSelector() {
    return clientConfigSelector;
  }

  public JobClientSelector getJobClientSelector() {
    return jobClient;
  }

  public ParameterClientSelector getParamClientSelector() {
    return paramClient;
  }

  public DecryptionKeyClientSelector getDecryptionKeyClientSelector() {
    return decryptionKeyClientSelector;
  }

  String getLocalFileSinglePullerPath() {
    return localFileSinglePullerPath;
  }

  String getLocalFileJobInfoPath() {
    return localFileJobInfoPath;
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

  @Beta
  public Optional<String> getTestEncodedKeysetHandle() {
    return Optional.ofNullable(testEncodedKeysetHandle).filter(s -> !s.isEmpty());
  }

  @Beta
  public Optional<String> getTestCoordinatorBEncodedKeysetHandle() {
    return Optional.ofNullable(testCoordinatorBEncodedKeysetHandle).filter(s -> !s.isEmpty());
  }

  public int getMaximumDepthOfStackTrace() {
    return maximumDepthOfStackTrace;
  }

  double getReportErrorThresholdPercentage() {
    return reportErrorThresholdPercentage;
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

  boolean isAttributionReportingDebugApiEnabled() {
    return attributionReportingDebugApiEnabled;
  }

  public boolean isParallelAggregatedFactNoisingEnabled() {
    return parallelAggregatedFactNoisingEnabled;
  }
}
