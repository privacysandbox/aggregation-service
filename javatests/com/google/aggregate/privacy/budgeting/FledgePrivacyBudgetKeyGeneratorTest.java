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

import static com.google.aggregate.adtech.worker.model.SharedInfo.FLEDGE_API;
import static org.junit.Assert.assertEquals;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FledgePrivacyBudgetKeyGeneratorTest {
  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);
  private static final String REPORTING_ORIGIN = "https://www.origin.com";
  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin.
   */
  private static final String PRIVACY_BUDGET_KEY_1 =
      "02830d9733afd50db718b8b9828f97e6026b6a23f7f85992521f7455bb89d692";

  String FLEDGE_VERSION_0_1 = "0.1";

  FledgePrivacyBudgetKeyGenerator fledgePrivacyBudgetKeyGenerator =
      new FledgePrivacyBudgetKeyGenerator();

  /**
   * Test to verify Privacy Budget Key is generated correctly from Shared Info for Fledge reports.
   */
  @Test
  public void testFledgePrivacyBudgetKeyGenerator() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(FLEDGE_API)
            .setVersion(FLEDGE_VERSION_0_1)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();

    String privacyBudgetKey = fledgePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si).get();

    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_1);
  }

  /**
   * Test to verify Privacy Budget Key generated for two Fledge SharedInfo with same fields is same.
   * This ensures the budget key generator hash is stable.
   */
  @Test
  public void testFledgePrivacyBudgetKeyGeneratorForTwoSharedInfos() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(FLEDGE_VERSION_0_1)
            .setApi(FLEDGE_API)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    SharedInfo si1 = sharedInfoBuilder1.build();
    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(FLEDGE_VERSION_0_1)
            .setApi(FLEDGE_API)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    SharedInfo si2 = sharedInfoBuilder2.build();

    String privacyBudgetKey1 = fledgePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si1).get();
    String privacyBudgetKey2 = fledgePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si2).get();

    assertEquals(privacyBudgetKey1, privacyBudgetKey2);
  }
}
