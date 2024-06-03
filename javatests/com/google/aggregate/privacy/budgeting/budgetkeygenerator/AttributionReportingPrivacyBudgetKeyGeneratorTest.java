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
package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting.PrivacyBudgetKeyGeneratorModule.AttributionReportingPrivacyBudgetKeyGenerators;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting.V1PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting.V2PrivacyBudgetKeyGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AttributionReportingPrivacyBudgetKeyGeneratorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

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

  @Inject PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  @Inject @AttributionReportingPrivacyBudgetKeyGenerators
  VersionedPrivacyBudgetKeyGeneratorProvider versionedPrivacyBudgetKeyGeneratorProvider;

  private static final PrivacyBudgetKeyGenerator V1_PRIVACY_BUDGET_KEY_GENERATOR =
      new V1PrivacyBudgetKeyGenerator();

  private static final PrivacyBudgetKeyGenerator V2_PRIVACY_BUDGET_KEY_GENERATOR =
      new V2PrivacyBudgetKeyGenerator();

  /** Test to verify Privacy Budget Key is generated correctly from Shared Info for VERSION_0_1. */
  @Test
  public void generatePrivacyBudgetKey_forV1() {
    SharedInfo si =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(si).build();

    PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput).get();
    String privacyBudgetKey =
        privacyBudgetKeyGenerator.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKeyGenerator).isInstanceOf(V1PrivacyBudgetKeyGenerator.class);
    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_2);
  }

  @Test
  public void generatePrivacyBudgetKey_forV2() {
    UnsignedLong filteringId = UnsignedLong.valueOf(78);
    SharedInfo si =
        SharedInfo.builder()
            .setVersion("1.0")
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION)
            .setScheduledReportTime(FIXED_TIME)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
            .setSharedInfo(si)
            .setFilteringId(filteringId)
            .build();
    String privacyBudgetKeyHashInput =
        String.join(
            "-",
            ImmutableList.of(
                si.api().get(),
                si.version(),
                si.reportingOrigin(),
                si.destination().get(),
                si.sourceRegistrationTime().get().toString(),
                String.valueOf(filteringId)));
    String expectedPBK =
        Hashing.sha256()
            .newHasher()
            .putBytes(privacyBudgetKeyHashInput.getBytes(StandardCharsets.UTF_8))
            .hash()
            .toString();

    PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput).get();
    String privacyBudgetKey =
        privacyBudgetKeyGenerator.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKeyGenerator).isInstanceOf(V2PrivacyBudgetKeyGenerator.class);
    assertEquals(privacyBudgetKey, expectedPBK);
  }

  /** Test to verify Privacy Budget Key is correctly generated for Chrome Golden Report. */
  @Test
  public void testAttributionReportingPrivacyBudgetKeyGeneratorChromeGoldenReport_forV1() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200));
    SharedInfo si = sharedInfoBuilder.build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(si).build();

    String privacyBudgetKey =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

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
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(
            PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(si1).build());
    String privacyBudgetKey2 =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(
            PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(si2).build());

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
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey1 =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

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
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey1 =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey1).isNotEmpty();
  }

  @Test
  public void validate_withoutSourceRegistrationTime_forV1() {
    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(VERSION_0_1)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400));
    SharedInfo sharedInfo = sharedInfoBuilder.build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo).build();

    String privacyBudgetKey =
        V1_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey).isNotEmpty();
  }

  @Test
  public void validate_withoutSourceRegistrationTime_forV2() {
    UnsignedLong filteringId = UnsignedLong.valueOf(456);
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion("1.0")
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(filteringId)
            .build();

    String privacyBudgetKey =
        V2_PRIVACY_BUDGET_KEY_GENERATOR.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKey).isNotEmpty();
  }

  @Test
  public void versionedPBKProvider_noOverlapsInVersions() {
    assertPBKGeneratorForVersion(
        /* version= */ "0.1", UnsignedLong.ZERO, V1PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "0.679999", UnsignedLong.ZERO, V1PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "0.999999", UnsignedLong.ZERO, V1PrivacyBudgetKeyGenerator.class);

    assertPBKGeneratorForVersion(
        /* version= */ "0.1", UnsignedLong.ONE, V2PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "0.679999", UnsignedLong.ONE, V2PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "0.999999", UnsignedLong.ONE, V2PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "1.0", UnsignedLong.ZERO, V2PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "1.9999", UnsignedLong.ONE, V2PrivacyBudgetKeyGenerator.class);
    assertPBKGeneratorForVersion(
        /* version= */ "167.9999", UnsignedLong.ONE, V2PrivacyBudgetKeyGenerator.class);
  }

  private void assertPBKGeneratorForVersion(
      String version, UnsignedLong filteringId, Class privacyBudgetKeyGeneratorClass) {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(version)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .build();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
            .setFilteringId(filteringId)
            .setSharedInfo(sharedInfo)
            .build();

    assertTrue(
        versionedPrivacyBudgetKeyGeneratorProvider.doesExactlyOneCorrespondingPBKGeneratorExist(
            privacyBudgetKeyInput));
    assertThat(
            privacyBudgetKeyGeneratorFactory
                .getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput)
                .get())
        .isInstanceOf(privacyBudgetKeyGeneratorClass);
  }

  static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
