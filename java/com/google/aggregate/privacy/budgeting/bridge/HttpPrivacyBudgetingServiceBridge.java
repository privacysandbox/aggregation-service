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

package com.google.aggregate.privacy.budgeting.bridge;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetRequest;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetResponse;
import com.google.scp.coordinator.privacy.budgeting.model.ReportingOriginToPrivacyBudgetUnits;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient.DistributedPrivacyBudgetClientException;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient.DistributedPrivacyBudgetServiceException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
      ImmutableList<PrivacyBudgetUnit> budgetsToConsume, String claimedIdentity)
      throws PrivacyBudgetingServiceBridgeException {
    Map<String, Set<com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit>>
        originToApiBudgetUnits = new LinkedHashMap<>();
    for (PrivacyBudgetUnit workerBudgetUnit : budgetsToConsume) {
      updateOriginToApiBudgetUnitsMap(workerBudgetUnit, originToApiBudgetUnits);
    }
    ImmutableList<ReportingOriginToPrivacyBudgetUnits> reportingOriginToPrivacyBudgetUnits =
        originToApiBudgetUnits.entrySet().stream()
            .map(
                entry ->
                    ReportingOriginToPrivacyBudgetUnits.builder()
                        .setReportingOrigin(entry.getKey())
                        .setPrivacyBudgetUnits(ImmutableList.copyOf(entry.getValue()))
                        .build())
            .collect(toImmutableList());
    ConsumePrivacyBudgetRequest consumePrivacyBudgetRequest =
        ConsumePrivacyBudgetRequest.builder()
            .reportingOriginToPrivacyBudgetUnitsList(reportingOriginToPrivacyBudgetUnits)
            .claimedIdentity(claimedIdentity)
            .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
            .build();

    try {
      ConsumePrivacyBudgetResponse budgetResponse =
          distributedPrivacyBudgetClient.consumePrivacyBudget(consumePrivacyBudgetRequest);
      return budgetResponse.exhaustedPrivacyBudgetUnitsByOrigin().stream()
          .flatMap(budgetUnitsByOrigin -> buildWorkerBudgetUnits(budgetUnitsByOrigin).stream())
          .collect(toImmutableList());
    } catch (DistributedPrivacyBudgetServiceException e) {
      throw new PrivacyBudgetingServiceBridgeException(e.getStatusCode(), e);
    } catch (DistributedPrivacyBudgetClientException e) {
      throw new PrivacyBudgetingServiceBridgeException(e.getMessage(), e);
    }
  }

  private void updateOriginToApiBudgetUnitsMap(
      PrivacyBudgetUnit workerBudgetUnit,
      Map<String, Set<com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit>>
          originToApiBudgetUnits) {
    String reportingOrigin = workerBudgetUnit.reportingOrigin();
    // The ordering does not matter from code logic point of view. It simply makes it easier to
    // assert on during unit tests.
    Set<com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit> apiBudgetUnits =
        originToApiBudgetUnits.getOrDefault(reportingOrigin, new LinkedHashSet<>());
    com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit apiBudgetUnit =
        com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
            .privacyBudgetKey(workerBudgetUnit.privacyBudgetKey())
            .reportingWindow(workerBudgetUnit.scheduledReportTime())
            .build();
    apiBudgetUnits.add(apiBudgetUnit);
    originToApiBudgetUnits.put(reportingOrigin, apiBudgetUnits);
  }

  /** Converts coordinator's privacy budget unit ID to worker's representation */
  private static ImmutableList<PrivacyBudgetUnit> buildWorkerBudgetUnits(
      ReportingOriginToPrivacyBudgetUnits reportingOriginToPrivacyBudgetUnits) {
    String reportingOrigin = reportingOriginToPrivacyBudgetUnits.reportingOrigin();
    return reportingOriginToPrivacyBudgetUnits.privacyBudgetUnits().stream()
        .map(
            apiBudgetUnit ->
                PrivacyBudgetUnit.create(
                    apiBudgetUnit.privacyBudgetKey(),
                    apiBudgetUnit.reportingWindow(),
                    reportingOrigin))
        .collect(toImmutableList());
  }
}
