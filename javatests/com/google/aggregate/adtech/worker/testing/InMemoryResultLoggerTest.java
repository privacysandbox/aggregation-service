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

import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InMemoryResultLoggerTest {

  // Under test
  InMemoryResultLogger inMemoryResultLogger;

  @Before
  public void setUp() {
    inMemoryResultLogger = new InMemoryResultLogger();
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
  public void logInMemoryReports_logSucceeds() throws ResultLogException {
    EncryptedReport encryptedReport1 =
        EncryptedReport.builder()
            .setPayload(ByteSource.wrap(new byte[] {0x00, 0x01}))
            .setKeyId("key1")
            .setSharedInfo("foo")
            .build();
    EncryptedReport encryptedReport2 =
        EncryptedReport.builder()
            .setPayload(ByteSource.wrap(new byte[] {0x01, 0x02}))
            .setKeyId("key2")
            .setSharedInfo("foo")
            .build();
    ImmutableList<EncryptedReport> encryptedReports =
        ImmutableList.of(encryptedReport1, encryptedReport2);

    inMemoryResultLogger.logReports(encryptedReports, FakeJobGenerator.generate("foo"), "1");

    assertThat(inMemoryResultLogger.getMaterializedEncryptedReports())
        .containsExactly(encryptedReport1, encryptedReport2);
  }

  @Test
  public void logNullReports_throwsException() {
    ResultLogException exception =
        assertThrows(
            ResultLogException.class, () -> inMemoryResultLogger.getMaterializedEncryptedReports());

    assertThat(exception).hasCauseThat().isInstanceOf(IllegalStateException.class);
    assertThat(exception)
        .hasMessageThat()
        .contains("MaterializedEncryptionReports is null. Maybe results did not get logged.");
  }

  @Test
  public void throwsWhenSetTo() {
    inMemoryResultLogger.setShouldThrow(true);
    ImmutableList<AggregatedFact> aggregatedFacts = ImmutableList.of();
    ImmutableList<EncryptedReport> encryptedReports = ImmutableList.of();
    Job Job = FakeJobGenerator.generate("foo");

    assertThrows(
        ResultLogException.class,
        () -> inMemoryResultLogger.logResults(aggregatedFacts, Job, /* isDebugRun= */ false));
    assertThrows(
        ResultLogException.class,
        () -> inMemoryResultLogger.logResults(aggregatedFacts, Job, /* isDebugRun= */ true));
    assertThrows(
        ResultLogException.class,
        () -> inMemoryResultLogger.logReports(encryptedReports, Job, "1"));
  }
}
