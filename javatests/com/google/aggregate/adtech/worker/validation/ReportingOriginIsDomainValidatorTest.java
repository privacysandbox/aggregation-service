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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.ATTRIBUTION_REPORT_TO_MALFORMED;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class ReportingOriginIsDomainValidatorTest {

  // Under test
  private ReportingOriginIsDomainValidator validator;

  private Report.Builder reportBuilder;
  SharedInfo.Builder sharedInfoBuilder;
  Job ctx;

  @Before
  public void setUp() {
    validator = new ReportingOriginIsDomainValidator();
    reportBuilder = Report.builder().setPayload(Payload.builder().build());
    sharedInfoBuilder =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setReportingOrigin("")
            .setScheduledReportTime(Instant.now())
            .setDestination("")
            .setSourceRegistrationTime(Instant.now());
    ctx = FakeJobGenerator.generate("");
  }

  /**
   * Test that validation passes for various attributionReportTo values. Parameterized test is used
   * to avoid repetition.
   */
  @Test
  @TestParameters({
    "{attributionReportTo: 'foo.com'}",
    "{attributionReportTo: 'foo.co.uk'}",
    "{attributionReportTo: 'foo.io'}",
    "{attributionReportTo: 'www.foo.com'}",
  })
  public void testValidationsShouldPass(String attributionReportTo) {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin(attributionReportTo).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    // `assertWithMessage` adds helpful error message and only support boolean assertion (as opposed
    // to Truth8)
    assertWithMessage(String.format("Test case: %s", attributionReportTo))
        .that(validationError.isPresent())
        .isFalse();
  }

  /**
   * Test that validation passes for various attributionReportTo values. Parameterized test is used
   * to avoid repetition.
   */
  @Test
  @TestParameters({
    "{attributionReportTo: 'http://www.foo.com'}",
    "{attributionReportTo: 'foo.tldthatdoesntexist'}",
    "{attributionReportTo: 'http://foo'}",
    "{attributionReportTo: 'foo.com/api/bar'}",
    "{attributionReportTo: '123.456.789.012'}",
    "{attributionReportTo: '2002:0ab3:7a45:3fa3:d025:a8f5:0b6e:5080'}",
  })
  public void testValidationsShouldFail(String attributionReportTo) {
    Report report =
        reportBuilder
            .setSharedInfo(sharedInfoBuilder.setReportingOrigin(attributionReportTo).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    // `assertWithMessage` adds helpful error message and only support boolean assertion (as opposed
    // to Truth8)
    assertWithMessage(String.format("Test case: %s", attributionReportTo))
        .that(validationError.isPresent())
        .isTrue();
    assertWithMessage(String.format("Test case: %s", attributionReportTo))
        .that(validationError.get().category())
        .isEqualTo(ATTRIBUTION_REPORT_TO_MALFORMED);
  }
}
