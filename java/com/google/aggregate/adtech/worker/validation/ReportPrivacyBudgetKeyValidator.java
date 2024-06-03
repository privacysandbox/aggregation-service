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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.REQUIRED_SHAREDINFO_FIELD_INVALID;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.isFieldNonEmpty;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;

/** Validates that the Report's SharedInfo can generate valid Privacy Budget Key. */
public final class ReportPrivacyBudgetKeyValidator implements ReportValidator {

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    if (isFieldNonEmpty(report.sharedInfo().api())) {
      Optional<PrivacyBudgetKeyValidator> validator =
          PrivacyBudgetKeyValidatorFactory.getPrivacyBudgetKeyValidator(
              report.sharedInfo().api().get(), report.sharedInfo().version());
      if (validator.isEmpty()) {
        return createErrorMessage(REQUIRED_SHAREDINFO_FIELD_INVALID);
      }
      return validator.get().validatePrivacyBudgetKey(report.sharedInfo());
    }
    return createErrorMessage(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }
}
