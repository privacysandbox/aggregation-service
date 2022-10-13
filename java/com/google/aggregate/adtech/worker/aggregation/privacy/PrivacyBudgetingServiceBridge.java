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

package com.google.aggregate.adtech.worker.aggregation.privacy;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;

/** Interface for consuming privacy budgeting. */
public interface PrivacyBudgetingServiceBridge {

  /**
   * Consumes the privacy budget for the given IDs and returns the budgets which cannot be consumed.
   *
   * <p>If the method returns an empty list, that means the budgets were successfully consumed,
   * otherwise, the first few units for which the budget could not be consumed is returned.
   *
   * @return First few privacy budgeting units for which budget consumption failed.
   */
  ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
      ImmutableList<PrivacyBudgetUnit> budgetsToConsume,
      String attributionReportTo,
      Optional<Integer> debugPrivacyBudgetLimit)
      throws PrivacyBudgetingServiceBridgeException;

  /** Identifier for an individual key of the privacy budget to be consumed. */
  @AutoValue
  abstract class PrivacyBudgetUnit {

    public static PrivacyBudgetUnit create(String privacyBudgetKey, Instant scheduledReportTime) {
      return new AutoValue_PrivacyBudgetingServiceBridge_PrivacyBudgetUnit(
          privacyBudgetKey, scheduledReportTime);
    }

    public abstract String privacyBudgetKey();

    public abstract Instant scheduledReportTime();
  }

  /** Exception that may happen when consuming the privacy budget. */
  final class PrivacyBudgetingServiceBridgeException extends Exception {

    public PrivacyBudgetingServiceBridgeException(Throwable cause) {
      super(cause);
    }
  }
}
