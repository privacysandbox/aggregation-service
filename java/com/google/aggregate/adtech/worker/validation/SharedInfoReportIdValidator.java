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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.INVALID_REPORT_ID;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.isFieldNonEmpty;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;
import java.util.UUID;

/** Validates that the report ID in SharedInfo is a valid UUID. */
public final class SharedInfoReportIdValidator implements ReportValidator {

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    if (isFieldNonEmpty(report.sharedInfo().reportId())) {
      try {
        UUID.fromString(report.sharedInfo().reportId().get());
        return Optional.empty();
      } catch (IllegalArgumentException exception) {
        return createErrorMessage(INVALID_REPORT_ID);
      }
    }
    return createErrorMessage(INVALID_REPORT_ID);
  }
}
