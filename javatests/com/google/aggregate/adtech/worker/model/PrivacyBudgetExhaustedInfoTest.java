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

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetExhaustedInfoTest {

  private static final SharedInfo SHARED_INFO_1 =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("dest.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech.com")
          .setScheduledReportTime(Instant.EPOCH)
          .setSourceRegistrationTime(Instant.EPOCH)
          .build();

  private static final SharedInfo SHARED_INFO_2 =
      SharedInfo.builder()
          .setApi(SharedInfo.ATTRIBUTION_REPORTING_API)
          .setDestination("dest.com")
          .setVersion(SharedInfo.LATEST_VERSION)
          .setReportId(UUID.randomUUID().toString())
          .setReportingOrigin("adtech2.com")
          .setScheduledReportTime(Instant.EPOCH)
          .setSourceRegistrationTime(Instant.EPOCH)
          .build();

  private static final UnsignedLong FILTERING_ID = UnsignedLong.valueOf(1);

  @Test
  public void privacyBudgetExhaustedBuilder_succeeds() {
    AggregatableInputBudgetConsumptionInfo info1 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder().setSharedInfo(SHARED_INFO_1).build())
            .build();

    AggregatableInputBudgetConsumptionInfo info2 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder()
                    .setSharedInfo(SHARED_INFO_2)
                    .setFilteringId(FILTERING_ID)
                    .build())
            .build();

    PrivacyBudgetExhaustedInfo exhaustedInfo =
        PrivacyBudgetExhaustedInfo.builder()
            .setAggregatableInputBudgetConsumptionInfos(ImmutableSet.of(info1, info2))
            .build();

    assertThat(exhaustedInfo.aggregatableInputBudgetConsumptionInfos())
        .containsExactly(info1, info2);
  }
}
