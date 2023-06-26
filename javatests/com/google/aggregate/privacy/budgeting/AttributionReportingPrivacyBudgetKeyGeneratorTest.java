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
package com.google.aggregate.privacy.budgeting;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.DEFAULT_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AttributionReportingPrivacyBudgetKeyGeneratorTest {
  private static final String PRIVACY_BUDGET_KEY_1 = "test_privacy_budget_key";
  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);
  private static final String DESTINATION = "https://www.destination.com";
  private static final String REPORTING_ORIGIN = "https://www.origin.com";
  private static final String DESTINATION_CHROME_GOLDEN_REPORT = "https://conversion.test";
  private static final String REPORTING_ORIGIN_CHROME_GOLDEN_REPORT = "https://report.test";
  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin + destination +
   * source_registration_time.
   */
  private static final String PRIVACY_BUDGET_KEY_2 =
      "7b16c743c6ffe44bc559561b4f457fd3dcf91b797446ed6dc6b01d9fb32d3565";
  /**
   * Privacy Budget Key generated based on Chrome Golden Report -
   * https://source.chromium.org/chromium/chromium/src/+/main:content/test/data/attribution_reporting/aggregatable_report_goldens/latest/report_1.json
   */
  private static final String PRIVACY_BUDGET_KEY_CHROME_GOLDEN_REPORT =
      "399bd3cd2282959381e4ad6858c5f434285ec70252b5a446808815780d36140f";

  AttributionReportingPrivacyBudgetKeyGenerator attributionReportingPrivacyBudgetKeyGenerator =
      new AttributionReportingPrivacyBudgetKeyGenerator();
  /** Test to verify Privacy Budget Key is generated correctly from Shared Info for VERSION_0_0. */
  @Test
  public void testAttributionReportingPrivacyBudgetKeyGeneratorWithPBKInSharedInfo() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();
    String privacyBudgetKey =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si).get();
    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_1);
  }

  /** Test to verify Privacy Budget Key is generated correctly from Shared Info for VERSION_0_1. */
  @Test
  public void testAttributionReportingPrivacyBudgetKeyGeneratorWithoutPBKInSharedInfo() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();

    String privacyBudgetKey =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si).get();

    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_2);
  }

  /** Test to verify Privacy Budget Key is correctly generated for Chrome Golden Report. */
  @Test
  public void testAttributionReportingPrivacyBudgetKeyGeneratorChromeGoldenReport() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si = sharedInfoBuilder.build();

    String privacyBudgetKey =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si).get();

    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_CHROME_GOLDEN_REPORT);
  }

  /**
   * Test to verify Privacy Budget Key generated for two SharedInfo with same fields is same. This
   * ensures the budget key generator hash is stable.
   */
  @Test
  public void validate_PrivacyBudgetKey_AttributionReportingAPI_forSameSharedInfos() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si1 = sharedInfoBuilder1.build();

    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si2 = sharedInfoBuilder2.build();

    String privacyBudgetKey1 =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si1).get();
    String privacyBudgetKey2 =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si2).get();

    assertEquals(privacyBudgetKey1, privacyBudgetKey2);
  }

  @Test
  public void validate_withSourceRegistrationTimeZero() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(0));
    SharedInfo sharedInfo = sharedInfoBuilder1.build();

    String privacyBudgetKey1 =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(sharedInfo).get();

    assertThat(privacyBudgetKey1).isNotEmpty();
  }

  @Test
  public void validate_withSourceRegistrationTimeNegative() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(-900));
    SharedInfo sharedInfo = sharedInfoBuilder1.build();

    String privacyBudgetKey1 =
        attributionReportingPrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(sharedInfo).get();

    assertThat(privacyBudgetKey1).isNotEmpty();
  }

  @Test
  public void validate_withoutSourceRegistrationTime_throwsException() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400));
    SharedInfo sharedInfo = sharedInfoBuilder1.build();

    assertThrows(
        NoSuchElementException.class,
        () ->
            attributionReportingPrivacyBudgetKeyGenerator
                .generatePrivacyBudgetKey(sharedInfo)
                .get());
  }
}
