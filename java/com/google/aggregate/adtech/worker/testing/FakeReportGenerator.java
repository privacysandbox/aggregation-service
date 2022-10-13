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

import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

/** Generates fake reports for testing purposes. */
public class FakeReportGenerator {
  /** Generates fake facts for testing purposes. */
  public static class FakeFactGenerator {
    /** Generates fake fact with bucket=bucket, value=value for testing purposes. */
    public static Fact generate(int bucket, int value) {
      return Fact.builder().setBucket(createBucketFromInt(bucket)).setValue(value).build();
    }
  }

  /**
   * Given an id, returns the following Report(PrivacyBudgetKey: "1", Destination: "dummy",
   * ReportingOrigin: "dummy", ScheduledReportTime: "1970-01-01 00:00:01", SourceRegistrationTime:
   * "1970-01-01 00:00:01", ReportId: {UUID} Facts: facts
   */
  public static Report generate(ImmutableList<Fact> facts) {
    return Report.builder()
        .setSharedInfo(
            SharedInfo.builder()
                .setPrivacyBudgetKey(String.valueOf("dummy"))
                .setDestination("dummy")
                .setReportingOrigin("dummy")
                .setScheduledReportTime(Instant.EPOCH.plus(1, SECONDS))
                .setSourceRegistrationTime(Instant.EPOCH.plus(1, SECONDS))
                .setReportId(String.valueOf(UUID.randomUUID()))
                .build())
        .setPayload(Payload.builder().addAllFact(facts).build())
        .build();
  }

  /**
   * Given an int param, returns the following if param=1, Report(PrivacyBudgetKey: "1",
   * Destination: "1", ReportingOrigin: "1", ScheduledReportTime: "1970-01-01 00:00:01",
   * SourceRegistrationTime: "1970-01-01 00:00:01", ReportId: {UUID} Facts: [Fact("1", 1)])
   * containing param number of Facts.
   */
  public static Report generate(int param) {
    return Report.builder()
        .setSharedInfo(
            SharedInfo.builder()
                .setPrivacyBudgetKey(String.valueOf(param))
                .setDestination(String.valueOf(param))
                .setReportingOrigin(String.valueOf(param))
                .setScheduledReportTime(Instant.EPOCH.plus(param, SECONDS))
                .setSourceRegistrationTime(Instant.EPOCH.plus(param, SECONDS))
                .setReportId(String.valueOf(UUID.randomUUID()))
                .build())
        .setPayload(
            Payload.builder()
                .addAllFact(
                    IntStream.range(0, param)
                        .mapToObj(
                            i ->
                                Fact.builder()
                                    .setBucket(createBucketFromInt(param))
                                    .setValue(param)
                                    .build())
                        .collect(toImmutableList()))
                .build())
        .build();
  }
  /**
   * generateWithFixedReportId can be used to generate a report with fake fixed report id for the
   * purpose of comparing reports. Other report fields are set based on param Given an int param,
   * returns the following if param=1, Report(PrivacyBudgetKey: "1", Destination: "1",
   * ReportingOrigin: "1", ScheduledReportTime: "1970-01-01 00:00:01", SourceRegistrationTime:
   * "1970-01-01 00:00:01", ReportId: {UUID} Facts: [Fact("1", 1)]) containing param number of
   * Facts.
   */
  public static Report generateWithFixedReportId(int param, String reportId) {
    return Report.builder()
        .setSharedInfo(
            SharedInfo.builder()
                .setPrivacyBudgetKey(String.valueOf(param))
                .setDestination(String.valueOf(param))
                .setReportingOrigin(String.valueOf(param))
                .setScheduledReportTime(Instant.EPOCH.plus(param, SECONDS))
                .setSourceRegistrationTime(Instant.EPOCH.plus(param, SECONDS))
                .setReportId(reportId)
                .build())
        .setPayload(
            Payload.builder()
                .addAllFact(
                    IntStream.range(0, param)
                        .mapToObj(
                            i ->
                                Fact.builder()
                                    .setBucket(createBucketFromInt(param))
                                    .setValue(param)
                                    .build())
                        .collect(toImmutableList()))
                .build())
        .build();
  }

  private FakeReportGenerator() {}
}
