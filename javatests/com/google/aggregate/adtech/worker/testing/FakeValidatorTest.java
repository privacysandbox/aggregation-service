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

import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeValidatorTest {

  private static Report report;
  private static Job ctx;

  // Under test
  private FakeValidator fakeValidator;

  @Before
  public void setUp() {
    report = FakeReportGenerator.generateWithParam(1, /* reportVersion */ "");
    ctx = FakeJobGenerator.generate("foo");

    fakeValidator = new FakeValidator();
  }

  @Test
  public void validation() {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false, true).iterator());

    Optional<ErrorMessage> firstValidationError = fakeValidator.validate(report, ctx);
    Optional<ErrorMessage> secondValidationError = fakeValidator.validate(report, ctx);

    assertThat(firstValidationError).isEmpty();
    assertThat(secondValidationError).isPresent();
    // Runs out of errors.
    assertThrows(NoSuchElementException.class, () -> fakeValidator.validate(report, ctx));
  }
}
