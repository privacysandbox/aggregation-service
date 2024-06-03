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

package com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage;

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorUtil;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.VersionedPrivacyBudgetKeyGeneratorProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Predicate;
import javax.inject.Qualifier;

/** Provides Shared Storage PrivacyBudgetKeyGenerators mapped to versions. */
public class PrivacyBudgetKeyGeneratorModule extends AbstractModule {

  @Provides
  @SharedStoragePrivacyBudgetKeyGenerators
  VersionedPrivacyBudgetKeyGeneratorProvider providePrivacyBudgetKeyGenerators() {
    Predicate<PrivacyBudgetKeyInput> versionV1Predicate =
        PrivacyBudgetKeyGeneratorUtil.getPrivacyBudgetKeyGeneratorV1Predicate();
    Predicate<PrivacyBudgetKeyInput> versionV2Predicate =
        PrivacyBudgetKeyGeneratorUtil.getPrivacyBudgetKeyGeneratorV2Predicate();

    // <Important> A version should map to only one Privacy Budget Key generator.
    return VersionedPrivacyBudgetKeyGeneratorProvider.builder()
        .add(versionV1Predicate, new V1PrivacyBudgetKeyGenerator())
        .add(versionV2Predicate, new V2PrivacyBudgetKeyGenerator())
        .build();
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SharedStoragePrivacyBudgetKeyGenerators {}
}
