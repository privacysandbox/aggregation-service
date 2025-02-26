/*
 * Copyright 2024 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.primitives.UnsignedLong;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AggregatableInputBudgetConsumptionInfoTest {

  private static final SharedInfo SHARED_INFO =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("dest.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech.com")
          .setScheduledReportTime(Instant.EPOCH)
          .setSourceRegistrationTime(Instant.EPOCH)
          .build();

  private static final UnsignedLong FILTERING_ID = UnsignedLong.valueOf(1);

  @Test
  public void setAggregatableInputBudgetConsumptionInfo_succeeds() {
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setSharedInfo(SHARED_INFO)
            .setFilteringId(FILTERING_ID)
            .build();

    AggregatableInputBudgetConsumptionInfo info =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(privacyBudgetKeyInput)
            .build();

    assertThat(info.privacyBudgetKeyInput()).isEqualTo(privacyBudgetKeyInput);
  }
}
