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

package com.google.aggregate.privacy.budgeting.budgetkeygenerator.attributionreportingdebug;

import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Generates V1 PrivacyBudgetKey. This version of Budget Key is internal to the service. It is
 * mapped to the report versions in {@link PrivacyBudgetKeyGeneratorModule}.
 *
 * <p>TODO(b/321719045): Deprecate V1 version of Privacy Budget Key when the corresponding report
 * versions are phased out.
 */
public class V1PrivacyBudgetKeyGenerator implements PrivacyBudgetKeyGenerator {

  @Override
  public String generatePrivacyBudgetKey(PrivacyBudgetKeyInput privacyBudgetKeyInput) {
    SharedInfo sharedInfo = privacyBudgetKeyInput.sharedInfo();
    List<String> privacyBudgetKeyInputElements = new LinkedList<>();
    privacyBudgetKeyInputElements.add(sharedInfo.api().get());
    privacyBudgetKeyInputElements.add(sharedInfo.version());
    privacyBudgetKeyInputElements.add(sharedInfo.reportingOrigin());
    privacyBudgetKeyInputElements.add(sharedInfo.destination().get());
    // Debug reports may omit the source registration time.
    sharedInfo
        .sourceRegistrationTime()
        .ifPresent(time -> privacyBudgetKeyInputElements.add(time.toString()));

    String privacyBudgetKeyHashInput =
        String.join(PRIVACY_BUDGET_KEY_DELIMITER, privacyBudgetKeyInputElements);

    return Hashing.sha256()
        .newHasher()
        .putBytes(privacyBudgetKeyHashInput.getBytes(StandardCharsets.UTF_8))
        .hash()
        .toString();
  }
}
