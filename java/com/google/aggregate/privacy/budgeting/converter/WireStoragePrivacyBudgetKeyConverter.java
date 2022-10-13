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

import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;

// TODO: Rename the class once the situation regarding binary splits are ready. It will likely move
// away from WireStorage.
public final class WireStoragePrivacyBudgetKeyConverter extends PrivacyBudgetKeyConverter {

  @Override
  protected com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey doForward(
      PrivacyBudgetKey privacyBudgetKey) {
    return com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey.builder()
        .setOriginalReportTime(privacyBudgetKey.originalReportTime())
        .setPrivacyBudgetKey(privacyBudgetKey.key())
        .build();
  }

  @Override
  protected PrivacyBudgetKey doBackward(
      com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey privacyBudgetKey) {
    return PrivacyBudgetKey.builder()
        .setOriginalReportTime(privacyBudgetKey.originalReportTime())
        .setKey(privacyBudgetKey.privacyBudgetKey())
        .build();
  }
}
