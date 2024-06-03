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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MALFORMED;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.common.net.InternetDomainName;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;

/**
 * Validates that the report's reportingOrigin is a valid domain.
 *
 * <p>Valid domain is defined as one that is syntactically valid and ends in a public suffix. This
 * does not check if the domain actually exists or if any host is reachable at the domain. See
 * {@code InternetDomainName} for more detail.
 */
public final class ReportingOriginIsDomainValidator implements ReportValidator {

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    if (InternetDomainName.isValid(report.sharedInfo().reportingOrigin())
        && InternetDomainName.from(report.sharedInfo().reportingOrigin()).hasPublicSuffix()) {
      return Optional.empty();
    }

    return createErrorMessage(ATTRIBUTION_REPORT_TO_MALFORMED);
  }
}
