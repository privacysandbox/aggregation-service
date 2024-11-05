/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** Utility class providing methods to fetch the code client version. */
public final class ClientVersionUtils {

  static final String VERSION_UNAVAILABLE_STRING = "client-version-unavailable";
  static final String VERSION_PREFIX_STRING = "aggregation-service/";

  // File paths adapted for different test environments (unit/e2e).
  private static final String[] VERSION_FILE_PATHS = {
    "/generated_version_file.txt", // Root directory
    "./generated_version_file.txt", // Current working directory
  };

  /**
   * Fetches the code client version from the VERSION file.
   *
   * @return the code client version as a String
   */
  public static String getServiceClientVersion() {
    for (String path : VERSION_FILE_PATHS) {
      try (BufferedReader br = new BufferedReader(new FileReader(path))) {
        String version = br.readLine();
        if (version != null && !version.isEmpty()) {
          return VERSION_PREFIX_STRING + version;
        }
      } catch (IOException e) {
        // Proceed to the next potential path.
      }
    }
    // To prevent service crashes from version fetch failures, exceptions are suppressed and a
    // default version string is returned.
    return VERSION_UNAVAILABLE_STRING;
  }
}
