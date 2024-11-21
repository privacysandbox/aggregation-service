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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.StatusCode;
import java.time.Instant;

/** Interface for consuming privacy budgeting. */
public interface PrivacyBudgetingServiceBridge {

  /**
   * Consumes privacy budget for the given IDs if budget is available for all budget units.
   *
   * <p>Only consumes privacy budget if budgets are available for all provided units. Otherwise, no
   * budgets are consumed and the first few units for which the budget was not available are
   * returned.
   *
   * @param budgetsToConsume - List of PrivacyBudgetUnits to consume budget against.
   * @param claimedIdentity - Adtech site value to be used for authorization.
   * @return Empty list if budgets were consumed successfully. Otherwise, first few privacy budget
   *     units for which the privacy budget was not available.
   */
  ImmutableList<PrivacyBudgetUnit> consumePrivacyBudget(
      ImmutableList<PrivacyBudgetUnit> budgetsToConsume, String claimedIdentity)
      throws PrivacyBudgetingServiceBridgeException;

  /** Identifier for an individual key of the privacy budget to be consumed. */
  @AutoValue
  abstract class PrivacyBudgetUnit {

    public static PrivacyBudgetUnit create(
        String privacyBudgetKey, Instant scheduledReportTime, String reportingOrigin) {
      return new com.google.aggregate.privacy.budgeting.bridge
          .AutoValue_PrivacyBudgetingServiceBridge_PrivacyBudgetUnit(
          privacyBudgetKey, scheduledReportTime, reportingOrigin);
    }

    public abstract String privacyBudgetKey();

    public abstract Instant scheduledReportTime();

    public abstract String reportingOrigin();
  }

  /** Exception that may happen when consuming the privacy budget. */
  final class PrivacyBudgetingServiceBridgeException extends Exception {
    private final StatusCode statusCode;

    public PrivacyBudgetingServiceBridgeException() {
      super();
      this.statusCode = StatusCode.UNKNOWN;
    }

    public PrivacyBudgetingServiceBridgeException(Throwable cause) {
      super(cause);
      this.statusCode = StatusCode.UNKNOWN;
    }

    public PrivacyBudgetingServiceBridgeException(String message, Throwable cause) {
      super(message, cause);
      this.statusCode = StatusCode.UNKNOWN;
    }

    public PrivacyBudgetingServiceBridgeException(StatusCode statusCode, Throwable cause) {
      super(statusCode.name(), cause);
      this.statusCode = statusCode;
    }

    public StatusCode getStatusCode() {
      return this.statusCode;
    }
  }
}
