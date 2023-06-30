/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.testing.loadtest;

import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionExecutor implements Callable<Void> {

  private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);
  private final PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge;
  private final int pbsKeysPerTransaction;
  private final String reportingOrigin;

  private final Random random;

  public TransactionExecutor(
      PrivacyBudgetingServiceBridge privacyBudgetingServiceBridge,
      int pbsKeysPerTransaction,
      String reportingOrigin) {
    this.privacyBudgetingServiceBridge = privacyBudgetingServiceBridge;
    this.pbsKeysPerTransaction = pbsKeysPerTransaction;
    this.reportingOrigin = reportingOrigin;
    this.random = new Random();
  }

  @Override
  public Void call() throws Exception {
    ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits = getPrivacyBudgetUnits();
    try {
      ImmutableList<PrivacyBudgetUnit> missingUnits =
          privacyBudgetingServiceBridge.consumePrivacyBudget(
              /* budgetsToConsume= */ privacyBudgetUnits,
              /* attributionReportTo= */ reportingOrigin);
      if (!missingUnits.isEmpty()) {
        logger.warn("Missing PBS units size: {}", missingUnits.size());
      }
    } catch (Exception e) {
      logger.error("Error consuming pbs units: {}", e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Generates a list of privacy budget keys. The list generation logic alternates between 1.
   * Choosing different budget keys for a given scheduled report time 2. Choosing a given privacy
   * budget key but with a range of scheduled report times
   *
   * <p>This is done so that the generated keys exercise both types of loads which we expect to see
   * in production. These are 1. Budget consumption requests that consume all keys from a single
   * database row ( daily batches for a constant pbs key such as daily batching on PAA reports) 2.
   * Budget consumption requests that span multiple database rows (weekly batching of PAA reports or
   * ARA reports where we have multiple PBS keys in the batch).
   *
   * @return a list of privacy budget keys
   */
  private ImmutableList<PrivacyBudgetUnit> getPrivacyBudgetUnits() {
    Instant scheduledReportTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
    if (random.nextBoolean()) {
      return IntStream.range(0, pbsKeysPerTransaction)
          .mapToObj(
              index -> PrivacyBudgetUnit.create(UUID.randomUUID().toString(), scheduledReportTime))
          .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    } else {
      String privacyBudgetKey = UUID.randomUUID().toString();
      return IntStream.range(0, pbsKeysPerTransaction)
          .mapToObj(
              index ->
                  PrivacyBudgetUnit.create(
                      privacyBudgetKey,
                      scheduledReportTime
                          .plus(index, ChronoUnit.HOURS)
                          .truncatedTo(ChronoUnit.HOURS)))
          .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }
  }
}
