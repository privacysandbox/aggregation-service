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

package com.google.aggregate.adtech.worker.aggregation.privacy;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetRequest;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetResponse;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient.DistributedPrivacyBudgetClientException;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient.DistributedPrivacyBudgetServiceException;
import java.util.Optional;
import javax.inject.Inject;

/** HTTP privacy budgeting bridge which consumes privacy budget from an external HTTP service. */
public final class HttpPrivacyBudgetingServiceBridge implements PrivacyBudgetingServiceBridge {

  public static final int DEFAULT_PRIVACY_BUDGET_LIMIT = 1;

  private final DistributedPrivacyBudgetClient distributedPrivacyBudgetClient;

  @Inject
  public HttpPrivacyBudgetingServiceBridge(
      DistributedPrivacyBudgetClient distributedPrivacyBudgetClient) {
    this.distributedPrivacyBudgetClient = distributedPrivacyBudgetClient;
  }

  @Override
  public ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
      ImmutableList<PrivacyBudgetUnit> budgetsToConsume,
      String attributionReportTo,
      Optional<Integer> debugPrivacyBudgetLimit)
      throws PrivacyBudgetingServiceBridgeException {
    ConsumePrivacyBudgetRequest consumePrivacyBudgetRequest =
        ConsumePrivacyBudgetRequest.builder()
            .attributionReportTo(attributionReportTo)
            .privacyBudgetUnits(
                budgetsToConsume.stream()
                    .map(HttpPrivacyBudgetingServiceBridge::scpBudgetUnit)
                    .collect(toImmutableList()))
            .privacyBudgetLimit(debugPrivacyBudgetLimit.orElse(DEFAULT_PRIVACY_BUDGET_LIMIT))
            .build();

    try {
      ConsumePrivacyBudgetResponse budgetResponse =
          distributedPrivacyBudgetClient.consumePrivacyBudget(consumePrivacyBudgetRequest);
      return budgetResponse.exhaustedPrivacyBudgetUnits().stream()
          .map(HttpPrivacyBudgetingServiceBridge::workerBudgetUnit)
          .collect(toImmutableList());
    } catch (DistributedPrivacyBudgetClientException | DistributedPrivacyBudgetServiceException e) {
      throw new PrivacyBudgetingServiceBridgeException(e);
    }
  }

  /** Converts worker's privacy budget unit ID to coordinator's representation */
  private static com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit scpBudgetUnit(
      PrivacyBudgetUnit budgetUnit) {
    return com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
        .privacyBudgetKey(budgetUnit.privacyBudgetKey())
        .reportingWindow(budgetUnit.scheduledReportTime())
        .build();
  }

  /** Converts coordinator's privacy budget unit ID to worker's representation */
  private static PrivacyBudgetUnit workerBudgetUnit(
      com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit budgetUnit) {
    return PrivacyBudgetUnit.create(budgetUnit.privacyBudgetKey(), budgetUnit.reportingWindow());
  }
}
