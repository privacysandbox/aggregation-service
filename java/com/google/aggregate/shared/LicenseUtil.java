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

package com.google.aggregate.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to print all licenses for dependencies.
 */
public final class LicenseUtil {

  public static void printLicenses() {
    try {
      InputStream inputStream =
          LicenseUtil.class.getResourceAsStream("/licenses/license_compliance.csv");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      List<DependencyMetadata> dependencies = new ArrayList<>();
      String inputLine;
      while ((inputLine = bufferedReader.readLine()) != null) {
        String[] values = inputLine.split(",");
        DependencyMetadata dep =
            DependencyMetadata.create(values[0], values[1], values[3], values[2]);
        dependencies.add(dep);
      }
      printAllLicenses(dependencies);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void printAllLicenses(List<DependencyMetadata> dependencies) throws IOException {
    Path rootPath = Path.of("/licenses");
    for (DependencyMetadata dependency : dependencies) {
      Path path =
          rootPath.resolve(dependency.license()).resolve(dependency.name()).resolve("LICENSE.txt");
      readAndPrint(dependency.license(), dependency.name(), path.toString());
    }
  }

  private static void readAndPrint(String licenseName, String dependencyName, String resourceName)
      throws IOException {
    System.out.println(
        String.format(
            "               ----- Dependency: %s ::: License : %s -----",
            dependencyName, licenseName));
    InputStream inputStream = LicenseUtil.class.getResourceAsStream(resourceName);
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    String inputLine;
    while ((inputLine = bufferedReader.readLine()) != null) {
      System.out.println(inputLine);
    }
    System.out.println(String.format("               ###### Done ######\n\n"));
  }
}
