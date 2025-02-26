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

package com.google.aggregate.adtech.worker.writer.json;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatableInputBudgetConsumptionInfo;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PrivacyBudgetExhaustedInfoSerdes;
import com.google.aggregate.adtech.worker.writer.PrivacyBudgetExhaustedInfoWriter.FileWriteException;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocalPrivacyBudgetExhaustedInfoWriterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject LocalPrivacyBudgetExhaustedInfoWriter localPrivacyBudgetExhaustedInfoWriter;

  @Inject PrivacyBudgetExhaustedInfoSerdes privacyBudgetExhaustedInfoSerdes;

  private static final UnsignedLong FILTERING_ID = UnsignedLong.valueOf(1);
  private static final String JSON_FILE_NAME = "privacy_budget_exhausted_debugging_info.json";
  private static final Instant TIME = Instant.ofEpochSecond(1609459200);
  private static final SharedInfo SHARED_INFO_1 =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("destination.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech.com")
          .setScheduledReportTime(TIME)
          .setSourceRegistrationTime(TIME)
          .build();

  private static final SharedInfo SHARED_INFO_2 =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("destination.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech2.com")
          .setScheduledReportTime(TIME)
          .setSourceRegistrationTime(TIME)
          .build();

  private static final AggregatableInputBudgetConsumptionInfo info1 =
      AggregatableInputBudgetConsumptionInfo.builder()
          .setPrivacyBudgetKeyInput(
              PrivacyBudgetKeyInput.builder()
                  .setSharedInfo(SHARED_INFO_1)
                  .setFilteringId(FILTERING_ID)
                  .build())
          .build();

  private static final AggregatableInputBudgetConsumptionInfo info2 =
      AggregatableInputBudgetConsumptionInfo.builder()
          .setPrivacyBudgetKeyInput(
              PrivacyBudgetKeyInput.builder()
                  .setSharedInfo(SHARED_INFO_2)
                  .setFilteringId(FILTERING_ID)
                  .build())
          .build();

  private static final PrivacyBudgetExhaustedInfo exhaustedInfo =
      PrivacyBudgetExhaustedInfo.builder()
          .setAggregatableInputBudgetConsumptionInfos(ImmutableSet.of(info1, info2))
          .build();

  private FileSystem filesystem;
  private Path jsonFilePath;

  @Before
  public void setUp() throws Exception {
    filesystem =
        Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    jsonFilePath = filesystem.getPath(JSON_FILE_NAME);
  }

  @Test
  public void testWriteFile() throws Exception {
    String expectedOutputFilePath =
        "javatests/com/google/aggregate/adtech/worker/writer/json/test/expected_privacy_budget_exhausted_debugging_info.json";
    String expectedOutputContent = Files.readString(Paths.get(expectedOutputFilePath));

    localPrivacyBudgetExhaustedInfoWriter.writePrivacyBudgetExhaustedInfo(
        exhaustedInfo, jsonFilePath);

    // skip trainling '\n' from test file, included to bypass pre-commit hook
    assertThat(Files.readString(jsonFilePath)).isEqualTo(expectedOutputContent.stripTrailing());
  }

  @Test
  public void testExceptionOnFailedWrite() throws Exception {
    Path nonExistentDirectory =
        jsonFilePath.getFileSystem().getPath("/nonexistant", jsonFilePath.toString());

    assertThrows(
        FileWriteException.class,
        () ->
            localPrivacyBudgetExhaustedInfoWriter.writePrivacyBudgetExhaustedInfo(
                exhaustedInfo, nonExistentDirectory));
  }

  public static final class TestEnv extends AbstractModule {}
}
