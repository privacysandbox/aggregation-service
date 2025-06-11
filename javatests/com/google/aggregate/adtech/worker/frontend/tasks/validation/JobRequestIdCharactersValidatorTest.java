/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.tasks.validation;

import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.JobGenerator;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class JobRequestIdCharactersValidatorTest {

  // Under test
  JobRequestIdCharactersValidator validator;

  RequestInfo.Builder requestInfoBuilder;

  @Before
  public void setUp() {
    validator = new JobRequestIdCharactersValidator();
    requestInfoBuilder = JobGenerator.createFakeRequestInfo("foo").toBuilder();
  }

  /**
   * Test that validation passes for various jobRequestId values. Parameterized test is used to
   * avoid repetition.
   */
  @Test
  @TestParameters({
    "{jobRequestId: 'abcdefghijklmnopqrstuvwxyz0123456789'}",
    "{jobRequestId: 'a7a0b84-c8a2-44e0-be2a-05f5e5caf2c5'}",
    "{jobRequestId: '7771565449025225878'}",
  })
  public void testValidationsShouldPass(String jobRequestId) {
    RequestInfo requestInfo = requestInfoBuilder.setJobRequestId(jobRequestId).build();

    Optional<String> errorMessage = validator.validate(requestInfo);

    assertThat(errorMessage).isEmpty();
  }

  /**
   * Non-parameterized test for punctuation since TestParameterInjector can't parse all punctuation
   */
  @Test
  public void testValidationsShouldPassForPunctuation() {
    RequestInfo requestInfo =
        requestInfoBuilder.setJobRequestId("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{}~").build();

    Optional<String> errorMessage = validator.validate(requestInfo);

    assertThat(errorMessage).isEmpty();
  }

  /**
   * Test that validation fails for various jobRequestId values. Parameterized test is used to avoid
   * repetition.
   */
  @Test
  @TestParameters({
    "{jobRequestId: 'foo|bar'}", // contains | character
    "{jobRequestId: 'abcdé'}", // illegal accent character
    "{jobRequestId: '\uD83D\uDE00'}", // Unicode (emoji smile)
    "{jobRequestId: '\u4F60\u597D'}", // Unicode (hello in chinese)
  })
  public void testValidationsShouldFail(String jobRequestId) {
    RequestInfo requestInfo = requestInfoBuilder.setJobRequestId(jobRequestId).build();

    Optional<String> errorMessage = validator.validate(requestInfo);

    assertThat(errorMessage).isPresent();
  }
}
