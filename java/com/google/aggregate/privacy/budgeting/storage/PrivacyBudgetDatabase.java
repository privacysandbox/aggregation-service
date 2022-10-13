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

package com.google.aggregate.privacy.budgeting.storage;

// TODO: Add InMemoryPrivacyBudgetDatabase and call this from InMemoryPrivacyBudgetManager:
// b/185832379.
/** Interface for reading from and writing to the privacy budget database. */
public interface PrivacyBudgetDatabase {

  /**
   * Adds 1 to the consumedBudget column for a {@code PrivacyBudgetKey} in the database. If the key
   * does not exist in the database, this will insert a new row and initialize the consumedBudget to
   * 1. If the key already exists, it will simply add 1 to consumedBudget. This performs a
   * transaction on the database to 1) insert/update the row and 2) read the updated value.
   *
   * @param key The privacy budget key to consume the budget from. Set the {@code
   *     PrivacyBudgetKey.privacyBudgetKey} and {@code PrivacyBudgetKey.originalReportTime} as part
   *     of the request.
   * @return The budget that this was consumed. If the key was already at the limit, {@code
   *     ConsumedPrivacyBudget.consumedBudget} returns 0. Otherwise, it will return the budget that
   *     was consumed.
   */
  ConsumedPrivacyBudget consumeBudget(PrivacyBudgetKey key);

  /**
   * Gets the budget for a {@code PrivacyBudgetKey} from the database. If the key does not exist,
   * the consumedBudget will be set to 0. Otherwise, consumedBudget will be set to the value from
   * the database.
   *
   * @param key The privacy budget key to consume the budget from. Set the {@code
   *     PrivacyBudgetKey.privacyBudgetKey} and {@code PrivacyBudgetKey.originalReportTime} as part
   *     of the request.
   * @return The current consumed budget for this key in the database.
   */
  PrivacyBudget getBudget(PrivacyBudgetKey key);
}
