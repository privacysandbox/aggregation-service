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

import static com.google.aggregate.adtech.worker.model.ErrorCounter.UNSUPPORTED_SHAREDINFO_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SUPPORTED_MAJOR_VERSIONS;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.Version;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;

/**
 * Validates that the report Version is supported by this version of Aggregation service. Valid
 * version needs to conform to {major}.{minor} format and needs to have major version in
 * SUPPORTED_MAJOR_VERSIONS list. Exception: 0.0 is not supported.
 */
public final class ReportVersionValidator implements ReportValidator {

  private static final Version latestVersion = Version.parse(LATEST_VERSION);

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    try {
      Version version = Version.parse(report.sharedInfo().version());
      if (version.isZero()) {
        // 0.0 is not supported sharedInfo version.
        return createErrorMessage(UNSUPPORTED_SHAREDINFO_VERSION);
      } else if (isReportVersionHigherThanLatestVersion(version)) {
        // throw exception to fail job.
        throw new ValidationException(
            UNSUPPORTED_SHAREDINFO_VERSION,
            String.format(
                "Current Aggregation Service deployment does not support Aggregatable reports with"
                    + " shared_info.version %s. This may require an Aggregation Service update. To"
                    + " continue please update Aggregation service or use only Aggregatable reports"
                    + " with supported major shared_info.version(s) are- %s",
                version, SUPPORTED_MAJOR_VERSIONS));
      } else if (SUPPORTED_MAJOR_VERSIONS.contains(Integer.toString(version.major()))) {
        // Pass validation for reports when SharedInfo Major Version is part of
        // SUPPORTED_MAJOR_VERSIONS.
        return Optional.empty();
      }
    } catch (IllegalArgumentException ex) {
      return createErrorMessage(UNSUPPORTED_SHAREDINFO_VERSION);
    }

    return createErrorMessage(UNSUPPORTED_SHAREDINFO_VERSION);
  }

  private boolean isReportVersionHigherThanLatestVersion(Version version) {
    return (version.major() > latestVersion.major());
  }
}
