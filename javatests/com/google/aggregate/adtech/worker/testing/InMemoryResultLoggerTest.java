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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatableInputBudgetConsumptionInfo;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.AvroDebugResultsSerdes;
import com.google.aggregate.adtech.worker.model.serdes.AvroResultsSerdes;
import com.google.aggregate.adtech.worker.model.serdes.PrivacyBudgetExhaustedInfoSerdes;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.protocol.avro.AvroDebugResultsSchemaSupplier;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InMemoryResultLoggerTest {
  @Rule public final Acai acai = new Acai(InMemoryResultLoggerTest.TestEnv.class);

  @Inject AvroResultsSerdes avroResultsSerdes;
  @Inject AvroDebugResultsSerdes avroDebugResultsSerdes;
  @Inject PrivacyBudgetExhaustedInfoSerdes privacyBudgetExhaustedInfoSerdes;

  // Under test
  InMemoryResultLogger inMemoryResultLogger;

  private static final Instant TIME = Instant.ofEpochSecond(1609459200);
  private static final UnsignedLong FILTERING_ID = UnsignedLong.valueOf(1);
  private static final SharedInfo SHARED_INFO =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("destination.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech.com")
          .setScheduledReportTime(TIME)
          .setSourceRegistrationTime(TIME)
          .build();

  private static final AggregatableInputBudgetConsumptionInfo info =
      AggregatableInputBudgetConsumptionInfo.builder()
          .setPrivacyBudgetKeyInput(
              PrivacyBudgetKeyInput.builder()
                  .setSharedInfo(SHARED_INFO)
                  .setFilteringId(FILTERING_ID)
                  .build())
          .build();
  private static final PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo =
      PrivacyBudgetExhaustedInfo.builder()
          .setAggregatableInputBudgetConsumptionInfos(ImmutableSet.of(info))
          .build();

  @Before
  public void setUp() {
    inMemoryResultLogger =
        new InMemoryResultLogger(
            avroResultsSerdes, avroDebugResultsSerdes, privacyBudgetExhaustedInfoSerdes);
  }

  @Test
  public void getAggregation() throws ResultLogException {
    AggregatedFact fact1 = AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 5);
    AggregatedFact fact2 = AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 1);
    ImmutableList<AggregatedFact> fakeAggregatedFacts = ImmutableList.of(fact1, fact2);

    inMemoryResultLogger.logResults(
        fakeAggregatedFacts, FakeJobGenerator.generate("foo"), /* isDebugRun= */ false);

    assertThat(
            inMemoryResultLogger.getMaterializedAggregationResults().getMaterializedAggregations())
        .containsExactly(fact1, fact2);
  }

  @Test
  public void getAggregationWithoutLogging() {
    ResultLogException exception =
        assertThrows(
            ResultLogException.class,
            () -> inMemoryResultLogger.getMaterializedAggregationResults());

    assertThat(exception).hasCauseThat().isInstanceOf(IllegalStateException.class);
    assertThat(exception)
        .hasMessageThat()
        .contains("MaterializedAggregations is null. Maybe results did not get logged.");
  }

  @Test
  public void getDebugAggregation() throws ResultLogException {
    AggregatedFact fact1 = AggregatedFact.create(BigInteger.valueOf(1), /* metric= */ 5, 2L);
    AggregatedFact fact2 = AggregatedFact.create(BigInteger.valueOf(2), /* metric= */ 1, 1L);
    ImmutableList<AggregatedFact> fakeAggregatedFacts = ImmutableList.of(fact1, fact2);

    inMemoryResultLogger.logResults(
        fakeAggregatedFacts, FakeJobGenerator.generate("foo"), /* isDebugRun= */ true);

    assertThat(
            inMemoryResultLogger
                .getMaterializedDebugAggregationResults()
                .getMaterializedAggregations())
        .containsExactly(fact1, fact2);
  }

  @Test
  public void getDebugAggregationWithoutLogging() {
    ResultLogException exception =
        assertThrows(
            ResultLogException.class,
            () -> inMemoryResultLogger.getMaterializedDebugAggregationResults());

    assertThat(exception).hasCauseThat().isInstanceOf(IllegalStateException.class);
    assertThat(exception)
        .hasMessageThat()
        .contains("MaterializedAggregations is null. Maybe results did not get logged.");
  }

  @Test
  public void throwsWhenSetTo() {
    inMemoryResultLogger.setShouldThrow(true);
    ImmutableList<AggregatedFact> aggregatedFacts = ImmutableList.of();
    Job Job = FakeJobGenerator.generate("foo");

    assertThrows(
        ResultLogException.class,
        () -> inMemoryResultLogger.logResults(aggregatedFacts, Job, /* isDebugRun= */ false));
    assertThrows(
        ResultLogException.class,
        () -> inMemoryResultLogger.logResults(aggregatedFacts, Job, /* isDebugRun= */ true));
  }

  @Test
  public void writePrivacyBudgetExhaustedDebuggingInformation_inMemory_succeeds() {
    Job job = FakeJobGenerator.generate("foo");

    inMemoryResultLogger.writePrivacyBudgetExhaustedDebuggingInformation(
        privacyBudgetExhaustedInfo, job, "");

    assertThat(inMemoryResultLogger.hasLogged()).isTrue();
    assertThat(inMemoryResultLogger.getInMemoryPrivacyBudgetExhaustedInfo()).isNotNull();
  }

  @Test
  public void writePrivacyBudgetExhaustedDebuggingInformation_inMemory_throwsWhenSetTo() {
    Job job = FakeJobGenerator.generate("foo");

    inMemoryResultLogger.setShouldThrow(true);

    assertThrows(
        ResultLogException.class,
        () ->
            inMemoryResultLogger.writePrivacyBudgetExhaustedDebuggingInformation(
                privacyBudgetExhaustedInfo, job, ""));
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(AvroResultsSchemaSupplier.class).toInstance(new AvroResultsSchemaSupplier());
      bind(AvroDebugResultsSchemaSupplier.class).toInstance(new AvroDebugResultsSchemaSupplier());
    }
  }
}
