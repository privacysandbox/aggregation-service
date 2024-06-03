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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting.PrivacyBudgetKeyGeneratorModule.AttributionReportingPrivacyBudgetKeyGenerators;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug.PrivacyBudgetKeyGeneratorModule.AttributionReportingDebugPrivacyBudgetKeyGenerators;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.protectedaudience.PrivacyBudgetKeyGeneratorModule.ProtectedAudiencePrivacyBudgetKeyGenerators;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage.PrivacyBudgetKeyGeneratorModule.SharedStoragePrivacyBudgetKeyGenerators;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Module for providing api to VersionedPrivacyBudgetKeyGeneratorProvider mapping. */
public class PrivacyBudgetKeyGeneratorModule extends AbstractModule {

  @Override
  protected void configure() {
    install(
        new com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting
            .PrivacyBudgetKeyGeneratorModule());
    install(
        new com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug
            .PrivacyBudgetKeyGeneratorModule());
    install(
        new com.google.aggregate.privacy.budgeting.budgetkeygenerator.protectedaudience
            .PrivacyBudgetKeyGeneratorModule());
    install(
        new com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage
            .PrivacyBudgetKeyGeneratorModule());
  }

  @Provides
  ImmutableMap<String, VersionedPrivacyBudgetKeyGeneratorProvider>
      providePrivacyBudgetKeyGeneratorMap(
          @AttributionReportingPrivacyBudgetKeyGenerators
              VersionedPrivacyBudgetKeyGeneratorProvider
                  attributionReportingPrivacyBudgetKeyGenerators,
          @AttributionReportingDebugPrivacyBudgetKeyGenerators
              VersionedPrivacyBudgetKeyGeneratorProvider
                  attributionReportingDebugPrivacyBudgetKeyGenerators,
          @ProtectedAudiencePrivacyBudgetKeyGenerators
              VersionedPrivacyBudgetKeyGeneratorProvider
                  protectedAudiencePrivacyBudgetKeyGenerators,
          @SharedStoragePrivacyBudgetKeyGenerators
              VersionedPrivacyBudgetKeyGeneratorProvider sharedStoragePrivacyBudgetKeyGenerators) {
    return ImmutableMap.of(
        ATTRIBUTION_REPORTING_API,
        attributionReportingPrivacyBudgetKeyGenerators,
        ATTRIBUTION_REPORTING_DEBUG_API,
        attributionReportingDebugPrivacyBudgetKeyGenerators,
        PROTECTED_AUDIENCE_API,
        protectedAudiencePrivacyBudgetKeyGenerators,
        SHARED_STORAGE_API,
        sharedStoragePrivacyBudgetKeyGenerators);
  }
}
