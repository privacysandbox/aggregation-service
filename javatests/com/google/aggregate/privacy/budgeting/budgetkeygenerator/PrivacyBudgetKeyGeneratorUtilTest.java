/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.privacy.budgeting.budgetkeygenerator;

import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.primitives.UnsignedLong;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetKeyGeneratorUtilTest {

  @Test
  public void getPrivacyBudgetKeyGeneratorV1Predicate() {
    Predicate<PrivacyBudgetKeyInput> pbkPredicate =
        PrivacyBudgetKeyGeneratorUtil.getPrivacyBudgetKeyGeneratorV1Predicate();

    // The predicate should be true for 0.0 <= version < 1.0 and filteringId = 0 or empty
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.1", Optional.of("54")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("1.0", Optional.of("0")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("1.0", Optional.of("65")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("78900.0", Optional.of("0")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("785.36", Optional.empty()))).isFalse();

    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.1", Optional.of("0")))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.9999", Optional.of("0")))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.9999", Optional.empty()))).isTrue();
  }

  @Test
  public void getPrivacyBudgetKeyGeneratorV2Predicate() {
    Predicate<PrivacyBudgetKeyInput> pbkPredicate =
        PrivacyBudgetKeyGeneratorUtil.getPrivacyBudgetKeyGeneratorV2Predicate();

    // The predicate should be true for version >= 1.0 or filteringId = 0 or empty
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.1", Optional.of("54")))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("1.0", Optional.of("0")))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("78900.0", Optional.of("0")))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("785.36", Optional.empty()))).isTrue();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("78900.0", Optional.of("9632587410"))))
        .isTrue();

    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.1", Optional.of("0")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.9999", Optional.of("0")))).isFalse();
    assertThat(pbkPredicate.test(getPrivacyBudgetKeyInput("0.888", Optional.empty()))).isFalse();
  }

  private static PrivacyBudgetKeyInput getPrivacyBudgetKeyInput(
      String version, Optional<String> filteringIdOptional) {
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setVersion(version)
            .setReportId("report_id")
            .setScheduledReportTime(Instant.now())
            .setDestination("destination.com")
            .setReportingOrigin("bar.com")
            .setReportDebugMode(true)
            .build();
    PrivacyBudgetKeyInput.Builder privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder().setSharedInfo(sharedInfo);
    filteringIdOptional.ifPresent(
        filteringId -> privacyBudgetKeyInput.setFilteringId(UnsignedLong.valueOf(filteringId)));
    return privacyBudgetKeyInput.build();
  }
}
