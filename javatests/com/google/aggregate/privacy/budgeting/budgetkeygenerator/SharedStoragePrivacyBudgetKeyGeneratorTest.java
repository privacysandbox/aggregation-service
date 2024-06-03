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

import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage.PrivacyBudgetKeyGeneratorModule.SharedStoragePrivacyBudgetKeyGenerators;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage.V1PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage.V2PrivacyBudgetKeyGenerator;
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
public class SharedStoragePrivacyBudgetKeyGeneratorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);
  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  /**
   * String representation of sha256 digest generated using UTF-8 representation of key. Key
   * constructed from shared info fields - api + version + reporting_origin.
   */
  private static final String PRIVACY_BUDGET_KEY_1 =
      "a5f0260eaa89fb0984127e70e396d0d744ac8509db2e92a22e9f5ba8d8360d6b";

  String SHARED_STORAGE_VERSION_0_1 = "0.1";
  String SHARED_STORAGE_VERSION_1_0 = "1.0";

  V1PrivacyBudgetKeyGenerator sharedStoragePrivacyBudgetKeyGenerator =
      new V1PrivacyBudgetKeyGenerator();

  @Inject private PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  @Inject @SharedStoragePrivacyBudgetKeyGenerators
  private VersionedPrivacyBudgetKeyGeneratorProvider versionedPrivacyBudgetKeyGeneratorProvider;

  /**
   * Test to verify Privacy Budget Key is generated correctly from Shared Info for Shared Storage
   * reports.
   */
  @Test
  public void generatePrivacyBudgetKey_forV1() {
    SharedInfo si = buildSharedInfo(SHARED_STORAGE_VERSION_0_1);
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder().setSharedInfo(si).build();

    PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput).get();
    String privacyBudgetKey =
        privacyBudgetKeyGenerator.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKeyGenerator).isInstanceOf(V1PrivacyBudgetKeyGenerator.class);
    assertEquals(privacyBudgetKey, PRIVACY_BUDGET_KEY_1);
  }

  @Test
  public void generatePrivacyBudgetKey_forV2() {
    SharedInfo sharedInfo = buildSharedInfo(SHARED_STORAGE_VERSION_1_0);
    UnsignedLong filteringId = UnsignedLong.valueOf(78);
    String privacyBudgetKeyHashInput =
        String.join(
            "-",
            ImmutableList.of(
                sharedInfo.api().get(),
                sharedInfo.version(),
                sharedInfo.reportingOrigin(),
                String.valueOf(filteringId)));
    String expectedPBK =
        Hashing.sha256()
            .newHasher()
            .putBytes(privacyBudgetKeyHashInput.getBytes(StandardCharsets.UTF_8))
            .hash()
            .toString();
    PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(filteringId)
            .build();

    PrivacyBudgetKeyGenerator privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput).get();
    String privacyBudgetKey =
        privacyBudgetKeyGenerator.generatePrivacyBudgetKey(privacyBudgetKeyInput);

    assertThat(privacyBudgetKeyGenerator).isInstanceOf(V2PrivacyBudgetKeyGenerator.class);
    assertEquals(privacyBudgetKey, expectedPBK);
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
    SharedInfo sharedInfo = buildSharedInfo(version);
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

  private static SharedInfo buildSharedInfo(String version) {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setApi(SHARED_STORAGE_API)
            .setVersion(version)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    return sharedInfo;
  }

  static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
