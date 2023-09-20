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
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.validation.v01.AttributionReportingPrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.ProtectedAudiencePrivacyBudgetKeyFieldsValidator;
import com.google.aggregate.adtech.worker.validation.v01.SharedStoragePrivacyBudgetKeyFieldsValidator;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetKeyValidatorFactoryTest {
  private static final String INVALID_API = "invalid-api";
  private static final String INVALID_VERSION = "invalid-version";

  @Test
  public void attributionReportingV01_privacyBudgetValidator_returnsValidator() {
    PrivacyBudgetKeyValidator attributionReportingPrivacyBudgetKeyValidator =
        PrivacyBudgetKeyValidatorFactory.getPrivacyBudgetKeyValidator(
                ATTRIBUTION_REPORTING_API, VERSION_0_1)
            .get();

    assertThat(attributionReportingPrivacyBudgetKeyValidator)
        .isInstanceOf(AttributionReportingPrivacyBudgetKeyFieldsValidator.class);
  }

  @Test
  public void protectedAudienceV01_privacyBudgetValidator_returnsValidator() {
    PrivacyBudgetKeyValidator protectedAudiencePrivacyBudgetKeyValidator =
        PrivacyBudgetKeyValidatorFactory.getPrivacyBudgetKeyValidator(
                PROTECTED_AUDIENCE_API, VERSION_0_1)
            .get();

    assertThat(protectedAudiencePrivacyBudgetKeyValidator)
        .isInstanceOf(ProtectedAudiencePrivacyBudgetKeyFieldsValidator.class);
  }

  @Test
  public void sharedStorageV01_privacyBudgetValidator_returnsValidator() {
    PrivacyBudgetKeyValidator sharedStoragePrivacyBudgetKeyValidator =
        PrivacyBudgetKeyValidatorFactory.getPrivacyBudgetKeyValidator(
                SHARED_STORAGE_API, VERSION_0_1)
            .get();

    assertThat(sharedStoragePrivacyBudgetKeyValidator)
        .isInstanceOf(SharedStoragePrivacyBudgetKeyFieldsValidator.class);
  }

  @Test
  public void invalidApiVersion_privacyBudgetValidator_returnsNoValidator() {
    assertThat(
            PrivacyBudgetKeyValidatorFactory.getPrivacyBudgetKeyValidator(
                INVALID_API, INVALID_VERSION))
        .isEqualTo(Optional.empty());
  }
}
