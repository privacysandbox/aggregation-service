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

package com.google.aggregate.adtech.worker.validation.v01;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.REQUIRED_SHAREDINFO_FIELD_INVALID;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SharedStoragePrivacyBudgetKeyFieldsValidatorTest {

  // Under test
  private SharedStoragePrivacyBudgetKeyFieldsValidator
      sharedStoragePrivacyBudgetKeyFieldsValidator =
          new SharedStoragePrivacyBudgetKeyFieldsValidator();

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String REPORTING_ORIGIN = "https://www.origin.com";

  private static final String RANDOM_UUID = UUID.randomUUID().toString();

  private static final String EMPTY_STRING = "";

  @Test
  public void sharedStorageReport_emptyReportingOrigin_validationFails() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setApi(SHARED_STORAGE_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(EMPTY_STRING)
            .setScheduledReportTime(FIXED_TIME)
            .build();

    Optional<ErrorMessage> validationError =
        sharedStoragePrivacyBudgetKeyFieldsValidator.validatePrivacyBudgetKey(sharedInfo);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedStorageReport_emptyVersion_validationFails() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setApi(SHARED_STORAGE_API)
            .setVersion(EMPTY_STRING)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .build();

    Optional<ErrorMessage> validationError =
        sharedStoragePrivacyBudgetKeyFieldsValidator.validatePrivacyBudgetKey(sharedInfo);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(REQUIRED_SHAREDINFO_FIELD_INVALID);
  }

  @Test
  public void sharedStorageReports_validationSucceeds() {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setApi(SHARED_STORAGE_API)
            .setVersion(VERSION_0_1)
            .setReportId(RANDOM_UUID)
            .setReportingOrigin(REPORTING_ORIGIN)
            .setScheduledReportTime(FIXED_TIME)
            .build();

    Optional<ErrorMessage> validationError =
        sharedStoragePrivacyBudgetKeyFieldsValidator.validatePrivacyBudgetKey(sharedInfo);

    assertThat(validationError).isEmpty();
  }
}
