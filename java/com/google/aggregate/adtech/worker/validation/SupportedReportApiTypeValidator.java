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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.UNSUPPORTED_REPORT_API_TYPE;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.FLEDGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Validates that the report API type is supported for aggregation. */
public final class SupportedReportApiTypeValidator implements ReportValidator {

  Set<String> supportedApiTypes;

  /** Constructor initializes set of supported report api types */
  @Inject
  public SupportedReportApiTypeValidator() {
    supportedApiTypes = new HashSet<>();
    supportedApiTypes.add(ATTRIBUTION_REPORTING_API);
    supportedApiTypes.add(FLEDGE_API);
    supportedApiTypes.add(SHARED_STORAGE_API);
  }

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    if (report.sharedInfo().api().isEmpty()
        || supportedApiTypes.contains(report.sharedInfo().api().get())) {
      /**
       * attribution-reporting reports with version "" do not have api field present in shared Info
       */
      return Optional.empty();
    }

    return Optional.of(
        ErrorMessage.builder()
            .setCategory(UNSUPPORTED_REPORT_API_TYPE.name())
            .setDetailedErrorMessage(detailedErrorMessage(report.sharedInfo().api().get()))
            .build());
  }

  private String detailedErrorMessage(String unsupportedApiType) {
    return String.format("Report's api type %s is not supported.", unsupportedApiType);
  }
}
