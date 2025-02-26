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

package com.google.aggregate.adtech.worker.aggregation.engine;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A skeletal implementation of {@code AggregationEngine}. This class handles report deduping,
 * contribution filtering, and Aggregation reports privacy budget unit generation logic.
 *
 * <p>A new implementation of {@link AggregationEngine} can extend this abstract class and override
 * the abstract methods. If an implementation can't extend this skeletal implementation, then it can
 * directly implement the {@link AggregationEngine} directly.
 */
public abstract class AbstractAggregationEngine implements AggregationEngine {

  private final Set<UUID> reportIdSet;

  private final ImmutableSet<UnsignedLong> filteringIds;

  protected final PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  AbstractAggregationEngine(
      Set<UUID> reportIdSet,
      ImmutableSet<UnsignedLong> filteringIds,
      PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory) {
    this.reportIdSet = reportIdSet;
    this.filteringIds = filteringIds;
    this.privacyBudgetKeyGeneratorFactory = privacyBudgetKeyGeneratorFactory;
  }

  /**
   * Updates aggregation and privacy budget tracker for the given report after filtering out
   * irrelevant facts.
   */
  @Override
  public void accept(Report report) {
    if (report.sharedInfo().reportId().isPresent()
        && reportIdSet.add(UUID.fromString(report.sharedInfo().reportId().get()))) {
      addPrivacyBudgetUnitForAggregatableReport(report.sharedInfo(), filteringIds);
      report.payload().data().stream()
          .filter(fact -> !isNullFact(fact))
          .filter(fact -> containsFilteringId(fact, filteringIds))
          .forEach(
              fact ->
                  upsertAggregationForFact(
                      ImmutableSet.of(fact.id().orElse(UnsignedLong.ZERO)),
                      fact.bucket(),
                      fact.value()));
    }
  }

  abstract void addPrivacyBudgetUnit(
      SharedInfo sharedInfo, UnsignedLong filteringId, PrivacyBudgetUnit privacyBudgetUnit);

  abstract void upsertAggregationForFact(
      ImmutableSet<UnsignedLong> filteringIds, BigInteger bucket, long value);

  /** Generates and adds a Privacy Budget Unit for the aggregatable report and each filtering Id. */
  private void addPrivacyBudgetUnitForAggregatableReport(
      SharedInfo sharedInfo, ImmutableSet<UnsignedLong> filteringIds) {

    for (UnsignedLong filteringId : filteringIds) {
      PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput privacyBudgetKeyInput =
          PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput.builder()
              .setSharedInfo(sharedInfo)
              .setFilteringId(filteringId)
              .build();

      Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
          privacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(privacyBudgetKeyInput);
      if (privacyBudgetKeyGenerator.isEmpty()) {
        // Impossible because validations ensure only the supported reports are allowed.
        throw new IllegalStateException(
            String.format(
                "PrivacyBudgetKeyGenerator for the given report is not found. Api = %s. Report"
                    + " Version  =%s.",
                sharedInfo.api().get(), sharedInfo.version()));
      }
      String privacyBudgetKey =
          privacyBudgetKeyGenerator.get().generatePrivacyBudgetKey(privacyBudgetKeyInput);
      PrivacyBudgetUnit budgetUnitId =
          PrivacyBudgetUnit.createHourTruncatedUnit(
              privacyBudgetKey, sharedInfo.scheduledReportTime(), sharedInfo.reportingOrigin());
      addPrivacyBudgetUnit(sharedInfo, filteringId, budgetUnitId);
    }
  }

  /** Returns true if the fact is a null fact. Null facts have both keys and values to 0. */
  private static boolean isNullFact(Fact fact) {
    return fact.value() == 0 && fact.bucket().equals(BigInteger.ZERO);
  }

  /** Checks if the queried filteringId matches the fact's. */
  private static boolean containsFilteringId(Fact fact, ImmutableSet<UnsignedLong> filteringIds) {
    // id = 0 is the default for reports w/o ids.
    UnsignedLong factId = fact.id().orElse(UnsignedLong.ZERO);
    return filteringIds.contains(factId);
  }
}
