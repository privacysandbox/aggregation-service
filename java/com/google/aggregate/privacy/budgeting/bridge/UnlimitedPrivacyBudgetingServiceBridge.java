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

package com.google.aggregate.privacy.budgeting.bridge;

import com.google.common.collect.ImmutableList;

/**
 * Privacy budgeting service bridge with unlimited budget
 *
 * <p>This implementation is meant for local testing without privacy budgeting management, i.e. the
 * budgeting is not capped, it's unlimited.
 */
public final class UnlimitedPrivacyBudgetingServiceBridge implements PrivacyBudgetingServiceBridge {

  @Override
  public ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
      ImmutableList<PrivacyBudgetUnit> originToPrivacyBudgetUnits, String claimedIdentity)
      throws PrivacyBudgetingServiceBridgeException {
    return ImmutableList.of();
  }
}
