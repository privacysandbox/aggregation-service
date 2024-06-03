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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakePrivacyBudgetingServiceBridgeTest {

  private FakePrivacyBudgetingServiceBridge privacyBudgetingService;

  private String attributionReportTo = "https://foo.com";

  private final PrivacyBudgetUnit firstId =
      PrivacyBudgetUnit.create("foo", Instant.ofEpochMilli(1000), attributionReportTo);
  private final PrivacyBudgetUnit secondId =
      PrivacyBudgetUnit.create("foo", Instant.ofEpochMilli(2000), attributionReportTo);

  @Before
  public void setUp() {
    privacyBudgetingService = new FakePrivacyBudgetingServiceBridge();
  }

  @Test
  public void noBudget() throws Exception {
    // No setup, no budget given.

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId), attributionReportTo);

    assertThat(missingBudget).containsExactly(firstId);
    assertThat(privacyBudgetingService.getLastAttributionReportToSent())
        .hasValue(attributionReportTo);
  }

  @Test
  public void oneBudgetMissing() throws Exception {
    privacyBudgetingService.setPrivacyBudget(firstId, 5);

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);

    assertThat(missingBudget).containsExactly(secondId);
    assertThat(privacyBudgetingService.getLastAttributionReportToSent())
        .hasValue(attributionReportTo);
  }

  @Test
  public void success() throws Exception {
    privacyBudgetingService.setPrivacyBudget(firstId, 5);
    privacyBudgetingService.setPrivacyBudget(secondId, 15);

    ImmutableList<PrivacyBudgetUnit> missingBudget =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);

    assertThat(missingBudget).isEmpty();
    assertThat(privacyBudgetingService.getLastAttributionReportToSent())
        .hasValue(attributionReportTo);
  }

  @Test
  public void budgetDepleted() throws Exception {
    privacyBudgetingService.setPrivacyBudget(firstId, 1);
    privacyBudgetingService.setPrivacyBudget(secondId, 15);

    ImmutableList<PrivacyBudgetUnit> missingBudgetFirst =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);
    ImmutableList<PrivacyBudgetUnit> missingBudgetSecond =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);

    assertThat(missingBudgetFirst).isEmpty();
    assertThat(missingBudgetSecond).containsExactly(firstId);
  }

  @Test
  public void budgetRestored() throws Exception {
    privacyBudgetingService.setPrivacyBudget(firstId, 1);
    privacyBudgetingService.setPrivacyBudget(secondId, 15);

    ImmutableList<PrivacyBudgetUnit> missingBudgetFirst =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);
    privacyBudgetingService.setPrivacyBudget(firstId, 1);
    ImmutableList<PrivacyBudgetUnit> missingBudgetSecond =
        privacyBudgetingService.consumePrivacyBudget(
            ImmutableList.of(firstId, secondId), attributionReportTo);

    assertThat(missingBudgetFirst).isEmpty();
    assertThat(missingBudgetFirst).isEmpty();
  }
}
