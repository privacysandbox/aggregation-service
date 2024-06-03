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

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Factory class to supply PrivacyBudgetKeyGenerator implementation based on SharedInfo API type.
 */
public final class PrivacyBudgetKeyGeneratorFactory {

  private final ImmutableMap<String, VersionedPrivacyBudgetKeyGeneratorProvider>
      versionedPrivacyBudgetKeyGeneratorMap;

  /** Returns PrivacyBudgetKeyGenerator instance corresponding to the privacyBudgetKeyInput. */
  public Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(
      PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    SharedInfo sharedInfo = privacyBudgetKeyInput.sharedInfo();
    Optional<VersionedPrivacyBudgetKeyGeneratorProvider> provider =
        Optional.ofNullable(versionedPrivacyBudgetKeyGeneratorMap.get(sharedInfo.api().get()));
    if (provider.isPresent()) {
      return provider.get().getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput);
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
