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
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.common.collect.ImmutableList;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeValidatorTest {

  private static Report report1;
  private static Report report2;
  private static String reportId1;
  private static String reportId2;
  private static Job ctx;

  // Under test
  private FakeValidator fakeValidator;

  @Before
  public void setUp() {
    reportId1 = String.valueOf(UUID.randomUUID());
    reportId2 = String.valueOf(UUID.randomUUID());
    report1 = FakeReportGenerator.generateWithFixedReportId(1, reportId1, LATEST_VERSION);
    report2 = FakeReportGenerator.generateWithFixedReportId(1, reportId2, LATEST_VERSION);
    ctx = FakeJobGenerator.generate("foo");

    fakeValidator = new FakeValidator();
  }

  @Test
  public void setReportIdShouldReturnErrorValidation() {
    fakeValidator.setReportIdShouldReturnError(Set.of(reportId2));

    Optional<ErrorMessage> firstValidationError = fakeValidator.validate(report1, ctx);
    Optional<ErrorMessage> secondValidationError = fakeValidator.validate(report2, ctx);

    assertThat(firstValidationError).isEmpty();
    assertThat(secondValidationError).isPresent();
  }

  @Test
  public void setNextShouldReturnErrorValidation() {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, true).iterator());

    Optional<ErrorMessage> firstValidationError = fakeValidator.validate(report1, ctx);
    Optional<ErrorMessage> secondValidationError = fakeValidator.validate(report2, ctx);

    assertThat(firstValidationError).isEmpty();
    assertThat(secondValidationError).isPresent();
    // Runs out of errors.
    assertThrows(NoSuchElementException.class, () -> fakeValidator.validate(report2, ctx));
  }
}
