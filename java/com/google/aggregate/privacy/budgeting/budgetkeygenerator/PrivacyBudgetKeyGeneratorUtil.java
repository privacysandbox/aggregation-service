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
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import java.util.function.Predicate;

/** Static utilities related to PrivacyBudgetKeyGenerators. */
public final class PrivacyBudgetKeyGeneratorUtil {

  /** Returns the predicate of PrivacyBudgetKeyInput for V1 version of PrivacyBudgetKeyGenerator. */
  public static Predicate<PrivacyBudgetKeyInput>
      getPrivacyBudgetKeyGeneratorV1Predicate() {
    Version version1_0 =
        Version.create(
            /** majorVersion = */ 1,
            /** minorVersion = */ 0);
    Predicate<Version> versionsLessThan1_0 =
        Version.getBetweenVersionPredicate(
            /* lowerInclusiveVersion= */ Version.create(/* major= */ 0, /* minor= */ 1),
            /* higherExclusiveVersion= */ version1_0);
    Predicate<Optional<UnsignedLong>> filteringIdIsZero =
        filteringId -> filteringId.isEmpty() || filteringId.get().equals(UnsignedLong.ZERO);
    Predicate<PrivacyBudgetKeyInput> versionV1Predicate =
        pbkInput ->
            versionsLessThan1_0.test(Version.parse(pbkInput.sharedInfo().version()))
                && filteringIdIsZero.test(pbkInput.filteringId());
    return versionV1Predicate;
  }

  /** Returns the predicate of PrivacyBudgetKeyInput for V2 version of PrivacyBudgetKeyGenerator. */
  public static Predicate<PrivacyBudgetKeyInput>
      getPrivacyBudgetKeyGeneratorV2Predicate() {
      Version version1_0 =
              Version.create(
                      /** majorVersion = */ 1,
                      /** minorVersion = */ 0);
    Predicate<Version> versionsGreaterThanOrEqualTo1_0 =
        Version.getGreaterThanOrEqualToVersionPredicate(version1_0);
    Predicate<Optional<UnsignedLong>> filteringIdIsNotZero =
        filteringId -> filteringId.isPresent() && !filteringId.get().equals(UnsignedLong.ZERO);

    Predicate<PrivacyBudgetKeyInput> versionV2Predicate =
        pbkInput ->
            versionsGreaterThanOrEqualTo1_0.test(Version.parse(pbkInput.sharedInfo().version()))
                || filteringIdIsNotZero.test(pbkInput.filteringId());
    return versionV2Predicate;
  }
}
