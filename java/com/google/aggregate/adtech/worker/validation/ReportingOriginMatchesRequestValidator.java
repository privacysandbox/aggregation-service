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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MISMATCH;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;

/**
 * Validates that the report's reportingOrigin is the same as the attributionReportTo provided in
 * the Aggregation Request.
 */
public final class ReportingOriginMatchesRequestValidator implements ReportValidator {

  @Override
  public Optional<ErrorMessage> validate(Report report, Job ctx) {
    String attributionReportTo =
        ctx.requestInfo().getJobParametersMap().get("attribution_report_to");
    if (report.sharedInfo().reportingOrigin().equals(attributionReportTo)) {
      return Optional.empty();
    }

    return createErrorMessage(
        ATTRIBUTION_REPORT_TO_MISMATCH,
        String.format(
            "Report's attributionReportTo didn't match the AdTech request. Report's"
                + " attributionReportTo: %s, Request's attributionReportTo: %s",
            report.sharedInfo().reportingOrigin(), attributionReportTo));
  }
}
