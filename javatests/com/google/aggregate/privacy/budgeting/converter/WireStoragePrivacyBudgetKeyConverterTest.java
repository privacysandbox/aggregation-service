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

package com.google.aggregate.privacy.budgeting.converter;

import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WireStoragePrivacyBudgetKeyConverterTest {

  private static final Instant ORIGINAL_REPORT_TIME = Instant.now();
  private static final String PRIVACY_BUDGET_KEY = "foo";
  private PrivacyBudgetKeyConverter converter;
  private PrivacyBudgetKey wirePrivacyBudgetKey;
  private com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey storagePrivacyBudgetKey;

  @Before
  public void setUp() {
    converter = new WireStoragePrivacyBudgetKeyConverter();
    wirePrivacyBudgetKey =
        PrivacyBudgetKey.builder()
            .setKey(PRIVACY_BUDGET_KEY)
            .setOriginalReportTime(ORIGINAL_REPORT_TIME)
            .build();
    storagePrivacyBudgetKey =
        com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey.builder()
            .setPrivacyBudgetKey(PRIVACY_BUDGET_KEY)
            .setOriginalReportTime(ORIGINAL_REPORT_TIME)
            .build();
  }

  @Test
  public void convertsFromWireToStorageFormat() {
    com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey convertedKey =
        converter.convert(wirePrivacyBudgetKey);

    assertThat(convertedKey).isEqualTo(storagePrivacyBudgetKey);
  }

  @Test
  public void convertsFromStorageToWireFormat() {
    PrivacyBudgetKey convertedKey = converter.reverse().convert(storagePrivacyBudgetKey);

    assertThat(convertedKey).isEqualTo(wirePrivacyBudgetKey);
  }
}
