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

package com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/**
 * Generates V2 PrivacyBudgetKey. This version includes filteringId in addition to other fields in
 * V1 in privacy budget key calculations. This version of Budget Key is internal to the service. It
 * is mapped to the report versions in {@link PrivacyBudgetKeyGeneratorModule}.
 */
public class V2PrivacyBudgetKeyGenerator implements PrivacyBudgetKeyGenerator {

  @Override
  public String generatePrivacyBudgetKey(PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    SharedInfo sharedInfo = privacyBudgetKeyInput.sharedInfo();
    ImmutableList.Builder<String> privacyBudgetKeyInputElements = ImmutableList.builder();
    privacyBudgetKeyInputElements.add(sharedInfo.api().get());
    privacyBudgetKeyInputElements.add(sharedInfo.version());
    privacyBudgetKeyInputElements.add(sharedInfo.reportingOrigin());
    privacyBudgetKeyInputElements.add(sharedInfo.destination().get());
    // Debug reports may omit the source registration time.
    sharedInfo
        .sourceRegistrationTime()
        .ifPresent(time -> privacyBudgetKeyInputElements.add(time.toString()));
    // Filtering ID will always be present.
    privacyBudgetKeyInputElements.add(String.valueOf(privacyBudgetKeyInput.filteringId().get()));

    String privacyBudgetKeyHashInput =
        String.join(PRIVACY_BUDGET_KEY_DELIMITER, privacyBudgetKeyInputElements.build());

    return Hashing.sha256()
        .newHasher()
        .putBytes(privacyBudgetKeyHashInput.getBytes(StandardCharsets.UTF_8))
        .hash()
        .toString();
  }
}
