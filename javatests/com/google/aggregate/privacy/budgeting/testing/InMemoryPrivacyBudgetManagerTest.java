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

package com.google.aggregate.privacy.budgeting.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.privacy.budgeting.model.ConsumedPrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudget;
import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InMemoryPrivacyBudgetManagerTest {

  private static final int PRIVACY_BUDGET = 2;
  private InMemoryPrivacyBudgetManager manager;

  @Before
  public void setUp() {
    manager = new InMemoryPrivacyBudgetManager(PRIVACY_BUDGET);
  }

  @Test
  public void getBudgetExistingKey() {
    PrivacyBudgetKey foo =
        PrivacyBudgetKey.builder().setKey("foo").setOriginalReportTime(Instant.now()).build();
    final int budget = 1;

    manager.setBudget(foo, budget);
    Map<PrivacyBudgetKey, PrivacyBudget> map = manager.getBudget(ImmutableList.of(foo));
    PrivacyBudget privacyBudget = map.get(foo);

    assertThat(privacyBudget.remainingBudget()).isEqualTo(PRIVACY_BUDGET - budget);
  }

  @Test
  public void getBudgetNewKey() {
    PrivacyBudgetKey foo =
        PrivacyBudgetKey.builder().setKey("foo").setOriginalReportTime(Instant.now()).build();

    Map<PrivacyBudgetKey, PrivacyBudget> map = manager.getBudget(ImmutableList.of(foo));
    PrivacyBudget privacyBudget = map.get(foo);

    assertThat(privacyBudget.remainingBudget()).isEqualTo(PRIVACY_BUDGET);
  }

  @Test
  public void consumeBudgetExistingKey() {
    PrivacyBudgetKey foo =
        PrivacyBudgetKey.builder().setKey("foo").setOriginalReportTime(Instant.now()).build();

    manager.setBudget(foo, 1);
    Map<PrivacyBudgetKey, ConsumedPrivacyBudget> consumedBudgetMap =
        manager.consumeBudget(ImmutableList.of(foo));
    ConsumedPrivacyBudget consumedBudget = consumedBudgetMap.get(foo);
    Map<PrivacyBudgetKey, PrivacyBudget> privacyBudgetMap =
        manager.getBudget(ImmutableList.of(foo));
    PrivacyBudget privacyBudget = privacyBudgetMap.get(foo);

    assertThat(consumedBudget.consumedBudget()).isEqualTo(1);
    assertThat(privacyBudget.remainingBudget()).isEqualTo(0);
  }

  @Test
  public void consumeBudgetNewKey() {
    PrivacyBudgetKey foo =
        PrivacyBudgetKey.builder().setKey("foo").setOriginalReportTime(Instant.now()).build();

    Map<PrivacyBudgetKey, ConsumedPrivacyBudget> consumedBudgetMap =
        manager.consumeBudget(ImmutableList.of(foo));
    ConsumedPrivacyBudget consumedBudget = consumedBudgetMap.get(foo);
    Map<PrivacyBudgetKey, PrivacyBudget> privacyBudgetMap =
        manager.getBudget(ImmutableList.of(foo));
    PrivacyBudget privacyBudget = privacyBudgetMap.get(foo);

    assertThat(consumedBudget.consumedBudget()).isEqualTo(1);
    assertThat(privacyBudget.remainingBudget()).isEqualTo(1);
  }

  @Test
  public void consumeBudgetNoBudgetRemaining() {
    PrivacyBudgetKey foo =
        PrivacyBudgetKey.builder().setKey("foo").setOriginalReportTime(Instant.now()).build();

    manager.setBudget(foo, PRIVACY_BUDGET);
    Map<PrivacyBudgetKey, ConsumedPrivacyBudget> consumedBudgetMap =
        manager.consumeBudget(ImmutableList.of(foo));
    ConsumedPrivacyBudget consumedBudget = consumedBudgetMap.get(foo);

    assertThat(consumedBudget.consumedBudget()).isEqualTo(0);
  }
}
