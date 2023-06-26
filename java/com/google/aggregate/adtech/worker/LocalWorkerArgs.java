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

/** Command line args for the standalone library */
public class LocalWorkerArgs {

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

  public int getMaximumDepthOfStackTrace() {
    return maximumDepthOfStackTrace;
  }

  double getReportErrorThresholdPercentage() {
    return reportErrorThresholdPercentage;
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
