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

package com.google.aggregate.adtech.worker.aggregation.engine;

import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator.FakeFactGenerator;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AggregationEngineTest {

  // Under test.
  private AggregationEngine engine;

  @Before
  public void setUp() {
    engine = AggregationEngine.create();
  }

  @Test
  public void oneReportOneFact() {
    Report report =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 1, /* reportVersion */ SharedInfo.LATEST_VERSION);

    engine.accept(report);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(1),
            AggregatedFact.create(/* key= */ createBucketFromInt(1), /* value= */ 1));
  }

  @Test
  public void twoReportNoFacts() {
    ImmutableList<Fact> factList = ImmutableList.of();
    Report firstReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ factList, /* reportVersion */ SharedInfo.LATEST_VERSION);
    Report secondReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ factList, /* reportVersion */ SharedInfo.LATEST_VERSION);

    engine.accept(firstReport);
    engine.accept(secondReport);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation).isEmpty();
  }

  @Test
  public void twoReportSameFactKey() {
    Fact firstReportFact = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 2);
    Fact secondReportFact = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 5);
    Report firstReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ ImmutableList.of(firstReportFact), /* reportVersion */
            SharedInfo.LATEST_VERSION);
    Report secondReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ ImmutableList.of(secondReportFact), /* reportVersion */
            SharedInfo.LATEST_VERSION);

    engine.accept(firstReport);
    engine.accept(secondReport);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(2), AggregatedFact.create(createBucketFromInt(2), /* value= */ 7));
  }

  @Test
  public void oneReportMultipleFacts() {
    Fact firstFact = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2);
    Fact secondFact = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 5);
    Fact thirdFact = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 10);
    Report report =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ ImmutableList.of(firstFact, secondFact, thirdFact), /* reportVersion */
            SharedInfo.LATEST_VERSION);

    engine.accept(report);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(1), AggregatedFact.create(createBucketFromInt(1), /* value= */ 7),
            createBucketFromInt(2), AggregatedFact.create(createBucketFromInt(2), /* value= */ 10));
  }

  @Test
  public void multipleReportsMultipleFacts() {
    Fact firstReportFirstFact = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2);
    Fact firstReportSecondFact = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 4);
    Fact firstReportThirdFact = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 1);
    Fact secondReportFirstFact = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 6);
    Fact secondReportSecondFact = FakeFactGenerator.generate(/* bucket= */ 3, /* value= */ 10);
    Fact secondReportThirdFact = FakeFactGenerator.generate(/* bucket= */ 4, /* value= */ 20);
    Report firstReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ ImmutableList.of(
                firstReportFirstFact,
                firstReportSecondFact,
                firstReportThirdFact), /* reportVersion */
            SharedInfo.LATEST_VERSION);
    Report secondReport =
        FakeReportGenerator.generateWithFactList(
            /* facts= */ ImmutableList.of(
                secondReportFirstFact,
                secondReportSecondFact,
                secondReportThirdFact), /* reportVersion */
            SharedInfo.LATEST_VERSION);

    engine.accept(firstReport);
    engine.accept(secondReport);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(1),
            AggregatedFact.create(createBucketFromInt(1), /* value= */ 6),
            createBucketFromInt(2),
            AggregatedFact.create(createBucketFromInt(2), /* value= */ 7),
            createBucketFromInt(3),
            AggregatedFact.create(createBucketFromInt(3), /* value= */ 10),
            createBucketFromInt(4),
            AggregatedFact.create(createBucketFromInt(4), /* value= */ 20));
  }

  @Test
  public void makeAggregation_nullReportsfilteredOut() {
    // Unlike reports without facts, null reports have facts with both key and value set to 0. They
    // are filtered out by the service.
    Report firstNullReport = FakeReportGenerator.generateNullReport();
    Report secondNullReport = FakeReportGenerator.generateNullReport();

    engine.accept(firstNullReport);
    engine.accept(secondNullReport);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation).isEmpty();
  }

  @Test
  public void makeAggregation_multipleReportsWithSomeNullReports() {
    Fact regularFact1 = FakeFactGenerator.generate(1, 2);
    Fact regularFact2 = FakeFactGenerator.generate(1, 4);
    Fact regularFact3 = FakeFactGenerator.generate(2, 1);
    Fact regularFact4 = FakeFactGenerator.generate(2, 6);
    Fact nullFact1 = Fact.builder().setBucket(BigInteger.ZERO).setValue(0).build();
    Fact nullFact2 = Fact.builder().setBucket(BigInteger.ZERO).setValue(0).build();
    Report regularReport =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(regularFact1, regularFact2, regularFact3), SharedInfo.LATEST_VERSION);
    Report reportWithSomeNullFacts =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(regularFact4, nullFact1, nullFact2), SharedInfo.LATEST_VERSION);
    // Reports with facts having both key and value set to 0.
    Report firstNullReport = FakeReportGenerator.generateNullReport();
    Report secondNullReport = FakeReportGenerator.generateNullReport();

    engine.accept(regularReport);
    engine.accept(reportWithSomeNullFacts);
    engine.accept(firstNullReport);
    engine.accept(secondNullReport);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(1),
            AggregatedFact.create(createBucketFromInt(1), 6),
            createBucketFromInt(2),
            AggregatedFact.create(createBucketFromInt(2), 7));
  }

  @Test
  public void privacyBudgetUnits() {
    Report report =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 1, /* reportVersion */ SharedInfo.LATEST_VERSION);
    Report reportDuplicate =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 1, /* reportVersion */ SharedInfo.LATEST_VERSION);
    Report secondReport =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 4000, /* reportVersion */ SharedInfo.LATEST_VERSION);
    Report thirdReport =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 100, /* reportVersion */ SharedInfo.LATEST_VERSION);

    engine.accept(report);
    engine.accept(reportDuplicate);
    engine.accept(secondReport);
    engine.accept(thirdReport);
    ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits = engine.getPrivacyBudgetUnits();

    Instant hourZero = Instant.parse("1970-01-01T00:00:00Z");
    Instant hourOne = Instant.parse("1970-01-01T01:00:00Z");
    assertThat(privacyBudgetUnits)
        .containsExactly(
            budgetUnit(
                "feb6671c7739adeb5140f2af92bb345545e8f16e1761292ac871eaae7904393f", hourZero),
            budgetUnit(
                "089ddac6c5bd89cc488d35924ce6416520f63d9a9f3ecfe53e97fd570d2c4f62", hourZero),
            budgetUnit(
                "43a9149aa0326808345d5f9b780c48f823d7aba37d72bf4decbba387bf3a283d", hourOne));
  }

  private static PrivacyBudgetUnit budgetUnit(String key, Instant scheduledTime) {
    return PrivacyBudgetUnit.create(key, scheduledTime);
  }
}
