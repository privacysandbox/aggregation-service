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

package com.google.aggregate.util;

import static com.google.common.truth.Truth.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ClientVersionUtilsTest {

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Test
  public void testGetServiceClientVersion_returnsValidVersion() throws IOException {
    File versionFile = tmpFolder.newFile("test_version_file.txt");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(versionFile))) {
      writer.write("1.2.3");
    }

    ClientVersionUtils.VERSION_FILE_PATHS = new String[] {versionFile.getAbsolutePath()};

    String version = ClientVersionUtils.getServiceClientVersion();
    assertThat(version).isEqualTo("aggregation-service/1.2.3");
  }

  @Test
  public void testGetServiceClientVersion_returnsVersionUnavailable_whenFileNotFound() {
    ClientVersionUtils.VERSION_FILE_PATHS = new String[] {"this_file_does_not_exist.txt"};

    String version = ClientVersionUtils.getServiceClientVersion();
    assertThat(version).isEqualTo(ClientVersionUtils.VERSION_UNAVAILABLE_STRING);
  }
}
