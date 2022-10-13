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
import static org.junit.Assert.assertThrows;

import com.google.aggregate.privacy.budgeting.model.ConsumedPrivacyBudget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WireStorageConsumedPrivacyBudgetConverterTest {

  private static final int CONSUMED_BUDGET = 1;
  private ConsumedPrivacyBudgetConverter converter;

  @Before
  public void setUp() {
    converter = new WireStorageConsumedPrivacyBudgetConverter();
  }

  @Test
  public void convertsFromWireToStorageFormat() {
    ConsumedPrivacyBudget wireConsumedPrivacyBudget =
        ConsumedPrivacyBudget.builder().setConsumedBudget(CONSUMED_BUDGET).build();

    assertThrows(
        UnsupportedOperationException.class,
        () -> converter.reverse().convert(wireConsumedPrivacyBudget));
  }

  @Test
  public void convertsFromStorageToWireFormat() {
    com.google.aggregate.privacy.budgeting.storage.ConsumedPrivacyBudget
        storageConsumedPrivacyBudget =
            com.google.aggregate.privacy.budgeting.storage.ConsumedPrivacyBudget.builder()
                .setConsumedBudget(CONSUMED_BUDGET)
                .build();

    ConsumedPrivacyBudget convertedConsumedPrivacyBudget =
        converter.convert(storageConsumedPrivacyBudget);

    ConsumedPrivacyBudget expectedConsumedPrivacyBudget =
        ConsumedPrivacyBudget.builder().setConsumedBudget(CONSUMED_BUDGET).build();
    assertThat(convertedConsumedPrivacyBudget).isEqualTo(expectedConsumedPrivacyBudget);
  }
}
