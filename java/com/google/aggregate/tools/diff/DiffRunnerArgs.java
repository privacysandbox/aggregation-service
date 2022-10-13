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

package com.google.aggregate.tools.diff;

import com.beust.jcommander.Parameter;

final class DiffRunnerArgs {

  @Parameter(names = "--test_input", required = true)
  private String testInput;

  @Parameter(names = "--test_key", required = true)
  private String testKey;

  @Parameter(names = "--test_golden", required = true)
  private String testGolden;

  @Parameter(
      names = "--domain_optional",
      description = "If set, option to threshold when output domain is not provided is enabled.")
  private boolean domainOptional = false;

  @Parameter(
      names = "--test_output_domain_dir",
      description =
          "Used only when domain_optional is true. The directory for storing outout domain"
              + " input to the worker.")
  private String testOutputDomainDir = "";

  @Parameter(
      names = "--test_dir",
      description = "The directory for storing outputs created by worker.")
  private String testDir = "/tmp";

  @Parameter(names = "--update_golden", description = "Override the golden")
  private boolean updateGolden = false;

  @Parameter(names = "--noising_epsilon", description = "Epsilon value for noising.")
  private double noisingEpsilon = 10;

  @Parameter(names = "--noising_l1_sensitivity", description = "L1 sensitivity for noising.")
  private long noisingL1Sensitivity = 65536;

  public String getTestInput() {
    return testInput;
  }

  public String getTestKey() {
    return testKey;
  }

  public String getTestGolden() {
    return testGolden;
  }

  public boolean isDomainOptional() {
    return domainOptional;
  }

  public String getTestOutputDomainDir() {
    return testOutputDomainDir;
  }

  public boolean isUpdateGolden() {
    return updateGolden;
  }

  public String getTestDir() {
    return testDir;
  }

  public double getNoisingEpsilon() {
    return noisingEpsilon;
  }

  public long getNoisingL1Sensitivity() {
    return noisingL1Sensitivity;
  }
}
