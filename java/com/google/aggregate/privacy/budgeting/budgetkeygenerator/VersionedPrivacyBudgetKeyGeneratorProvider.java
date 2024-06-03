/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.function.Predicate;

/** Provides PrivacyBudgetKeyGenerator for the version given. */
@AutoValue
public abstract class VersionedPrivacyBudgetKeyGeneratorProvider {

  protected abstract ImmutableList<VersionedPrivacyBudgetKeyGenerator>
      versionedPrivacyBudgetKeyGeneratorList();

  public static Builder builder() {
    return new AutoValue_VersionedPrivacyBudgetKeyGeneratorProvider.Builder();
  }

  /** Returns the PrivacyBudgetKeyGenerator for the given version of the report. */
  protected Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(
      PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    return versionedPrivacyBudgetKeyGeneratorList().stream()
        .filter(privacyGenerator -> privacyGenerator.isMappedVersion(privacyBudgetKeyInput))
        .map(privacyGenerator -> privacyGenerator.privacyBudgetKeyGenerator())
        .findAny();
  }

  /**
   * Returns if exactly one Privacy Budget Key generator for the corresponding version. Intended for
   * testing.
   */
  @VisibleForTesting
  boolean doesExactlyOneCorrespondingPBKGeneratorExist(
      PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    ImmutableList<PrivacyBudgetKeyGenerator> privacyBudgetKeyGeneratorsList =
        versionedPrivacyBudgetKeyGeneratorList().stream()
            .filter(privacyGenerator -> privacyGenerator.isMappedVersion(privacyBudgetKeyInput))
            .map(privacyGenerator -> privacyGenerator.privacyBudgetKeyGenerator())
            .collect(ImmutableList.toImmutableList());
    return privacyBudgetKeyGeneratorsList.size() == 1;
  }

  @AutoValue.Builder
  public abstract static class Builder {

    abstract ImmutableList.Builder<VersionedPrivacyBudgetKeyGenerator>
        versionedPrivacyBudgetKeyGeneratorListBuilder();

    public Builder add(
        Predicate<PrivacyBudgetKeyInput> versionPredicate,
        PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator) {
      versionedPrivacyBudgetKeyGeneratorListBuilder()
          .add(
              VersionedPrivacyBudgetKeyGenerator.create(
                  versionPredicate, privacyBudgetKeyGenerator));
      return this;
    }

    public abstract VersionedPrivacyBudgetKeyGeneratorProvider build();
  }

  /** Maps the version with PrivacyBudgetKeyGenerator. */
  @AutoValue
  abstract static class VersionedPrivacyBudgetKeyGenerator {
    abstract Predicate<PrivacyBudgetKeyInput> privacyBudgetKeyInputPredicate();

    abstract PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator();

    static VersionedPrivacyBudgetKeyGenerator create(
        Predicate<PrivacyBudgetKeyInput> privacyBudgetKeyInputPredicate,
        PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator) {
      return new AutoValue_VersionedPrivacyBudgetKeyGeneratorProvider_VersionedPrivacyBudgetKeyGenerator(
          privacyBudgetKeyInputPredicate, privacyBudgetKeyGenerator);
    }

    /**
     * Checks if the given version corresponds to the privacy budget calculation.
     *
     * @param privacyBudgetKeyInput input needed for privacy budget generation.
     */
    boolean isMappedVersion(PrivacyBudgetKeyInput privacyBudgetKeyInput) {
      return privacyBudgetKeyInputPredicate().test(privacyBudgetKeyInput);
    }
  }
}
