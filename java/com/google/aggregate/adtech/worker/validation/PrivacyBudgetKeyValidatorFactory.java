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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;

import com.google.aggregate.adtech.worker.validation.v01.AttributionReportingPrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.ProtectedAudiencePrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.SharedStoragePrivacyBudgetKeyFieldsValidator;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

/**
 * Factory class to supply PrivacyBudgetKeyValidator implementation based on SharedInfo API and
 * Version type.
 */
public final class PrivacyBudgetKeyValidatorFactory {

  private static final ApiAndVersion ATTRIBUTION_REPORTING_V01 =
      ApiAndVersion.builder().setApi(ATTRIBUTION_REPORTING_API).setVersion(VERSION_0_1).build();
  private static final ApiAndVersion PROTECTED_AUDIENCE_API_V01 =
      ApiAndVersion.builder().setApi(PROTECTED_AUDIENCE_API).setVersion(VERSION_0_1).build();
  private static final ApiAndVersion SHARED_STORAGE_API_V01 =
      ApiAndVersion.builder().setApi(SHARED_STORAGE_API).setVersion(VERSION_0_1).build();
  private static final ImmutableMap<ApiAndVersion, PrivacyBudgetKeyValidator>
      privacyBudgetKeyValidatorMap =
          ImmutableMap.of(
              ATTRIBUTION_REPORTING_V01,
              new AttributionReportingPrivacyBudgetKeyFieldsValidator(),
              PROTECTED_AUDIENCE_API_V01,
              new ProtectedAudiencePrivacyBudgetKeyFieldsValidator(),
              SHARED_STORAGE_API_V01,
              new SharedStoragePrivacyBudgetKeyFieldsValidator());

  /** Returns PrivacyBudgetKeyValidator instance corresponding the SharedInfo API and version. */
  public static Optional<PrivacyBudgetKeyValidator> getPrivacyBudgetKeyValidator(
      String api, String version) {
    PrivacyBudgetKeyValidatorFactory.ApiAndVersion apiVersionKey =
        PrivacyBudgetKeyValidatorFactory.ApiAndVersion.builder()
            .setApi(api)
            .setVersion(version)
            .build();
    return Optional.ofNullable(privacyBudgetKeyValidatorMap.get(apiVersionKey));
  }

  @AutoValue
  abstract static class ApiAndVersion {
    static Builder builder() {
      return new AutoValue_PrivacyBudgetKeyValidatorFactory_ApiAndVersion.Builder();
    }

    abstract String api();

    abstract String version();

    @AutoValue.Builder
    abstract static class Builder {
      abstract ApiAndVersion build();

      abstract Builder setVersion(String value);

      abstract Builder setApi(String value);
    }
  }
}
