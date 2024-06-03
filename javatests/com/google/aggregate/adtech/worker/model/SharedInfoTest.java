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
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
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

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is enabled */
  @Test
  public void sharedInfo_withDebugModeEnabled() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .setReportDebugMode(true);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString().get(), "enabled");
    assertTrue(si.getReportDebugMode());
  }

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is disabled */
  @Test
  public void sharedInfo_withDebugModeDisabled() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .setReportDebugMode(false);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString(), Optional.empty());
    assertFalse(si.getReportDebugMode());
  }

  /** Test to verify the correctness of set/getReportDebugMode when debug mode is default */
  @Test
  public void sharedInfo_withDebugModeDefault() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo si = sharedInfoBuilder.build();

    assertEquals(si.reportDebugModeString(), Optional.empty());
    assertFalse(si.getReportDebugMode());
  }

  /**
   * Test to verify setReportDebugModeString has the same result as setReportDebugMode when debug
   * mode is enabled
   */
  @Test
  public void sharedInfo_withDebugModeEnabledInTwoWays() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .setReportDebugModeString("enabled");
    SharedInfo si1 = sharedInfoBuilder1.build();

    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .setReportDebugMode(true);
    SharedInfo si2 = sharedInfoBuilder2.build();

    assertEquals(si1, si2);
  }

  @Test
  public void sharedInfo_withAttributionReportingAPIType() {
    SharedInfo.Builder sharedInfoAttributionReportingBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo sharedInfoAttributionReporting = sharedInfoAttributionReportingBuilder.build();

    assertEquals(sharedInfoAttributionReporting.api().get(), ATTRIBUTION_REPORTING_API);
  }

  /**
   * Verifies that both V0.1 and V1.0 of the Attribution Reporting Debug API work with SharedInfo.
   */
  @Test
  public void sharedInfo_withAttributionReportingDebugAPIType() {
    SharedInfo attributionReportingDebug1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .build();

    SharedInfo attributionReportingDebug2 =
        SharedInfo.builder()
            .setVersion(VERSION_1_0)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .build();

    assertThat(attributionReportingDebug1.api()).hasValue(ATTRIBUTION_REPORTING_DEBUG_API);
    assertThat(attributionReportingDebug2.api()).hasValue(ATTRIBUTION_REPORTING_DEBUG_API);
  }

  @Test
  public void sharedInfo_withProtectedAudienceAPIType() {
    SharedInfo.Builder sharedInfoSharedStorageBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(PROTECTED_AUDIENCE_API)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID);

    SharedInfo sharedInfoSharedStorage = sharedInfoSharedStorageBuilder.build();

    assertEquals(sharedInfoSharedStorage.api().get(), PROTECTED_AUDIENCE_API);
  }

  @Test
  public void sharedInfo_withSharedStorageAPIType() {
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
