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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import java.util.HashMap;
import java.util.Optional;

/**
 * Factory class to supply PrivacyBudgetKeyGenerator implementation based on SharedInfo API type.
 */
public final class PrivacyBudgetKeyGeneratorFactory {
  private static HashMap<String, PrivacyBudgetKeyGenerator> privacyBudgetKeyGeneratorMap =
      new HashMap<>();

  /**
   * Returns PrivacyBudgetKeyGenerator instance corresponding the API type. If api field is not
   * present in report then report is of API type ATTRIBUTION_REPORTING_API.
   */
  public static Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(
      Optional<String> api) {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator = Optional.empty();
    if (api.isEmpty() || (api.get().equals(ATTRIBUTION_REPORTING_API))) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          ATTRIBUTION_REPORTING_API, new AttributionReportingPrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator =
          Optional.of(privacyBudgetKeyGeneratorMap.get(ATTRIBUTION_REPORTING_API));
    } else if (api.get().equals(PROTECTED_AUDIENCE_API)) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          PROTECTED_AUDIENCE_API, new ProtectedAudiencePrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator =
          Optional.of(privacyBudgetKeyGeneratorMap.get(PROTECTED_AUDIENCE_API));
    } else if (api.get().equals(SHARED_STORAGE_API)) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          SHARED_STORAGE_API, new SharedStoragePrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator = Optional.of(privacyBudgetKeyGeneratorMap.get(SHARED_STORAGE_API));
    }
    return privacyBudgetKeyGenerator;
  }
}
