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

import static com.google.aggregate.adtech.worker.aggregation.privacy.HttpPrivacyBudgetingServiceBridge.DEFAULT_PRIVACY_BUDGET_LIMIT;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.aggregation.privacy.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetRequest;
import com.google.scp.coordinator.privacy.budgeting.model.ConsumePrivacyBudgetResponse;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HttpPrivacyBudgetingServiceBridgeTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private final PrivacyBudgetUnit firstId =
      PrivacyBudgetUnit.create("foo", Instant.ofEpochMilli(1000));
  private final PrivacyBudgetUnit secondId =
      PrivacyBudgetUnit.create("foo", Instant.ofEpochMilli(2000));
  private final String attributionReportTo = "foo.com";

  @Inject FakeHttpPrivacyBudgetingServiceClient fakeHttpPrivacyBudgetingServiceClient;

  // Under test
  @Inject HttpPrivacyBudgetingServiceBridge privacyBudgetingService;

  @Test
  public void noBudget() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(
        ImmutableList.of(workerToScpUnit(firstId), workerToScpUnit(secondId)));

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId),
            attributionReportTo,
            /* debugPrivacyBudgetLimit= */ Optional.empty());

    assertThat(missingBudget).containsExactly(firstId, secondId);
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .privacyBudgetUnits(
                    ImmutableList.of(workerToScpUnit(firstId), workerToScpUnit(secondId)))
                .attributionReportTo(attributionReportTo)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  @Test
  public void oneBudgetMissing() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(
        ImmutableList.of(workerToScpUnit(firstId)));

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId),
            attributionReportTo,
            /* debugPrivacyBudgetLimit= */ Optional.empty());

    assertThat(missingBudget).containsExactly(firstId);
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .privacyBudgetUnits(
                    ImmutableList.of(workerToScpUnit(firstId), workerToScpUnit(secondId)))
                .attributionReportTo(attributionReportTo)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  @Test
  public void success() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(ImmutableList.of());

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId),
            attributionReportTo,
            /* debugPrivacyBudgetLimit= */ Optional.empty());

    assertThat(missingBudget).isEmpty();
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .privacyBudgetUnits(
                    ImmutableList.of(workerToScpUnit(firstId), workerToScpUnit(secondId)))
                .attributionReportTo(attributionReportTo)
                .privacyBudgetLimit(DEFAULT_PRIVACY_BUDGET_LIMIT)
                .build());
  }

  @Test
  public void usesDebugLimitIfPresent() throws Exception {
    fakeHttpPrivacyBudgetingServiceClient.setExhaustedUnits(ImmutableList.of());
    Optional<Integer> debugPrivacyBudgetLimit = Optional.of(5);

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo, debugPrivacyBudgetLimit);

    assertThat(missingBudget).isEmpty();
    assertThat(fakeHttpPrivacyBudgetingServiceClient.lastRequestSent)
        .isEqualTo(
            ConsumePrivacyBudgetRequest.builder()
                .privacyBudgetUnits(
                    ImmutableList.of(workerToScpUnit(firstId), workerToScpUnit(secondId)))
                .attributionReportTo(attributionReportTo)
                .privacyBudgetLimit(debugPrivacyBudgetLimit.get())
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
    public ConsumePrivacyBudgetResponse consumePrivacyBudget(ConsumePrivacyBudgetRequest request)
        throws DistributedPrivacyBudgetServiceException {
      lastRequestSent = request;
      return ConsumePrivacyBudgetResponse.builder()
          .exhaustedPrivacyBudgetUnits(exhaustedUnits)
          .build();
    }

    public ConsumePrivacyBudgetRequest getLastRequestSent() {
      return lastRequestSent;
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
