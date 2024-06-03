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

package com.google.aggregate.adtech.worker.validation.v01;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.REQUIRED_SHAREDINFO_FIELD_INVALID;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.isFieldNonEmpty;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.validation.PrivacyBudgetKeyValidator;
import java.util.Optional;

/**
 * Validates that the Protected Audience API type report's SharedInfo fields used in Privacy Budget
 * Key generation are present and not null/empty.
 */
public class ProtectedAudiencePrivacyBudgetKeyFieldsValidator implements PrivacyBudgetKeyValidator {

  @Override
  public Optional<ErrorMessage> validatePrivacyBudgetKey(SharedInfo sharedInfo) {
    if (isFieldNonEmpty(sharedInfo.reportingOrigin())
        && isFieldNonEmpty(sharedInfo.version())
        && sharedInfo.scheduledReportTime() != null) {
      return Optional.empty();
    }

    return createErrorMessage(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }
}
