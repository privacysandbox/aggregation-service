/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Represents the version of the report. */
@AutoValue
public abstract class Version implements Comparable<Version> {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

  public static Version create(int major, int minor) {
    return new AutoValue_Version(major, minor);
  }

  public static Version parse(String version) {
    Preconditions.checkArgument(VERSION_PATTERN.matcher(version).matches());
    String[] parts = version.split("\\.", 2);
    return Version.create(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
  }

  // TODO(b/303480127): Remove isZero() from Version class in ReportVersionValidator when
  // MAJOR_VERSION_ZERO is removed from SUPPORTED_MAJOR_VERSIONS.
  public boolean isZero() {
    return (major() == 0 && minor() == 0);
  }

  public String getMajorVersion() {
    return String.valueOf(major());
  }

  @Override
  public String toString() {
    return Joiner.on(".").join(major(), minor());
  }

  public abstract int major();

  public abstract int minor();

  @Override
  public int compareTo(Version other) {
    if (other == null) {
      return 1;
    }
    int result = major() - other.major();
    if (result != 0) {
      return result;
    }
    return minor() - other.minor();
  }

  /**
   * Creates a predicate that checks if the version is between lowerInclusiveVersion and
   * higherExclusiveVersion.
   *
   * @param lowerInclusiveVersion lower version included in the range
   * @param higherExclusiveVersion higher version excluded from the range.
   */
  public static Predicate<Version> getBetweenVersionPredicate(
      Version lowerInclusiveVersion, Version higherExclusiveVersion) {
    if (higherExclusiveVersion.compareTo(lowerInclusiveVersion) <= 0) {
      throw new IllegalArgumentException(
          "higherExclusiveVersion should be greater than lowerInclusiveVersion");
    }
    return version ->
        version.compareTo(lowerInclusiveVersion) >= 0
            && version.compareTo(higherExclusiveVersion) < 0;
  }

  /**
   * Creates a predicate that checks if the version >= compareToVersion.
   *
   * @param compareToVersion
   */
  public static Predicate<Version> getGreaterThanOrEqualToVersionPredicate(
      Version compareToVersion) {
    return version -> version.compareTo(compareToVersion) >= 0;
  }
}
