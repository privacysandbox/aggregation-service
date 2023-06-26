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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static org.junit.Assert.assertEquals;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtectedAudiencePrivacyBudgetKeyGeneratorTest {
  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);
  private static final String REPORTING_ORIGIN = "https://www.origin.com";
  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin.
   */
  private static final String PRIVACY_BUDGET_KEY_PROTECTED_AUDIENCE =
      "2ba6169867645673b16783915d6eb47bcfd8a056f27d50f09f7cdc56fec513b0";

  ProtectedAudiencePrivacyBudgetKeyGenerator protectedAudiencePrivacyBudgetKeyGenerator =
      new ProtectedAudiencePrivacyBudgetKeyGenerator();

  @Test
  public void generatePrivacyBudgetKey_forProtectedAudienceAPI() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setApi(PROTECTED_AUDIENCE_API)
            .setVersion(LATEST_VERSION)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN);
    SharedInfo si = sharedInfoBuilder.build();

    String privacyBudgetKey =
        protectedAudiencePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si).get();

    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_PROTECTED_AUDIENCE);
  }

  /**
   * Test to verify Privacy Budget Key generated for two protected audience SharedInfo with same
   * fields is same. This ensures the budget key generator hash is stable.
   */
  @Test
  public void validate_PrivacyBudgetKey_ProtectedAudienceAPI_forSameSharedInfos() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setApi(PROTECTED_AUDIENCE_API)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    SharedInfo si1 = sharedInfoBuilder1.build();
    SharedInfo.Builder sharedInfoBuilder2 =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setApi(PROTECTED_AUDIENCE_API)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME);
    SharedInfo si2 = sharedInfoBuilder2.build();

    String privacyBudgetKey1 =
        protectedAudiencePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si1).get();
    String privacyBudgetKey2 =
        protectedAudiencePrivacyBudgetKeyGenerator.generatePrivacyBudgetKey(si2).get();

    assertEquals(privacyBudgetKey1, privacyBudgetKey2);
  }
}
