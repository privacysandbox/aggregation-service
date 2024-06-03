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
import java.util.HashMap;
import java.util.Optional;

/**
 * Fake privacy budgeting service bridge to be used strictly in tests
 *
 * <p>NOTE: This implementation is **NOT** thread-safe.
 *
 * <p>This bridge supports programmatically setting the budget.
 */
public final class FakePrivacyBudgetingServiceBridge implements PrivacyBudgetingServiceBridge {
  private PrivacyBudgetingServiceBridgeException exception;
  private Optional<ImmutableList<PrivacyBudgetUnit>> lastBudgetsToConsumeSent = Optional.empty();
  private Optional<String> lastAttributionReportToSent = Optional.empty();

  private final HashMap<PrivacyBudgetUnit, Integer> privacyBudgets;

  public FakePrivacyBudgetingServiceBridge() {
    privacyBudgets = new HashMap<>();
  }

  public void setPrivacyBudget(PrivacyBudgetUnit budgetId, int budget) {
    privacyBudgets.put(budgetId, budget);
  }

  /**
   * If {@link #setShouldThrow} is called for at least once, {@link #consumePrivacyBudget} will
   * throw a default exception when the method is called. If you need to override the exception that
   * is being thrown by {@link #consumePrivacyBudget}, please use {@link #setException} instead.
   */
  public void setShouldThrow() {
    Exception e = new Exception("encountered fake privacy budget exception.");
    setException(new PrivacyBudgetingServiceBridgeException(e.getMessage(), e));
  }

  /**
   * If {@link #setException} method is called for at least once, {@link #consumePrivacyBudget} will
   * throw the given exception when the method is called.
   */
  public void setException(PrivacyBudgetingServiceBridgeException e) {
    this.exception = e;
  }

  @Override
  public ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
      ImmutableList<PrivacyBudgetUnit> budgetsToConsume, String claimedIdentity)
      throws PrivacyBudgetingServiceBridgeException {
    if (exception != null) {
      throw exception;
    }

    lastBudgetsToConsumeSent = Optional.of(budgetsToConsume);
    lastAttributionReportToSent = Optional.of(claimedIdentity);

    ImmutableList<PrivacyBudgetUnit> insufficientPrivacyBudgetUnits =
        budgetsToConsume.stream()
            .filter(budgetId -> privacyBudgets.getOrDefault(budgetId, 0) < 1)
            .collect(toImmutableList());

    if (!insufficientPrivacyBudgetUnits.isEmpty()) {
      return insufficientPrivacyBudgetUnits;
    }

    // Reduces the budget by 1 for each key.
    budgetsToConsume.stream()
        .forEach(budgetId -> privacyBudgets.compute(budgetId, (id, budget) -> budget - 1));

    return ImmutableList.of();
  }

  public Optional<String> getLastAttributionReportToSent() {
    return lastAttributionReportToSent;
  }

  public Optional<ImmutableList<PrivacyBudgetUnit>> getLastBudgetsToConsumeSent() {
    return lastBudgetsToConsumeSent;
  }
}
