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
import static com.google.aggregate.adtech.worker.model.SharedInfo.SUPPORTED_MAJOR_VERSIONS;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates that the report Version is supported by this version of Aggregation service. Valid
 * version needs to conform to {major}.{minor} format and needs to have major version in
 * SUPPORTED_MAJOR_VERSIONS list. Exception: 0.0 is not supported.
 */
public final class ReportVersionValidator implements ReportValidator {

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {
    try {
      Version version = Version.parse(report.sharedInfo().version());
      if (!version.isZero()
          && SUPPORTED_MAJOR_VERSIONS.contains(Integer.toString(version.major()))) {
        // Pass validation for reports when SharedInfo Major Version is part of
        // SUPPORTED_MAJOR_VERSIONS.
        return Optional.empty();
      }
    } catch (IllegalArgumentException ex) {
      // Provided Version is not supported, count the report in error counter.
      return createErrorMessage(
          UNSUPPORTED_SHAREDINFO_VERSION, UNSUPPORTED_SHAREDINFO_VERSION.getDescription());
    }

    return createErrorMessage(
        UNSUPPORTED_SHAREDINFO_VERSION, UNSUPPORTED_SHAREDINFO_VERSION.getDescription());
  }

  @AutoValue
  abstract static class Version {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    static Version create(int major, int minor) {
      return new AutoValue_ReportVersionValidator_Version(major, minor);
    }

    static Version parse(String version) throws IllegalArgumentException {
      Preconditions.checkArgument(VERSION_PATTERN.matcher(version).matches());
      String[] parts = version.split("\\.", 2);
      try {
        return Version.create(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException();
      }
    }

    // TODO(b/303480127): Remove isZero() from Version class in ReportVersionValidator when
    // MAJOR_VERSION_ZERO is removed from SUPPORTED_MAJOR_VERSIONS.
    boolean isZero() {
      return (major() == 0 && minor() == 0);
    }

    @Override
    public String toString() {
      return Joiner.on(".").join(major(), minor());
    }

    abstract int major();

    abstract int minor();
  }
}
