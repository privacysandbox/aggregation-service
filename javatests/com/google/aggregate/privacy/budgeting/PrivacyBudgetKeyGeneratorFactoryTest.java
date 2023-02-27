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
import static com.google.aggregate.adtech.worker.model.SharedInfo.FLEDGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetKeyGeneratorFactoryTest {

  @Test
  public void testGetAttributionReportingPbkGenerator() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
            Optional.of(ATTRIBUTION_REPORTING_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(
        privacyBudgetKeyGenerator.get() instanceof AttributionReportingPrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetFledgePbkGenerator() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(Optional.of(FLEDGE_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(privacyBudgetKeyGenerator.get() instanceof FledgePrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetSharedStoragePbkGenerator() {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
            Optional.of(SHARED_STORAGE_API));

    assertThat(privacyBudgetKeyGenerator).isPresent();
    assertTrue(privacyBudgetKeyGenerator.get() instanceof SharedStoragePrivacyBudgetKeyGenerator);
  }

  @Test
  public void testGetInvalidApiPbkGenerator() {
    assertThat(
            PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(
                Optional.of("invalid-api")))
        .isEqualTo(Optional.empty());
  }
}
