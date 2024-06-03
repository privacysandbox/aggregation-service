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
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.MAJOR_VERSION_ONE;
import static com.google.aggregate.adtech.worker.model.SharedInfo.MAJOR_VERSION_ZERO;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import com.google.aggregate.adtech.worker.model.Version;
import com.google.aggregate.adtech.worker.validation.v01.AttributionReportingDebugPrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.AttributionReportingPrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.ProtectedAudiencePrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.SharedStoragePrivacyBudgetKeyFieldsValidator;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

/**
 * Factory class to supply PrivacyBudgetKeyValidator implementation based on SharedInfo API and
 * Major Version type.
 */
public final class PrivacyBudgetKeyValidatorFactory {

  private static final ApiAndMajorVersion ATTRIBUTION_REPORTING_V0 =
      ApiAndMajorVersion.builder()
          .setApi(ATTRIBUTION_REPORTING_API)
          .setMajorVersion(MAJOR_VERSION_ZERO)
          .build();
  private static final ApiAndMajorVersion ATTRIBUTION_REPORTING_V1 =
      ApiAndMajorVersion.builder()
          .setApi(ATTRIBUTION_REPORTING_API)
          .setMajorVersion(MAJOR_VERSION_ONE)
          .build();
  private static final ApiAndMajorVersion ATTRIBUTION_REPORTING_DEBUG_V0 =
      ApiAndMajorVersion.builder()
          .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
          .setMajorVersion(MAJOR_VERSION_ZERO)
          .build();
  private static final ApiAndMajorVersion ATTRIBUTION_REPORTING_DEBUG_V1 =
      ApiAndMajorVersion.builder()
          .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
          .setMajorVersion(MAJOR_VERSION_ONE)
          .build();
  private static final ApiAndMajorVersion PROTECTED_AUDIENCE_API_V0 =
      ApiAndMajorVersion.builder()
          .setApi(PROTECTED_AUDIENCE_API)
          .setMajorVersion(MAJOR_VERSION_ZERO)
          .build();
  private static final ApiAndMajorVersion PROTECTED_AUDIENCE_API_V1 =
      ApiAndMajorVersion.builder()
          .setApi(PROTECTED_AUDIENCE_API)
          .setMajorVersion(MAJOR_VERSION_ONE)
          .build();
  private static final ApiAndMajorVersion SHARED_STORAGE_API_V0 =
      ApiAndMajorVersion.builder()
          .setApi(SHARED_STORAGE_API)
          .setMajorVersion(MAJOR_VERSION_ZERO)
          .build();
  private static final ApiAndMajorVersion SHARED_STORAGE_API_V1 =
      ApiAndMajorVersion.builder()
          .setApi(SHARED_STORAGE_API)
          .setMajorVersion(MAJOR_VERSION_ONE)
          .build();
  private static final ImmutableMap<ApiAndMajorVersion, PrivacyBudgetKeyValidator>
      privacyBudgetKeyValidatorMap =
          ImmutableMap.of(
              ATTRIBUTION_REPORTING_V0,
              new AttributionReportingPrivacyBudgetKeyFieldsValidator(),
              ATTRIBUTION_REPORTING_V1,
              new AttributionReportingPrivacyBudgetKeyFieldsValidator(),
              ATTRIBUTION_REPORTING_DEBUG_V0,
              new AttributionReportingDebugPrivacyBudgetKeyFieldsValidator(),
              ATTRIBUTION_REPORTING_DEBUG_V1,
              new AttributionReportingDebugPrivacyBudgetKeyFieldsValidator(),
              PROTECTED_AUDIENCE_API_V0,
              new ProtectedAudiencePrivacyBudgetKeyFieldsValidator(),
              PROTECTED_AUDIENCE_API_V1,
              new ProtectedAudiencePrivacyBudgetKeyFieldsValidator(),
              SHARED_STORAGE_API_V0,
              new SharedStoragePrivacyBudgetKeyFieldsValidator(),
              SHARED_STORAGE_API_V1,
              new SharedStoragePrivacyBudgetKeyFieldsValidator());

  /**
   * Returns PrivacyBudgetKeyValidator instance corresponding the SharedInfo API and major version.
   */
  public static Optional<PrivacyBudgetKeyValidator> getPrivacyBudgetKeyValidator(
      String api, String version) {
    String majorVersion = Version.parse(version).getMajorVersion();
    PrivacyBudgetKeyValidatorFactory.ApiAndMajorVersion apiVersionKey =
        PrivacyBudgetKeyValidatorFactory.ApiAndMajorVersion.builder()
            .setApi(api)
            .setMajorVersion(majorVersion)
            .build();
    return Optional.ofNullable(privacyBudgetKeyValidatorMap.get(apiVersionKey));
  }

  @AutoValue
  abstract static class ApiAndMajorVersion {
    static Builder builder() {
      return new AutoValue_PrivacyBudgetKeyValidatorFactory_ApiAndMajorVersion.Builder();
    }

    abstract String api();

    abstract String majorVersion();

    @AutoValue.Builder
    abstract static class Builder {
      abstract ApiAndMajorVersion build();

      abstract Builder setMajorVersion(String value);

      abstract Builder setApi(String value);
    }
  }
}
