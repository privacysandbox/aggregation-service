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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.DEFAULT_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.FLEDGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SharedInfoTest {

  private static final String PRIVACY_BUDGET_KEY_1 = "test_privacy_budget_key";

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

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

    assertEquals(si.reportDebugModeString().get(), "enabled");
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

    assertEquals(si.reportDebugModeString(), Optional.empty());
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

    assertEquals(si.reportDebugModeString(), Optional.empty());
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

  /** Tests to verify Attribution reporting API type in SharedInfo */
  @Test
  public void testSharedInfoApiTypeAttributionReporting() {
    SharedInfo.Builder sharedInfoMissingApiBuilder =
        SharedInfo.builder()
            .setVersion(DEFAULT_VERSION)
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo.Builder sharedInfoAttributionReportingBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo sharedInfoMissingApi = sharedInfoMissingApiBuilder.build();
    SharedInfo sharedInfoAttributionReporting = sharedInfoAttributionReportingBuilder.build();

    assertEquals(sharedInfoMissingApi.api(), Optional.empty());
    assertEquals(sharedInfoAttributionReporting.api().get(), ATTRIBUTION_REPORTING_API);
  }

  /** Tests to verify Fledge API type in SharedInfo */
  @Test
  public void testSharedInfoApiTypeFledge() {
    SharedInfo.Builder sharedInfoFledgeBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(FLEDGE_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo sharedInfoFledge = sharedInfoFledgeBuilder.build();

    assertEquals(sharedInfoFledge.api().get(), FLEDGE_API);
  }

  /** Tests to verify Shared Storage API type in SharedInfo */
  @Test
  public void testSharedInfoApiTypeSharedStorage() {
    SharedInfo.Builder sharedInfoSharedStorageBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(SHARED_STORAGE_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo sharedInfoSharedStorage = sharedInfoSharedStorageBuilder.build();

    assertEquals(sharedInfoSharedStorage.api().get(), SHARED_STORAGE_API);
  }
}
