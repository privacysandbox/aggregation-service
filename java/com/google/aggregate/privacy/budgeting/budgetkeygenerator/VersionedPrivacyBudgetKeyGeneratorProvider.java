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

import com.google.aggregate.adtech.worker.model.Version;
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
  protected Optional<PrivacyBudgetKeyGenerator> getPrivacyBudgetKeyGenerator(String reportVersion) {
    Version versionObj = getReportVersion(reportVersion);
    return versionedPrivacyBudgetKeyGeneratorList().stream()
        .filter(privacyGenerator -> privacyGenerator.isMappedVersion(versionObj))
        .map(privacyGenerator -> privacyGenerator.privacyBudgetKeyGenerator())
        .findAny();
  }

  /**
   * Returns if exactly one Privacy Budget Key generator for the corresponding version. Intended for
   * testing.
   */
  @VisibleForTesting
  boolean doesExactlyOneCorrespondingPBKGeneratorExist(String reportVersion) {
    Version versionObj = getReportVersion(reportVersion);
    ImmutableList<PrivacyBudgetKeyGenerator> privacyBudgetKeyGeneratorsList =
        versionedPrivacyBudgetKeyGeneratorList().stream()
            .filter(privacyGenerator -> privacyGenerator.isMappedVersion(versionObj))
            .map(privacyGenerator -> privacyGenerator.privacyBudgetKeyGenerator())
            .collect(ImmutableList.toImmutableList());
    return privacyBudgetKeyGeneratorsList.size() == 1;
  }

  private static Version getReportVersion(String reportVersion) {
    try {
      return Version.parse(reportVersion);
    } catch (IllegalArgumentException iae) {
      // Impossible since the validation layer verifies the format.
      throw new AssertionError("Invalid report version format.", iae);
    }
  }

  @AutoValue.Builder
  public abstract static class Builder {

    abstract ImmutableList.Builder<VersionedPrivacyBudgetKeyGenerator>
        versionedPrivacyBudgetKeyGeneratorListBuilder();

    public Builder add(
        Predicate<Version> versionPredicate, PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator) {
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
    abstract Predicate<Version> versionPredicate();

    abstract PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator();

    static VersionedPrivacyBudgetKeyGenerator create(
        Predicate<Version> versionPredicate, PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator) {
      return new AutoValue_VersionedPrivacyBudgetKeyGeneratorProvider_VersionedPrivacyBudgetKeyGenerator(
          versionPredicate, privacyBudgetKeyGenerator);
    }

    /**
     * Checks if the given version corresponds to the privacy budget calculation.
     *
     * @param version report version.
     */
    boolean isMappedVersion(Version version) {
      return versionPredicate().test(version);
    }
  }
}
