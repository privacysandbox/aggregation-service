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

package com.google.aggregate.privacy.budgeting;

import com.google.aggregate.privacy.budgeting.model.ConsumedPrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface PrivacyBudgetManager {

  /**
   * Adds 1 "count" to the privacy budget for each key in the privacyBudgetKeys.
   *
   * @param privacyBudgetKeys A list of privacy budget keys to consume the budget from.
   * @return A map where all the keys are the privacyBudgetKeys, and the values are the budgets
   *     consumed in this API call. If the budget consumed is 0, that means there were no available
   *     budget left for this key.
   */
  ImmutableMap<PrivacyBudgetKey, ConsumedPrivacyBudget> consumeBudget(
      ImmutableList<PrivacyBudgetKey> privacyBudgetKeys);

  /**
   * Returns the budget for each key in privacyBudgetKeys.
   *
   * @param privacyBudgetKeys A list of privacy budget keys to check the budgets of.
   * @return A map where all the keys are the privacyBudgetKeys, and the values are their associated
   *     privacy budget values.
   */
  ImmutableMap<PrivacyBudgetKey, PrivacyBudget> getBudget(
      ImmutableList<PrivacyBudgetKey> privacyBudgetKeys);
}
