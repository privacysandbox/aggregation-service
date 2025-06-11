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
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JobRequestIdLengthValidatorTest {

  // Under test
  JobRequestIdLengthValidator validator;

  RequestInfo.Builder requestInfoBuilder;

  @Before
  public void setUp() {
    validator = new JobRequestIdLengthValidator();
    requestInfoBuilder = JobGenerator.createFakeRequestInfo("foo").toBuilder();
  }

  /** Validation should pass for strings 128 characters or shorter */
  @Test
  public void validationPassesForShorterString() {
    // 128 char string
    String jobRequestId =
        "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqr"
            + "stuvwxyz0123456789abcdefghijklmnopqrst";
    RequestInfo requestInfo = requestInfoBuilder.setJobRequestId(jobRequestId).build();

    Optional<String> errorMessage = validator.validate(requestInfo);

    assertThat(errorMessage).isEmpty();
  }

  /** Validation should fail for strings longer than 128 characters */
  @Test
  public void validationFailsForLongerString() {
    // 129 char string
    String jobRequestId =
        "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqr"
            + "stuvwxyz0123456789abcdefghijklmnopqrstu";
    RequestInfo requestInfo = requestInfoBuilder.setJobRequestId(jobRequestId).build();

    Optional<String> errorMessage = validator.validate(requestInfo);

    assertThat(errorMessage).isPresent();
  }
}
