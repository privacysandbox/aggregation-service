/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.tools.privacybudgetutil.common;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This tool is for generating lists of privacy budget units in an adtech environment
 * and this config controls what is extracted.
 * Given the correct credentials the lists can be generated with:
 * bazel run //java/com/google/aggregate/tools/privacybudgetunit/extraction/(aws:AwsPrivacyBudgetUnitExtraction | gcp:GcpPrivacyBudgetUnitExtraction) \
 * (generate_keys | write_keys) \
 * --bucket <bucket> \
 * --input_prefix <input_prefix>
 */
public final class PrivacyBudgetUnitExtractionConfig {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PrivacyBudgetUnitExtractionConfig.class);

  @Parameter(description = "The main function of the tool, generate_keys or write_keys.")
  private String function = "generate_keys";

  @Parameter(names = "--help", description = "Prints usage.", help = true)
  private boolean help = false;

  @Parameter(names = "--bucket", description = "Bucket where the avro files reside.")
  private String bucket;

  @Parameter(names = "--input_prefix", description = "Prefix for file selection.")
  private String inputPrefix;

  @Parameter(
      names = "--dry_run",
      description = "Run in dry run mode. This will not write any files.")
  private boolean dryRun = false;

  @Parameter(
      names = "--output_prefix",
      description = "The output file prefix to write the file to.")
  private String outputPrefix = "shared_ids";

  @Parameter(names = "--single_file", description = "Create a single file for the output.")
  private boolean singleFile = true;

  @Parameter(
      names = "--filtering_ids",
      description =
          "The filtering IDs to use. This option can be used multiple times, and may be"
              + " comma-separated.",
      converter = StringToUnsignedLongConverter.class)
  private List<UnsignedLong> filteringIds = ImmutableList.of(UnsignedLong.ZERO);

  private String groups;
  private final JCommander commander;
  private final GCPCommands gcp;
  private final AWSCommands aws;

  public PrivacyBudgetUnitExtractionConfig(CloudPlatform cloudPlatform, String[] args) {
    aws = new AWSCommands();
    gcp = new GCPCommands();
    JCommander.Builder builder = JCommander.newBuilder().addObject(this);
    if (cloudPlatform == CloudPlatform.AWS) {
      builder.addObject(aws);
    } else {
      builder.addObject(gcp);
    }
    commander = builder.build();
    commander.parse(args);
  }

  public boolean printHelp() {
    if (help) {
      commander.usage();
    }
    return this.help;
  }

  public String getBucket() {
    return this.bucket;
  }

  public String getRegion() {
    return this.aws.region;
  }

  public String getFunction() {
    return this.function;
  }

  public String getInputPrefix() {
    return this.inputPrefix;
  }

  public Boolean getDryRun() {
    return this.dryRun;
  }

  public String getOutputPrefix() {
    return this.outputPrefix;
  }

  public Boolean getSingleFile() {
    return this.singleFile;
  }

  public String getProjectId() {
    return this.gcp.projectId;
  }

  public List<UnsignedLong> getFilteringIds() {
    return this.filteringIds;
  }

  private class AWSCommands {

    @Parameter(names = "--region", description = "Region for the request")
    private String region = "us-east-1";
  }

  private class GCPCommands {

    @Parameter(names = "--project_id", description = "GCP project Id.")
    private String projectId;
  }

  public enum CloudPlatform {
    GCP,
    AWS
  }

  public static class StringToUnsignedLongConverter implements IStringConverter<UnsignedLong> {
    @Override
    public UnsignedLong convert(String value) {
      return UnsignedLong.valueOf(value);
    }
  }
}
