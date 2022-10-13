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

import com.google.aggregate.privacy.budgeting.model.PrivacyBudget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WireStoragePrivacyBudgetConverterTest {
  private PrivacyBudgetConverter converter;

  @Before
  public void setUp() {
    // Max privacy budget per key is 3.
    converter = new WireStoragePrivacyBudgetConverter();
  }

  @Test
  public void convertsFromWireToStorageFormat() {
    PrivacyBudget wirePrivacyBudget = PrivacyBudget.builder().setRemainingBudget(1).build();

    assertThrows(
        UnsupportedOperationException.class, () -> converter.reverse().convert(wirePrivacyBudget));
  }

  @Test
  public void convertsFromStorageToWireFormat() {
    com.google.aggregate.privacy.budgeting.storage.PrivacyBudget storagePrivacyBudget =
        com.google.aggregate.privacy.budgeting.storage.PrivacyBudget.builder()
            .setConsumedBudget(1)
            .build();

    PrivacyBudget convertedPrivacyBudget = converter.convert(storagePrivacyBudget);

    PrivacyBudget expectedPrivacyBudget = PrivacyBudget.builder().setRemainingBudget(2).build();
    assertThat(convertedPrivacyBudget).isEqualTo(expectedPrivacyBudget);
  }

  @Test
  public void consumedBudgetOverMaxBudget() {
    com.google.aggregate.privacy.budgeting.storage.PrivacyBudget storagePrivacyBudget =
        com.google.aggregate.privacy.budgeting.storage.PrivacyBudget.builder()
            .setConsumedBudget(100)
            .build();

    PrivacyBudget convertedPrivacyBudget = converter.convert(storagePrivacyBudget);

    PrivacyBudget expectedPrivacyBudget = PrivacyBudget.builder().setRemainingBudget(0).build();
    assertThat(convertedPrivacyBudget).isEqualTo(expectedPrivacyBudget);
  }
}
