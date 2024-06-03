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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.truth.Truth.assertThat;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator.FakeFactGenerator;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeReportGeneratorTest {

  @Test
  public void testGenerateFakeFact() {
    int id = 1;
    int val = 2;

    Fact fakeFact = FakeFactGenerator.generate(id, val);

    assertThat(fakeFact)
        .isEqualTo(Fact.builder().setBucket(createBucketFromInt(id)).setValue(val).build());
  }

  @Test
  public void testGenerate_reportId_version_0_1() {
    String reportId = "My Report ID";
    int dummyValue = 2;

    Report generatedReport =
        FakeReportGenerator.generateWithFixedReportId(
            dummyValue, reportId, /* reportVersion */ "0.1");

    assertThat(generatedReport.sharedInfo().reportId()).isEqualTo(Optional.of(reportId));
  }

  @Test
  public void testGenerate_FakeReportList_version_0_1() {
    // Setup.
    int id1 = 1;
    int val1 = 1;

    int id2 = 2;
    int val2 = 2;

    ImmutableList<Fact> factList =
        ImmutableList.of(
            FakeFactGenerator.generate(id1, val1), FakeFactGenerator.generate(id2, val2));

    // Invocation.
    Report generatedReport = FakeReportGenerator.generateWithFactList(factList, VERSION_0_1);

    // Assert.
    assertThat(generatedReport)
        .isEqualTo(
            Report.builder()
                .setSharedInfo(
                    SharedInfo.builder()
                        .setVersion("0.1")
                        .setApi("attribution-reporting")
                        .setDestination("dummy")
                        .setReportingOrigin("https://foo.com")
                        .setScheduledReportTime(Instant.EPOCH.plus(1, SECONDS))
                        .setSourceRegistrationTime(Instant.EPOCH.plus(1, SECONDS))
                        .setReportId(generatedReport.sharedInfo().reportId().get())
                        .build())
                .setPayload(
                    Payload.builder()
                        .addFact(FakeFactGenerator.generate(id1, val1))
                        .addFact(FakeFactGenerator.generate(id2, val2))
                        .build())
                .build());
  }

  @Test
  public void testGenerate_version_0_1() {
    int id = 2;

    Report generatedReport =
        FakeReportGenerator.generateWithParam(id, VERSION_0_1, "https://foo.com");

    assertThat(generatedReport)
        .isEqualTo(
            Report.builder()
                .setSharedInfo(
                    SharedInfo.builder()
                        .setVersion("0.1")
                        .setApi("attribution-reporting")
                        .setDestination(String.valueOf(id))
                        .setReportingOrigin("https://foo.com")
                        .setScheduledReportTime(Instant.EPOCH.plus(id, SECONDS))
                        .setSourceRegistrationTime(Instant.EPOCH.plus(id, SECONDS))
                        .setReportId(generatedReport.sharedInfo().reportId().get())
                        .build())
                .setPayload(
                    Payload.builder()
                        .addFact(FakeFactGenerator.generate(id, id))
                        .addFact(FakeFactGenerator.generate(id, id))
                        .build())
                .build());
  }

  @Test
  public void generateNullReport() {
    Report generatedReport = FakeReportGenerator.generateNullReport();

    // Assert.
    assertThat(generatedReport)
        .isEqualTo(
            Report.builder()
                .setSharedInfo(
                    SharedInfo.builder()
                        .setVersion(LATEST_VERSION)
                        .setDestination("dummy")
                        .setReportingOrigin("https://foo.com")
                        .setScheduledReportTime(Instant.EPOCH.plus(1, SECONDS))
                        .setSourceRegistrationTime(Instant.EPOCH.plus(1, SECONDS))
                        .setReportId(generatedReport.sharedInfo().reportId().get())
                        .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
                        .build())
                .setPayload(
                    Payload.builder()
                        .addFact(Fact.builder().setBucket(BigInteger.ZERO).setValue(0).build())
                        .build())
                .build());
  }
}
