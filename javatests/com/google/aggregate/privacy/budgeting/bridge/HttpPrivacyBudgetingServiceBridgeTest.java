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

import static com.google.aggregate.privacy.budgeting.bridge.HttpPrivacyBudgetingServiceBridge.DEFAULT_PRIVACY_BUDGET_LIMIT;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetRequest;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetResponse;
import com.google.scp.coordinator.privacy.budgeting.model.ReportingOriginToPrivacyBudgetUnits;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient;
import java.time.Instant;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HttpPrivacyBudgetingServiceBridgeTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  private static final String CLAIMED_IDENTITY = "https://foo.com";

  private static final String REPORTING_ORIGIN_1 = "origin1.foo.com";
  private static final String REPORTING_ORIGIN_2 = "origin2.foo.com";

  private static final PrivacyBudgetUnit WORKER_FIRST_UNIT =
      PrivacyBudgetUnit.create("foo1", Instant.ofEpochMilli(1000), REPORTING_ORIGIN_1);
  private static final PrivacyBudgetUnit WORKER_SECOND_UNIT =
      PrivacyBudgetUnit.create("foo2", Instant.ofEpochMilli(2000), REPORTING_ORIGIN_1);

  private static final PrivacyBudgetUnit WORKER_THIRD_UNIT =
      PrivacyBudgetUnit.create("foo3", Instant.ofEpochMilli(3000), REPORTING_ORIGIN_2);

  private static final PrivacyBudgetUnit WORKER_FOURTH_UNIT =
      PrivacyBudgetUnit.create("foo4", Instant.ofEpochMilli(4000), REPORTING_ORIGIN_1);

  private static final com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit
      API_FIRST_UNIT =
          com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
              .privacyBudgetKey("foo1")
              .reportingWindow(Instant.ofEpochMilli(1000))
              .build();

  private static final com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit
      API_SECOND_UNIT =
          com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
              .privacyBudgetKey("foo2")
              .reportingWindow(Instant.ofEpochMilli(2000))
              .build();

  private static final com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit
      API_THIRD_UNIT =
          com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
              .privacyBudgetKey("foo3")
              .reportingWindow(Instant.ofEpochMilli(3000))
              .build();

  private static final com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit
      API_FOURTH_UNIT =
          com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
              .privacyBudgetKey("foo4")
              .reportingWindow(Instant.ofEpochMilli(4000))
              .build();

  private static final ReportingOriginToPrivacyBudgetUnits ORIGIN_1_UNITS =
      ReportingOriginToPrivacyBudgetUnits.builder()
          .setReportingOrigin(REPORTING_ORIGIN_1)
          .setPrivacyBudgetUnits(ImmutableList.of(API_FIRST_UNIT, API_SECOND_UNIT, API_FOURTH_UNIT))
          .build();

  private static final ReportingOriginToPrivacyBudgetUnits ORIGIN_2_UNITS =
      ReportingOriginToPrivacyBudgetUnits.builder()
          .setReportingOrigin(REPORTING_ORIGIN_2)
          .setPrivacyBudgetUnits(ImmutableList.of(API_THIRD_UNIT))
          .build();

  @Inject FakeHttpPrivacyBudgetingServiceClient fakeHttpPrivacyBudgetingServiceClient;

  // Under test
  @Inject HttpPrivacyBudgetingServiceBridge privacyBudgetingService;

  @Test
  public void noBudget() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(
        ImmutableList.of(workerToScpUnit(WORKER_FIRST_UNIT), workerToScpUnit(WORKER_SECOND_UNIT)));

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(
                WORKER_THIRD_UNIT, WORKER_FIRST_UNIT, WORKER_SECOND_UNIT, WORKER_FOURTH_UNIT),
            CLAIMED_IDENTITY);

    assertThat(missingBudget).containsExactly(WORKER_FIRST_UNIT, WORKER_SECOND_UNIT);
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .reportingOriginToPrivacyBudgetUnitsList(
                    ImmutableList.of(ORIGIN_2_UNITS, ORIGIN_1_UNITS))
                .claimedIdentity(CLAIMED_IDENTITY)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  @Test
  public void oneBudgetMissing() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(
        ImmutableList.of(workerToScpUnit(WORKER_FIRST_UNIT)));

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(WORKER_FIRST_UNIT, WORKER_SECOND_UNIT, WORKER_FOURTH_UNIT),
            CLAIMED_IDENTITY);

    assertThat(missingBudget).containsExactly(WORKER_FIRST_UNIT);
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .reportingOriginToPrivacyBudgetUnitsList(ImmutableList.of(ORIGIN_1_UNITS))
                .claimedIdentity(CLAIMED_IDENTITY)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  @Test
  public void success() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(ImmutableList.of());

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(
                WORKER_FIRST_UNIT, WORKER_SECOND_UNIT, WORKER_THIRD_UNIT, WORKER_FOURTH_UNIT),
            CLAIMED_IDENTITY);

    assertThat(missingBudget).isEmpty();
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .reportingOriginToPrivacyBudgetUnitsList(
                    ImmutableList.of(ORIGIN_1_UNITS, ORIGIN_2_UNITS))
                .claimedIdentity(CLAIMED_IDENTITY)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  private static com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit
      workerToScpUnit(PrivacyBudgetUnit unit) {
    return com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit.builder()
        .privacyBudgetKey(unit.privacyBudgetKey())
        .reportingWindow(unit.scheduledReportTime())
        .build();
  }

  private static final class FakeHttpPrivacyBudgetingServiceClient
      implements DistributedPrivacyBudgetClient {

    private ImmutableList<com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit>
        exhaustedUnits;
    private ConsumePrivacyBudgetRequest lastRequestSent;

    public void setExhaustedUnits(
        ImmutableList<com.google.scp.coordinator.privacy.budgeting.model.PrivacyBudgetUnit>
            exhaustedUnits) {
      this.exhaustedUnits = exhaustedUnits;
    }

    @Override
    public ConsumePrivacyBudgetResponse consumePrivacyBudget(ConsumePrivacyBudgetRequest request) {
      lastRequestSent = request;
      String reportingOrigin1 = "origin1.foo.com";
      return ConsumePrivacyBudgetResponse.builder()
          .exhaustedPrivacyBudgetUnitsByOrigin(
              ImmutableList.of(
                  ReportingOriginToPrivacyBudgetUnits.builder()
                      .setReportingOrigin(reportingOrigin1)
                      .setPrivacyBudgetUnits(exhaustedUnits)
                      .build()))
          .build();
    }
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeHttpPrivacyBudgetingServiceClient.class).in(TestScoped.class);
      bind(DistributedPrivacyBudgetClient.class).to(FakeHttpPrivacyBudgetingServiceClient.class);
    }
  }
}
