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

package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Factory class to supply PrivacyBudgetKeyGenerator implementation based on SharedInfo API type.
 */
public final class PrivacyBudgetKeyGeneratorFactory {
  private static HashMap<String, PrivacyBudgetKeyGenerator> privacyBudgetKeyGeneratorMap =
      new HashMap<>();

  private final ImmutableMap<String, VersionedPrivacyBudgetKeyGeneratorProvider>
      versionedPrivacyBudgetKeyGeneratorMap;

  /**
   * Returns PrivacyBudgetKeyGenerator instance corresponding the API type. If api field is not
   * present in report then report is of API type ATTRIBUTION_REPORTING_API.
   *
   * @deprecated This method is deprecated in favor of the instance method
   *     getPrivacyBudgetKeyGenerator(sharedInfo).
   *     <p>TODO(b/292494729): Remove this method with Privacy Budget Labels implementation.
   */
  @Deprecated(forRemoval = true)
  public static Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(
      Optional<String> api) {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator = Optional.empty();
    if (api.get().equals(ATTRIBUTION_REPORTING_API)) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          ATTRIBUTION_REPORTING_API,
          new com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting
              .V1PrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator =
          Optional.of(privacyBudgetKeyGeneratorMap.get(ATTRIBUTION_REPORTING_API));
    } else if (api.get().equals(PROTECTED_AUDIENCE_API)) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          PROTECTED_AUDIENCE_API,
          new com.google.aggregate.privacy.budgeting.budgetkeygenerator.protectedaudience
              .V1PrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator =
          Optional.of(privacyBudgetKeyGeneratorMap.get(PROTECTED_AUDIENCE_API));
    } else if (api.get().equals(SHARED_STORAGE_API)) {
      privacyBudgetKeyGeneratorMap.putIfAbsent(
          SHARED_STORAGE_API,
          new com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage
              .V1PrivacyBudgetKeyGenerator());
      privacyBudgetKeyGenerator = Optional.of(privacyBudgetKeyGeneratorMap.get(SHARED_STORAGE_API));
    }
    return privacyBudgetKeyGenerator;
  }

  /** Returns PrivacyBudgetKeyGenerator instance corresponding the report's SharedInfo. */
  public Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(SharedInfo sharedInfo) {
    Optional<VersionedPrivacyBudgetKeyGeneratorProvider> provider =
        Optional.ofNullable(versionedPrivacyBudgetKeyGeneratorMap.get(sharedInfo.api().get()));
    if (provider.isPresent()) {
      return provider.get().getPrivacyBudgetKeyGenerator(sharedInfo.version());
    }

    throw new IllegalArgumentException(
        String.format(
            "PrivacyBudgetKeyGenerator not found for the report's sharedInfo. api = %s, version ="
                + " %s.",
            sharedInfo.api().get(), sharedInfo.version()));
  }

  @Inject
  PrivacyBudgetKeyGeneratorFactory(
      ImmutableMap<String, VersionedPrivacyBudgetKeyGeneratorProvider>
          versionedPrivacyBudgetKeyGeneratorMap) {
    this.versionedPrivacyBudgetKeyGeneratorMap = versionedPrivacyBudgetKeyGeneratorMap;
  }
}
