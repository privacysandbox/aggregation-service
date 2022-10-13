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

package com.google.aggregate.privacy.budgeting.testing;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.google.aggregate.privacy.budgeting.PrivacyBudgetManager;
import com.google.aggregate.privacy.budgeting.model.ConsumedPrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class InMemoryPrivacyBudgetManager implements PrivacyBudgetManager {

  private final int budget;
  private final Map<PrivacyBudgetKey, Integer> consumedDatabase;

  /**
   * Constructs an in-memory privacy budget manager where the database is stored as a hash map.
   *
   * @param budget The budget that is allowed per privacy budget key.
   */
  public InMemoryPrivacyBudgetManager(int budget) {
    this.budget = budget;
    this.consumedDatabase = new HashMap<>();
  }

  /**
   * Updates the in-memory hash map and sets the budget for a given key.
   *
   * @param privacyBudgetKey key to update.
   * @param budget the new value for this key.
   */
  public void setBudget(PrivacyBudgetKey privacyBudgetKey, int budget) {
    consumedDatabase.put(privacyBudgetKey, budget);
  }

  @Override
  public ImmutableMap<PrivacyBudgetKey, ConsumedPrivacyBudget> consumeBudget(
      ImmutableList<PrivacyBudgetKey> privacyBudgetKeys) {
    privacyBudgetKeys.forEach(this::consumeBudgetSingleKey);

    return privacyBudgetKeys.stream().collect(toImmutableMap(identity(), this::getConsumedBudget));
  }

  @Override
  public ImmutableMap<PrivacyBudgetKey, PrivacyBudget> getBudget(
      ImmutableList<PrivacyBudgetKey> privacyBudgetKeys) {
    return privacyBudgetKeys.stream().collect(toImmutableMap(identity(), this::getRemainingBudget));
  }

  private void consumeBudgetSingleKey(PrivacyBudgetKey key) {
    consumedDatabase.putIfAbsent(key, 0);
    consumedDatabase.compute(key, ((privacyBudgetKey, consumedBudget) -> consumedBudget + 1));
  }

  private ConsumedPrivacyBudget getConsumedBudget(PrivacyBudgetKey key) {
    int consumedBudget = consumedDatabase.get(key) > budget ? 0 : 1;
    return ConsumedPrivacyBudget.builder().setConsumedBudget(consumedBudget).build();
  }

  private PrivacyBudget getRemainingBudget(PrivacyBudgetKey key) {
    return PrivacyBudget.builder()
        .setRemainingBudget(Math.max(0, budget - consumedDatabase.getOrDefault(key, 0)))
        .build();
  }
}
