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

import static com.google.aggregate.adtech.worker.model.SharedInfo.DEFAULT_VERSION;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
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
   * Generates a Fake Report of the following format:
   *
   * <p>Payload: facts (Default Version) SharedInfo: reportingOrigin: "dummy", reportId: random
   * UUID, privacyBudgetKey: "dummy", destination: "dummy", scheduledReportTime: 1 sec past epoch,
   * sourceRegistrationTime: 1 sec past epoch
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: "dummy", scheduledReportTime: 1 sec past
   * epoch, sourceRegistrationTime: 1 sec past epoch
   *
   * @param facts Facts to populate the Report's Payload with
   * @param reportVersion Version of the report to generate. Empty String for default version,
   *     anything else for latest.
   * @return Fake Report
   */
  public static Report generateWithFactList(ImmutableList<Fact> facts, String reportVersion) {
    return generate(
        Optional.of(facts),
        /* dummyValue */ Optional.empty(),
        /* reportId */ Optional.empty(),
        reportVersion);
  }

  /**
   * Generates a Fake Report of the following format:
   *
   * <p>Payload: dummyValue number of Facts, each with bucket: dummyValue, value: dummyValue
   * (Default Version) SharedInfo: reportingOrigin: dummyValue, reportId: random UUID,
   * privacyBudgetKey: dummyValue, destination: dummyValue, scheduledReportTime: dummyValue sec past
   * epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: dummyValue, scheduledReportTime: dummyValue
   * sec past epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * @param dummyValue a dummy integer value that is set as various applicable values in the
   *     returned Report. See above for where it is specifically used.
   * @param reportVersion Version of the report to generate. Empty String for default version,
   *     anything else for latest.
   * @return
   */
  public static Report generateWithParam(int dummyValue, String reportVersion) {
    return generate(
        /* facts */ Optional.empty(),
        Optional.of(dummyValue),
        /* reportId */ Optional.empty(),
        reportVersion);
  }

  /**
   * Generates a Fake Report of the following format:
   *
   * <p>Payload: dummyValue number of Facts, each with bucket: dummyValue, value: dummyValue
   * (Default Version) SharedInfo: reportingOrigin: dummyValue, reportId: reportVersion,
   * privacyBudgetKey: dummyValue, destination: dummyValue, scheduledReportTime: dummyValue sec past
   * epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: dummyValue, scheduledReportTime: dummyValue
   * sec past epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * @param dummyValue a dummy integer value that is set as various applicable values in the
   *     returned Report. See above for where it is specifically used.
   * @param reportId Value to set the report's ID to.
   * @param reportVersion Version of the report to generate. Empty String for default version,
   *     anything else for latest.
   * @return
   */
  public static Report generateWithFixedReportId(
      int dummyValue, String reportId, String reportVersion) {
    return generate(
        /* facts */ Optional.empty(),
        Optional.of(dummyValue),
        Optional.of(reportId),
        reportVersion);
  }

  /**
   * Internal Fake Report generator. Either facts OR dummyValue must be set, and only exactly one.
   * If reportId is set, dummyValue must be too. See public generate() methods for specifics on the
   * Fake Report that will be generated.
   *
   * @param facts Facts to place inside the Report's Payload. Will be a list of dummy facts if not
   *     set.
   * @param dummyValue Integer value to set many integer-based Report variables to, namely the Time
   *     variables and the number of facts to generate for the payload if `facts` is not set.
   * @param reportId The reportId of the generated Fake Report. If not specified, the Fake Report's
   *     id will be set to a random UUID.
   * @param reportVersion Version of the Fake Report to generate. If set to the default version (aka
   *     an empty String), the privacyBudgetKey will be set to the String equivalent of dummyValue.
   *     Otherwise, it will be auto-generated from other SharedInfo variables.
   * @return A Fake Report generated from the parameter data.
   */
  private static Report generate(
      Optional<ImmutableList<Fact>> facts,
      Optional<Integer> dummyValue,
      Optional<String> reportId,
      String reportVersion) {
    // Sanity check. Evaluates as XNOR to confirm only one of facts or dummyValue is present
    if (!(facts.isPresent() ^ dummyValue.isPresent())) {
      throw new IllegalStateException(
          "Exactly one of the parameters facts or dummyValue must be set");
    }

    Optional<String> paramString = dummyValue.map(number -> String.valueOf(number));
    int dummyValueActual = dummyValue.orElse(1);
    String dummyStringActual = paramString.orElse("dummy");
    Instant dummyTime = Instant.EPOCH.plus(dummyValueActual, SECONDS);

    Report.Builder reportBuilder =
        Report.builder()
            .setPayload(
                Payload.builder()
                    .addAllFact(
                        facts.orElse(
                            IntStream.range(0, dummyValueActual)
                                .mapToObj(
                                    i ->
                                        Fact.builder()
                                            .setBucket(createBucketFromInt(dummyValueActual))
                                            .setValue(dummyValueActual)
                                            .build())
                                .collect(toImmutableList())))
                    .build());

    SharedInfo.Builder sharedInfoBuilder =
        SharedInfo.builder()
            .setDestination(dummyStringActual)
            .setScheduledReportTime(dummyTime)
            .setSourceRegistrationTime(dummyTime)
            .setReportingOrigin(dummyStringActual)
            .setReportId(reportId.orElse(String.valueOf(UUID.randomUUID())));

    if (reportVersion.equals(DEFAULT_VERSION)) {
      sharedInfoBuilder.setPrivacyBudgetKey(dummyStringActual);
    } else {
      /** SharedInfo in latest format */
      sharedInfoBuilder.setVersion("0.1").setApi("attribution-reporting");
    }

    reportBuilder.setSharedInfo(sharedInfoBuilder.build());
    return reportBuilder.build();
  }

  private FakeReportGenerator() {}
}
