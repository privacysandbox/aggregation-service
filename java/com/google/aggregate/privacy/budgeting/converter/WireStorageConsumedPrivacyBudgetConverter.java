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

package com.google.aggregate.privacy.budgeting.converter;

import com.google.aggregate.privacy.budgeting.model.ConsumedPrivacyBudget;

// TODO (b/187715286): Rename the class once the situation regarding binary splits are ready. It
// will likely move away from WireStorage.
public class WireStorageConsumedPrivacyBudgetConverter extends ConsumedPrivacyBudgetConverter {

  @Override
  protected ConsumedPrivacyBudget doForward(
      com.google.aggregate.privacy.budgeting.storage.ConsumedPrivacyBudget consumedPrivacyBudget) {
    return ConsumedPrivacyBudget.builder()
        .setConsumedBudget(consumedPrivacyBudget.consumedBudget())
        .build();
  }

  @Override
  protected com.google.aggregate.privacy.budgeting.storage.ConsumedPrivacyBudget doBackward(
      ConsumedPrivacyBudget consumedPrivacyBudget) {
    throw new UnsupportedOperationException("Converting to storage format is not supported");
  }
}
