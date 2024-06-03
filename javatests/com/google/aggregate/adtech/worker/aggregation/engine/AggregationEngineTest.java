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

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator.FakeFactGenerator;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.math.BigInteger;
import java.time.Instant;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AggregationEngineTest {
  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject AggregationEngineFactory aggregationEngineFactory;

  // Under test.
  private AggregationEngine engine;

  @Before
  public void setUp() {
    engine = aggregationEngineFactory.create();
  }

  @Test
  public void oneReportOneFact() {
    Report report =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 1, /* reportVersion */ SharedInfo.LATEST_VERSION, "https://foo.com");

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
            /* bucket= */ 1, /* reportVersion */ SharedInfo.VERSION_0_1, "https://origin1.foo.com");
    Report reportDuplicate =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 1, /* reportVersion */ SharedInfo.VERSION_0_1, "https://origin1.foo.com");
    Report secondReport =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 4000, /* reportVersion */
            SharedInfo.VERSION_0_1,
            "https://origin2.foo.com");
    Report thirdReport =
        FakeReportGenerator.generateWithParam(
            /* bucket= */ 100, /* reportVersion */
            SharedInfo.VERSION_0_1,
            "https://origin3.foo.com");

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
                "686f11a611747492b911f8de3dc514c502246e78cc9d7a82e99d0b5af1cc2594",
                hourZero,
                /* reportingOrigin= */ "https://origin1.foo.com"),
            budgetUnit(
                "02e1093c37d58ed073a379f191289a42db00eb7901c0fb9598ed2e74e6782570",
                hourZero,
                /* reportingOrigin= */ "https://origin3.foo.com"),
            budgetUnit(
                "2b873089e720f2fc134b74cdef5f2edbb0fc4307a4a919036cc0ecd66d53f208",
                hourOne,
                /* reportingOrigin= */ "https://origin2.foo.com"));
  }

  @Test
  public void makeAggregation_withOutFilteringId_forReportsWithoutLabelIds_aggregatesAllTheFacts() {
    Fact factWithoutLabel1 = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2);
    Fact factWithoutLabel2 = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 3);
    Report reportWithoutLabels =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutLabel1, factWithoutLabel2), SharedInfo.VERSION_0_1);

    engine.accept(reportWithoutLabels);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation)
        .containsExactly(createBucketFromInt(1), AggregatedFact.create(createBucketFromInt(1), 5));
  }

  @Test
  public void makeAggregation_withFilteringId_forReportsWithoutLabelIds_aggregatesNone() {
    Fact factWithoutLabel1 = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2);
    Fact factWithoutLabel2 = FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 3);
    Report reportWithoutLabels =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutLabel1, factWithoutLabel2), SharedInfo.VERSION_0_1);

    AggregationEngine engine =
        aggregationEngineFactory.create(
            /* filteringIds= */ ImmutableSet.of(UnsignedLong.valueOf(7)));
    engine.accept(reportWithoutLabels);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();

    assertThat(aggregation).isEmpty();
  }

  @Test
  public void makeAggregation_withoutFilteringId_forReportsWithLabelIds_aggregatesForLabelId0() {
    Fact factWithLabel1 =
        FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2, /* id= */ UnsignedLong.ZERO);
    Fact factWithLabel2 =
        FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 3, /* id= */ UnsignedLong.ZERO);
    Report reportWithLabels1 =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithLabel1, factWithLabel2), /* version= */ "1.0");
    Fact factWithLabel3 =
        FakeFactGenerator.generate(
            /* bucket= */ 1, /* value= */ 2, /* id= */ UnsignedLong.valueOf(3));
    Fact factWithLabel4 =
        FakeFactGenerator.generate(
            /* bucket= */ 1, /* value= */ 3, /* id= */ UnsignedLong.valueOf(2));
    Report reportWithLabels2 =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithLabel3, factWithLabel4), /* version= */ "1.1");

    AggregationEngine engine = aggregationEngineFactory.create();
    engine.accept(reportWithLabels1);
    engine.accept(reportWithLabels2);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();
    ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits = engine.getPrivacyBudgetUnits();

    assertThat(aggregation)
        .containsExactly(createBucketFromInt(1), AggregatedFact.create(createBucketFromInt(1), 5));
    // PBK is taken even for those reports without matching labels.
    assertThat(privacyBudgetUnits).hasSize(2);
  }

  @Test
  public void
      makeAggregation_withFilteringIds_aggregatesForMatchingLabelId_PBKsGeneratedForEveryFilteringId() {
    UnsignedLong matchingFilteringId1 = UnsignedLong.valueOf(12345);
    UnsignedLong matchingFilteringId2 = UnsignedLong.valueOf(123);
    UnsignedLong nonmatchingFilteringId1 = UnsignedLong.ZERO;
    UnsignedLong nonmatchingFilteringId2 = UnsignedLong.valueOf(999);
    Fact factWithMatchingLabel1 =
        FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 2, /* id= */ matchingFilteringId1);
    Fact factWithMatchingLabel2 =
        FakeFactGenerator.generate(/* bucket= */ 1, /* value= */ 3, /* id= */ matchingFilteringId2);
    Report reportsWithMatchingLabelIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithMatchingLabel1, factWithMatchingLabel2),
            /* version= */ "1.00");
    Fact factWithMatchingLabel3 =
        FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 4, /* id= */ matchingFilteringId1);
    Fact factWithNonMatchingLabel1 =
        FakeFactGenerator.generate(
            /* bucket= */ 2, /* value= */ 3, /* id= */ nonmatchingFilteringId1);
    Report reportsWithSomeMatchingLabelIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithMatchingLabel3, factWithNonMatchingLabel1),
            /* version= */ "1.01");
    Fact factWithNonMatchingLabel2 =
        FakeFactGenerator.generate(
            /* bucket= */ 2, /* value= */ 3, /* id= */ nonmatchingFilteringId2);
    Report reportsWithoutMatchingLabelIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithNonMatchingLabel2), /* version= */ "1.02");
    Fact factWithoutLabelId = FakeFactGenerator.generate(/* bucket= */ 2, /* value= */ 3);
    Report reportsWithoutLabelIds =
        FakeReportGenerator.generateWithFactList(
            ImmutableList.of(factWithoutLabelId), /* version= */ "1.03");

    AggregationEngine engine =
        aggregationEngineFactory.create(
            /** filteringIds = */
            ImmutableSet.of(matchingFilteringId1, matchingFilteringId2));
    engine.accept(reportsWithMatchingLabelIds);
    engine.accept(reportsWithSomeMatchingLabelIds);
    engine.accept(reportsWithoutMatchingLabelIds);
    engine.accept(reportsWithoutLabelIds);
    ImmutableMap<BigInteger, AggregatedFact> aggregation = engine.makeAggregation();
    ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits = engine.getPrivacyBudgetUnits();

    assertThat(aggregation)
        .containsExactly(
            createBucketFromInt(1),
            AggregatedFact.create(createBucketFromInt(1), 5),
            createBucketFromInt(2),
            AggregatedFact.create(createBucketFromInt(2), 4));
    // PBK is calculated for every filteringId = 2 filteringId x 4 reports
    assertThat(privacyBudgetUnits).hasSize(8);
  }

  private static PrivacyBudgetUnit budgetUnit(
      String key, Instant scheduledTime, String reportingOrigin) {
    return PrivacyBudgetUnit.create(key, scheduledTime, reportingOrigin);
  }

  static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
