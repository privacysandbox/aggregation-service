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

package com.google.aggregate.adtech.worker.model;

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DecryptionValidationResultTest {

  /** Test for validation that the report can be set without error */
  @Test
  public void testValidationCanSetReport() {
    DecryptionValidationResult.Builder resultBuilder =
        DecryptionValidationResult.builder()
            .setReport(
                FakeReportGenerator.generateWithParam(
                    1, /* reportVersion */ LATEST_VERSION, "https://foo.com"));

    resultBuilder.build();

    // Test passes if no exceptions are thrown
  }

  /** Test for validation that error messages can be added without error */
  @Test
  public void testValidationCanSetErrors() {
    DecryptionValidationResult.Builder resultBuilder =
        DecryptionValidationResult.builder()
            .addErrorMessage(
                ErrorMessage.builder().setCategory(ErrorCounter.DECRYPTION_ERROR).build());

    resultBuilder.build();

    // Test passes if no exceptions are thrown
  }

  /**
   * Test for validation that an error is thrown if both a record and error messages are provided
   */
  @Test
  public void testValidationErrorThrownIfBothSet() {
    DecryptionValidationResult.Builder resultBuilder =
        DecryptionValidationResult.builder()
            .setReport(
                FakeReportGenerator.generateWithParam(
                    1, /* reportVersion */ LATEST_VERSION, "https://foo.com"))
            .addErrorMessage(
                ErrorMessage.builder().setCategory(ErrorCounter.DECRYPTION_ERROR).build());

    // An exception should be thrown as the DecryptionValidationResult is not valid, it contains
    // both a record and error messages.
    assertThrows(IllegalStateException.class, resultBuilder::build);
  }

  /**
   * Test for validation that an error is thrown if neither a report or error messages are provided
   */
  @Test
  public void testValidationErrorThrownIfNeitherSet() {
    DecryptionValidationResult.Builder resultBuilder = DecryptionValidationResult.builder();

    // An exception should be thrown as the DecryptionValidationResult is not valid, it contains
    // neither a record or error messages.
    assertThrows(IllegalStateException.class, resultBuilder::build);
  }
}
