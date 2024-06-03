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
package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_1_0;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug.PrivacyBudgetKeyGeneratorModule.AttributionReportingDebugPrivacyBudgetKeyGenerators;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug.V1PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug.V2PrivacyBudgetKeyGenerator;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.time.Instant;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AttributionReportingDebugPrivacyBudgetKeyGeneratorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);
  private static final String DESTINATION = "https://www.destination.com";
  private static final String REPORTING_ORIGIN = "https://www.origin.com";
  private static final UnsignedLong FILTERING_ID = UnsignedLong.valueOf(123);

  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin + destination +
   * source_registration_time. Actual values used for generating key for V1 :
   *
   * <p>UTF-8 Input = ATTRIBUTION_REPORTING_DEBUG_API + {Version} + REPORTING_ORIGIN + DESTINATION +
   * FIXED_TIME.
   */
  private static final String PRIVACY_BUDGET_KEY_DEBUG_V1 =
      "56f9dee8fe909ddc293084429e614cc272b8a79090577a7d566aea140ff087a4";

  /**
   * Actual values used for generating key for V2 :
   *
   * <p>UTF-8 Input = ATTRIBUTION_REPORTING_DEBUG_API + {Version} + REPORTING_ORIGIN + DESTINATION +
   * FIXED_TIME + FILTERING_ID.
   */
  private static final String PRIVACY_BUDGET_KEY_DEBUG_V2 =
      "768bf2420536f93b4cf25ea907711eff43e12e600aa9c7099a7d3aa095b4152f";

  @Inject @AttributionReportingDebugPrivacyBudgetKeyGenerators
  VersionedPrivacyBudgetKeyGeneratorProvider versionedPrivacyBudgetKeyGeneratorProvider;

  private static final PrivacyBudgetKeyGenerator V1_PRIVACY_BUDGET_KEY_GENERATOR =
      new V1PrivacyBudgetKeyGenerator();

  private static final PrivacyBudgetKeyGenerator V2_PRIVACY_BUDGET_KEY_GENERATOR =
      new V2PrivacyBudgetKeyGenerator();

  /**
   * Test to verify Privacy Budget Key is generated correctly from Shared Info with API VERSION_0_1.
   */
  @Test
  public void generatePrivacyBudgetKey_forV1_succeeds() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    assertEquals(
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput),
        PRIVACY_BUDGET_KEY_DEBUG_V1);
  }

  /**
   * Test to verify Privacy Budget Key is generated correctly from Shared Info with API VERSION_1_0.
   */
  @Test
  public void generatePrivacyBudgetKey_forV2_succeeds() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(VERSION_1_0)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(FILTERING_ID)
            .build();

    assertEquals(
        V2_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput),
        PRIVACY_BUDGET_KEY_DEBUG_V2);
  }

  @Test
  public void zeroSourceRegistrationTime_generatesValidKey() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(0));
    SharedInfo sharedInfo = sharedInfoBuilder1.build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey1 =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey1).isNotEmpty();
  }

  @Test
  public void negativeSourceRegistrationTime_generatesValidKey() {
    SharedInfo.Builder sharedInfoBuilder1 =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(-900));
    SharedInfo sharedInfo = sharedInfoBuilder1.build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey1 =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey1).isNotEmpty();
  }

  @Test
  public void generatorV1_withoutSourceRegistrationTime_generatesValidKey() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400));
    SharedInfo sharedInfo = sharedInfoBuilder.build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey).isNotEmpty();
  }

  @Test
  public void generatorV2_withoutSourceRegistrationTime_generatesValidKey() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(VERSION_1_0)
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(UnsignedLong.valueOf(456))
            .build();

    String privacyBudgetKey =
        V2_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey).isNotEmpty();
  }

  @Test
  public void versionedPBKProvider_providesExactlyOneGeneratorPerVersion() {
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 0.1, UnsignedLong.ZERO);
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 0.98765, UnsignedLong.ZERO);

    assertExactlyOnePBKGeneratorForVersion(/* version= */ 1.0, UnsignedLong.ZERO);
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 0.98765, UnsignedLong.ONE);
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 1.0, UnsignedLong.valueOf(5));
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 5.987, UnsignedLong.valueOf(5));
    assertExactlyOnePBKGeneratorForVersion(/* version= */ 19.678, UnsignedLong.valueOf(5));
  }

  private void assertExactlyOnePBKGeneratorForVersion(double version, UnsignedLong filteringId) {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(String.valueOf(version))
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setDestination(DESTINATION)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .build();
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(filteringId)
            .build();

    assertTrue(
        versionedPrivacyBudgetKeyGeneratorProvider.doesExactlyOneCorrespondingPBKGeneratorExist(
            privacyBudgetKeyInput));
  }

  static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
