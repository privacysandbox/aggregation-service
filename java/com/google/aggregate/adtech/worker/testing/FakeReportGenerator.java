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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
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

    public static Fact generate(int bucket, int value, UnsignedLong id) {
      return Fact.builder()
          .setId(id)
          .setBucket(createBucketFromInt(bucket))
          .setValue(value)
          .build();
    }
  }

  /**
   * Generates a Fake Report of the following format:
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: "dummy", scheduledReportTime: 1 sec past
   * epoch, sourceRegistrationTime: 1 sec past epoch
   *
   * @param facts Facts to populate the Report's Payload with
   * @param reportVersion Version of the report to generate.
   * @return Fake Report
   */
  public static Report generateWithFactList(ImmutableList<Fact> facts, String reportVersion) {
    return generate(
        Optional.of(facts),
        /* dummyValue */ Optional.empty(),
        /* reportId */ Optional.empty(),
        reportVersion,
        "https://foo.com");
  }

  /**
   * Generates a Fake Report of the following format:
   *
   * <p>Payload: dummyValue number of Facts, each with bucket: dummyValue, value: dummyValue
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: dummyValue, scheduledReportTime: dummyValue
   * sec past epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * @param dummyValue a dummy integer value that is set as various applicable values in the
   *     returned Report. See above for where it is specifically used.
   * @param reportVersion Version of the report to generate.
   * @return
   */
  public static Report generateWithParam(
      int dummyValue, String reportVersion, String reportingOrigin) {
    return generate(
        /* facts */ Optional.empty(),
        Optional.of(dummyValue),
        /* reportId */ Optional.empty(),
        reportVersion,
        reportingOrigin);
  }

  /**
   * Generates a Fake Report of the following format:
   *
   * <p>Payload: dummyValue number of Facts, each with bucket: dummyValue, value: dummyValue
   *
   * <p>(Latest Version) SharedInfo: reportId: auto-generated from the following values, version:
   * "0.1", api: "attribution-reporting", destination: dummyValue, scheduledReportTime: dummyValue
   * sec past epoch, sourceRegistrationTime: dummyValue sec past epoch
   *
   * @param dummyValue a dummy integer value that is set as various applicable values in the
   *     returned Report. See above for where it is specifically used.
   * @param reportId Value to set the report's ID to.
   * @param reportVersion Version of the report to generate.
   * @return
   */
  public static Report generateWithFixedReportId(
      int dummyValue, String reportId, String reportVersion) {
    return generate(
        /* facts */ Optional.empty(),
        Optional.of(dummyValue),
        Optional.of(reportId),
        reportVersion,
        "https://foo.com");
  }

  /**
   * Returns a null report. A null report has facts with key and value set to 0.
   *
   * @return A fake null report having key and value set to 0.
   */
  public static Report generateNullReport() {
    Fact nullFact = Fact.builder().setBucket(BigInteger.ZERO).setValue(0).build();
    return generate(
        Optional.of(ImmutableList.of(nullFact)),
        Optional.empty(),
        Optional.empty(),
        LATEST_VERSION,
        "https://foo.com");
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
   * @param reportVersion Version of the Fake Report to generate.
   * @return A Fake Report generated from the parameter data.
   */
  private static Report generate(
      Optional<ImmutableList<Fact>> facts,
      Optional<Integer> dummyValue,
      Optional<String> reportId,
      String reportVersion,
      String reportingOrigin) {
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

    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setDestination(dummyStringActual)
            .setScheduledReportTime(dummyTime)
            .setSourceRegistrationTime(dummyTime)
            .setReportingOrigin(reportingOrigin)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setReportId(reportId.orElse(String.valueOf(UUID.randomUUID())))
            .setVersion(reportVersion)
            .build();

    reportBuilder.setSharedInfo(sharedInfo);
    return reportBuilder.build();
  }

  private FakeReportGenerator() {}
}
