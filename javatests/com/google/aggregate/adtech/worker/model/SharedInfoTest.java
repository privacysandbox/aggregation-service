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

package com.google.aggregate.adtech.worker.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SharedInfoTest {

  private static final String DEFAULT_VERSION = "";

  private static final String ATTRIBUTION_REPORTING_API = "attribution-reporting";

  private static final String PRIVACY_BUDGET_KEY_1 = "test_privacy_budget_key";

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String VERSION_ZERO_DOT_ONE = "0.1";

  private static final String DESTINATION = "https://www.destination.com";

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION_CHROME_GOLDEN_REPORT = "https://conversion.test";

  private static final String REPORTING_ORIGIN_CHROME_GOLDEN_REPORT = "https://report.test";

  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin + destination +
   * source_registration_time
   */
  private static final String PRIVACY_BUDGET_KEY_2 =
      "23f305727d53223a10d0a5a0e2132ea261a5a266524d5c14c4a61bd3f3e60a40";

  /**
   * Privacy Budget Key generated based on Chrome Golden Report -
   * https://source.chromium.org/chromium/chromium/src/+/main:content/test/data/attribution_reporting/aggregatable_report_goldens/latest/report_1.json
   */
  private static final String PRIVACY_BUDGET_KEY_CHROME_GOLDEN_REPORT =
      "b8f9b95599410466fb20e1daeb33ac83ffe05450a43c25e1c3ba9857f4d13063";

  /** Test to verify Privacy Budget Key is pickup correctly from Shared Info */
  @Test
  public void testGetPrivacyBudgetKeyWithPBKInSharedInfo() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.getPrivacyBudgetKey(), PRIVACY_BUDGET_KEY_1);
  }

  /** Test to verify Privacy Budget Key is generated correctly from Shared Info */
  @Test
  public void testGetPrivacyBudgetKeyWithoutPBKInSharedInfo() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.getPrivacyBudgetKey(), PRIVACY_BUDGET_KEY_2);
  }

  /** Test to verify Privacy Budget Key is correctly generated for Chrome Golden Report */
  @Test
  public void testGetPrivacyBudgetKeyForChromeGoldenReport() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.getPrivacyBudgetKey(), PRIVACY_BUDGET_KEY_CHROME_GOLDEN_REPORT);
  }

  /** Test to verify Privacy Budget Key generated for two SharedInfo with same fields is same */
  @Test
  public void testMatchPrivacyBudgetKeyForTwoSharedInfos() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si1 = sharedInfoBuilder1.build();

    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si2 = sharedInfoBuilder2.build();

    assertEquals(si1.getPrivacyBudgetKey(), si2.getPrivacyBudgetKey());
  }

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is enabled */
  @Test
  public void testSetAndGetReportDebugModeEnabled() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugMode(true);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString(), "enabled");
    assertTrue(si.getReportDebugMode());
  }

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is disabled */
  @Test
  public void testSetAndGetReportDebugModeDisabled() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugMode(false);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString(), "disabled");
    assertFalse(si.getReportDebugMode());
  }

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is default */
  @Test
  public void testSetAndGetReportDebugModeDefault() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString(), "disabled");
    assertFalse(si.getReportDebugMode());
  }

  /**
   * Test to verify setReportDebugModeString has the same result as setReportDebugMode when debug
   * mode is enabled
   */
  @Test
  public void testSetReportDebugModeEnabledTwoTypes() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugModeString("enabled");
    SharedInfo si1 = sharedInfoBuilder1.build();

    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugMode(true);
    SharedInfo si2 = sharedInfoBuilder2.build();

    assertEquals(si1, si2);
  }

  /**
   * Test to verify setReportDebugModeString has the same result as setReportDebugMode when debug
   * mode is disabled
   */
  @Test
  public void testSetReportDebugModeDisabledTwoTypes() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugModeString("disabled");
    SharedInfo si1 = sharedInfoBuilder1.build();

    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setReportDebugMode(false);
    SharedInfo si2 = sharedInfoBuilder2.build();

    assertEquals(si1, si2);
  }
}
