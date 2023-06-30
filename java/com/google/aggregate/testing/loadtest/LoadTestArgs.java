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

package com.google.aggregate.testing.loadtest;

import com.beust.jcommander.Parameter;
import software.amazon.awssdk.regions.Region;

public final class LoadTestArgs {

  @Parameter(names = "--coordinator_a_assume_role_arn")
  private String coordinatorARoleArn = "";

  @Parameter(names = "--coordinator_b_assume_role_arn")
  private String coordinatorBRoleArn = "";

  @Parameter(
      names = "--coordinator_a_privacy_budgeting_endpoint",
      description = "Coordinator A's HTTP endpoint for privacy budgeting.")
  private String coordinatorAPrivacyBudgetingEndpoint = "";

  @Parameter(
      names = "--coordinator_b_privacy_budgeting_endpoint",
      description = "Coordinator B's HTTP endpoint for privacy budgeting.")
  private String coordinatorBPrivacyBudgetingEndpoint = "";

  @Parameter(
      names = "--coordinator_a_privacy_budget_service_auth_endpoint",
      description = "Coordinator A's Auth endpoint for privacy budgeting service.")
  private String coordinatorAPrivacyBudgetServiceAuthEndpoint = "";

  @Parameter(
      names = "--coordinator_b_privacy_budget_service_auth_endpoint",
      description = "Coordinator B's Auth endpoint for privacy budgeting service.")
  private String coordinatorBPrivacyBudgetServiceAuthEndpoint = "";

  @Parameter(
      names = "--coordinator_a_region",
      description = "The region of coordinator A services.")
  private String coordinatorARegion = Region.US_EAST_1.toString();

  @Parameter(
      names = "--coordinator_b_region",
      description = "The region of coordinator B services.")
  private String coordinatorBRegion = Region.US_EAST_1.toString();

  @Parameter(
      names = "--reporting_origin",
      description =
          "The reporting origin to be used when sending requests to Privacy budgeting service")
  private String reportingOrigin = "foo.com";

  @Parameter(
      names = "--num_pbs_keys_per_transaction",
      description = "The number of privacy budget keys to be consumed in every transaction.")
  private int numPbsKeysPerTransaction = 20;

  @Parameter(
      names = "--num_parallel_transactions_per_second",
      description = "The number of privacy budget keys to be consumed in every transaction.")
  private int parallelTransactionsPerSecond = 50;

  @Parameter(
      names = "--task_duration_mins",
      description = "The duration in minutes for which the load generation should happen.")
  private int taskDurationMins = 15;

  public int getNumPbsKeysPerTransaction() {
    return numPbsKeysPerTransaction;
  }

  public int getParallelTransactionsPerSecond() {
    return parallelTransactionsPerSecond;
  }

  public String getCoordinatorARoleArn() {
    return coordinatorARoleArn;
  }

  public String getCoordinatorBRoleArn() {
    return coordinatorBRoleArn;
  }

  public String getCoordinatorAPrivacyBudgetingEndpoint() {
    return coordinatorAPrivacyBudgetingEndpoint;
  }

  public String getCoordinatorBPrivacyBudgetingEndpoint() {
    return coordinatorBPrivacyBudgetingEndpoint;
  }

  public String getCoordinatorAPrivacyBudgetServiceAuthEndpoint() {
    return coordinatorAPrivacyBudgetServiceAuthEndpoint;
  }

  public String getCoordinatorBPrivacyBudgetServiceAuthEndpoint() {
    return coordinatorBPrivacyBudgetServiceAuthEndpoint;
  }

  public String getCoordinatorARegion() {
    return coordinatorARegion;
  }

  public String getCoordinatorBRegion() {
    return coordinatorBRegion;
  }

  public String getReportingOrigin() {
    return reportingOrigin;
  }

  public int getTaskDurationMins() {
    return taskDurationMins;
  }
}
