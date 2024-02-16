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
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetKeyGeneratorFactoryTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Instant FIXED_TIME = Instant.ofEpochSecond(0);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String DESTINATION = "dest.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  @Inject private PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  @Test
  public void testGetAttributionReportingPbkGenerator_withStaticFactory() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
            Optional.of(ATTRIBUTION_REPORTING_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(
        privacyBudgetKeyGenerator.get()
            instanceof
            com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreporting
                .V1PrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetProtectedAudiencePbkGenerator_withStaticFactory() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
            Optional.of(PROTECTED_AUDIENCE_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(
        privacyBudgetKeyGenerator.get()
            instanceof
            com.google.aggregate.privacy.budgeting.budgetkeygenerator.protectedaudience
                .V1PrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetSharedStoragePbkGenerator_withStaticFactory() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
            Optional.of(SHARED_STORAGE_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(
        privacyBudgetKeyGenerator.get()
            instanceof
            com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage
                .V1PrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetInvalidApiPbkGenerator_withStaticFactory() {
    assertThat(
            PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
                Optional.of("invalid-api")))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getSharedStoragePbkGenerator() {
    SharedInfo sharedInfoV1 = buildSharedInfo(/* api= */ SHARED_STORAGE_API, /* version= */ "0.9");
    validatePrivacyGeneratorClass(
        sharedInfoV1,
        com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage
            .V1PrivacyBudgetKeyGenerator.class);

    SharedInfo sharedInfoV2 = buildSharedInfo(/* api= */ SHARED_STORAGE_API, /* version= */ "1.0");
    validatePrivacyGeneratorClass(
        sharedInfoV2,
        com.google.aggregate.privacy.budgeting.budgetkeygenerator.sharedstorage
            .V2PrivacyBudgetKeyGenerator.class);
  }

  @Test
  public void getProtectedAudienceGenerator() {
    SharedInfo sharedInfoV1 =
        buildSharedInfo(/* api= */ PROTECTED_AUDIENCE_API, /* version= */ "0.1");
    validatePrivacyGeneratorClass(
        sharedInfoV1,
        /* expectedGeneratorClass= */ com.google.aggregate.privacy.budgeting.budgetkeygenerator
            .protectedaudience.V1PrivacyBudgetKeyGenerator.class);

    SharedInfo sharedInfoV2 =
        buildSharedInfo(/* api= */ PROTECTED_AUDIENCE_API, /* version= */ "1.0");
    validatePrivacyGeneratorClass(
        sharedInfoV2,
        /* expectedGeneratorClass= */ com.google.aggregate.privacy.budgeting.budgetkeygenerator
            .protectedaudience.V2PrivacyBudgetKeyGenerator.class);
  }

  @Test
  public void getAttributionReportingGenerator() {
    SharedInfo sharedInfoV1 =
        buildSharedInfo(/* api= */ ATTRIBUTION_REPORTING_API, /* version= */ "0.1");
    validatePrivacyGeneratorClass(
        sharedInfoV1,
        /* expectedGeneratorClass= */ com.google.aggregate.privacy.budgeting.budgetkeygenerator
            .attributionreporting.V1PrivacyBudgetKeyGenerator.class);

    SharedInfo sharedInfoV2 =
        buildSharedInfo(/* api= */ ATTRIBUTION_REPORTING_API, /* version= */ "1.0");
    validatePrivacyGeneratorClass(
        sharedInfoV2,
        /* expectedGeneratorClass= */ com.google.aggregate.privacy.budgeting.budgetkeygenerator
            .attributionreporting.V2PrivacyBudgetKeyGenerator.class);
  }

  @Test
  public void getPBKGenerator_forInvalidAPI_throwsIllegalArgument() {
    SharedInfo sharedInfo = buildSharedInfo(/* api= */ "invalid-api", /* version= */ "0.1");

    assertThrows(
        IllegalArgumentException.class,
        () -> privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(sharedInfo));
  }

  @Test
  public void getPBKGenerator_forInvalidVersion_throwsIllegalArgument() {
    SharedInfo sharedInfo = buildSharedInfo(/* api= */ SHARED_STORAGE_API, /* version= */ "-2.0");

    assertThrows(
        AssertionError.class,
        () -> privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(sharedInfo));
  }

  private static SharedInfo buildSharedInfo(String api, String version) {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setDestination(DESTINATION)
            .setSourceRegistrationTime(FIXED_TIME)
            .setReportId(RANDOM_UUID)
            .setReportDebugMode(true)
            .setApi(api)
            .setVersion(version)
            .build();
    return sharedInfo;
  }

  private void validatePrivacyGeneratorClass(SharedInfo sharedInfo, Class expectedGeneratorClass) {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(sharedInfo);

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertThat(privacyBudgetKeyGenerator.get()).isInstanceOf(expectedGeneratorClass);
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
