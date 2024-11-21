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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Command line args for the standalone library.
 *
 * <p>To add a new worker arg: declare a new parameter in this class and its getter function, update
 * the {@link LocalWorkerModule} to inject it to the appropriate location, and set the param in the
 * BUILD rules.
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
public class LocalWorkerArgs {

  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();

  @Parameter(
      names = "--input_data_avro_file",
      order = 0,
      description = "Path to the local file which contains aggregate reports in avro format")
  private String inputDataAvroFile = "";

  @Parameter(
      names = "--domain_avro_file",
      order = 1,
      description = "Path to the local file which contains the pre-listed keys in avro format")
  private String domainAvroFile = "";

  @Parameter(
      names = "--output_directory",
      order = 2,
      description = "Path to the directory where the output would be written")
  private String outputDirectory = "";

  @Parameter(
      names = "--epsilon",
      order = 3,
      description = "Epsilon value for noise > 0 and <= 64",
      validateWith = EpsilonValidator.class)
  private double epsilon = 10;

  @Parameter(
      names = "--print_licenses",
      order = 4,
      description = "only prints licenses for all the dependencies.")
  private boolean printLicenses = false;

  @Parameter(names = "--help", order = 5, help = true, description = "list all the parameters.")
  private boolean help = false;

  @Parameter(names = "--no_noising", order = 6, description = "ignore noising and thresholding.")
  private boolean noNoising = false;

  @Parameter(names = "--json_output", order = 7, description = "output the result in json format.")
  private boolean jsonOutput = false;

  @Parameter(names = "--l1_sensitivity", description = "L1 sensitivity for noising.")
  private long l1Sensitivity = 65536;

  @Parameter(names = "--delta", description = "Delta value for noising.")
  private double delta = 1e-5;

  @Parameter(
      names = "--skip_domain",
      description = "If set, domain is optional and thresholding is not done.",
      hidden = true)
  private boolean skip_domain = false;

  @Parameter(
      names = "--debug_run",
      order = 8,
      description = "if set, the service will generate summary and debug results.")
  private boolean debugRun = false;

  @Parameter(
      names = "--domain_file_format",
      description = "Format of the domain generation file.",
      hidden = true)
  private DomainFormatSelector domainFileFormat = DomainFormatSelector.AVRO;

  @Parameter(
      names = "--return_stack_trace",
      description =
          "Flag to allow stackTrace to be added to the resultInfo if there are any exceptions.")
  private boolean enableReturningStackTraceInResponse = false;

  @Parameter(
      names = "--max_depth_of_stack_trace",
      description =
          "Maximum depth of stack trace for returning in response. The return_stack_trace flag"
              + " needs to be enabled for this to take effect.")
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
      names = "--streaming_output_domain_processing_enabled",
      description = "Flag to enable RxJava streaming based output domain processing.")
  private boolean streamingOutputDomainProcessingEnabled = false;

  @Parameter(
      names = "--local_job_params_input_filtering_ids",
      description =
          "Filtering Id to be added in Job Params to filter the labeled payload contributions.")
  private String filteringIds = null;

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
      names = "--nonblocking_thread_pool_size",
      description = "Size of the non-blocking thread pool")
  private int nonBlockingThreadPoolSize = Math.max(1, NUM_CPUS);

  @Parameter(
      names = "--blocking_thread_pool_size",
      description = "Size of the blocking thread pool")
  // Blocking thread is for I/O which is faster than non-IO operation in aggregation service.
  // Therefore, the thread pool size default is set to be smaller than nonBlockingThreadPool size.
  private int blockingThreadPoolSize = Math.max(1, NUM_CPUS / 2);

  @Parameter(
      names = "--parallel_fact_noising_enabled",
      description = "Flag to enable parallel aggregated fact noising.")
  private boolean parallelAggregatedFactNoisingEnabled = false;

  public String getInputDataAvroFile() {
    return inputDataAvroFile;
  }

  public String getOutputDirectory() {
    return outputDirectory;
  }

  public Long getL1Sensitivity() {
    return l1Sensitivity;
  }

  public Double getDelta() {
    return delta;
  }

  public Double getEpsilon() {
    return epsilon;
  }

  public boolean isNoNoising() {
    return noNoising;
  }

  public boolean isJsonOutput() {
    return jsonOutput;
  }

  public boolean isPrintLicenses() {
    return printLicenses;
  }

  public boolean isHelp() {
    return help;
  }

  public String getDomainAvroFile() {
    return domainAvroFile;
  }

  public DomainFormatSelector getDomainFileFormat() {
    return domainFileFormat;
  }

  public boolean isSkipDomain() {
    return skip_domain;
  }

  public boolean isDebugRun() {
    return debugRun;
  }

  public boolean isEnableReturningStackTraceInResponse() {
    return enableReturningStackTraceInResponse;
  }

  public long getOutputShardFileSizeBytes() {
    return outputShardFileSizeBytes;
  }

  public int getMaximumDepthOfStackTrace() {
    return maximumDepthOfStackTrace;
  }

  double getReportErrorThresholdPercentage() {
    return reportErrorThresholdPercentage;
  }

  public boolean isStreamingOutputDomainProcessingEnabled() {
    return streamingOutputDomainProcessingEnabled;
  }

  String getFilteringIds() {
    return filteringIds;
  }

  boolean isLabeledPrivacyBudgetKeysEnabled() {
    return labeledPrivacyBudgetKeysEnabled;
  }

  boolean isAttributionReportingDebugApiEnabled() {
    return attributionReportingDebugApiEnabled;
  }

  int getNonBlockingThreadPoolSize() {
    return nonBlockingThreadPoolSize;
  }

  int getBlockingThreadPoolSize() {
    return blockingThreadPoolSize;
  }

  public boolean isParallelAggregatedFactNoisingEnabled() {
    return parallelAggregatedFactNoisingEnabled;
  }

  public void validate() {
    if (inputDataAvroFile == null || inputDataAvroFile.isBlank()) {
      throw new ParameterException(
          String.format(
              "Required Parameter %s missing, should be a valid avro file path containing reports"
                  + " in batch format.",
              "--input_data_avro_file"));
    }
    if (!skip_domain && (domainAvroFile == null || domainAvroFile.isBlank())) {
      throw new ParameterException(
          String.format(
              "Required Parameter %s missing, should be a valid avro file path containing"
                  + " pre-defined keys.",
              "--domain_avro_file"));
    }
    if (outputDirectory == null || outputDirectory.isBlank()) {
      throw new ParameterException(
          String.format(
              "Required Parameter %s missing, should be a writeable directory for writing results.",
              "--output_directory"));
    }

    if (getNonBlockingThreadPoolSize() < 1) {
      throw new ParameterException(
          "NonBlockingThreadPoolSize must be >= 1. Provided value: "
              + getNonBlockingThreadPoolSize());
    }

    if (getBlockingThreadPoolSize() < 1) {
      throw new ParameterException(
          "BlockingThreadPoolSize must be >= 1. Provided value: " + getBlockingThreadPoolSize());
    }
  }

  public static class EpsilonValidator implements IParameterValidator {

    @Override
    public void validate(String param, String value) throws ParameterException {
      boolean inRange = false;
      try {
        Double epsilon = Double.parseDouble(value);
        inRange = epsilon.doubleValue() > 0d && epsilon.doubleValue() <= 64d;
      } catch (NumberFormatException e) {
        // not handling
      }
      if (!inRange) {
        throw new ParameterException(
            String.format(
                "Parameter %s should be a number > 0 and <= 64 (found %s)", param, value));
      }
    }
  }
}
