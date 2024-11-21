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

import static com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.time.temporal.ChronoUnit.HOURS;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Data engine for centrally aggregating facts coming in from different threads
 *
 * <p>The engine should be used for *one* aggregation batch and new instance should be created for
 * each aggregation.
 *
 * <p>This implementation is thread-safe.
 *
 * <p>The engine aggregates by keeping a map of aggregation data, keyed by facts' buckets. The
 * engine is a consumer of reports and aggregates by flattening individual facts from reports and
 * adds +1 for each fact bucket count and +x for fact value.
 */
public final class AggregationEngine implements Consumer<Report> {

  private final PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  // Track aggregations for individual facts, keyed by fact buckets that are 128-bit integers.
  private final ConcurrentMap<BigInteger, LongAdder> aggregationMap;

  // Tracks distinct privacy budget unit identifiers for the reports aggregated.
  private final Set<PrivacyBudgetUnit> privacyBudgetUnits;

  /** reportIdSet tracks the unique report ids within a single aggregation batch. */
  private final Set<UUID> reportIdSet;

  /** Queried filteringIds to filter payload contributions. */
  private final ImmutableSet<UnsignedLong> filteringIds;

  /**
   * Consumes a report by adding its individual facts to the aggregation Only reports with unique
   * report_id within a batch are used in aggregation
   */
  @Override
  public void accept(Report report) {
    if (report.sharedInfo().reportId().isPresent()
        && reportIdSet.add(UUID.fromString(report.sharedInfo().reportId().get()))) {
      // For privacy reasons, filteringIds listed in the job parameters is assumed to be present in
      // all the reports.
      // One filteringId can be used in maximum of one job and, as a result, contributes to only one
      // summary rerport.
      filteringIds.forEach(filteringId -> addPrivacyBudgetKey(report.sharedInfo(), filteringId));
      report.payload().data().stream()
          .filter(fact -> !isNullFact(fact))
          .filter(fact -> containsFilteringId(fact, filteringIds))
          .forEach(this::upsertAggregationForFact);
    }
  }

  /** Checks if the queried filteringId matches the fact's. */
  private static boolean containsFilteringId(Fact fact, ImmutableSet<UnsignedLong> filteringIds) {
    // id = 0 is the default for reports w/o ids.
    UnsignedLong factId = fact.id().orElse(UnsignedLong.ZERO);
    return filteringIds.contains(factId);
  }

  /**
   * Insert a new key with an empty fact. PBKs are not calculated for keys added using this method.
   */
  public void accept(BigInteger key) {
    aggregationMap.computeIfAbsent(key, unused -> new LongAdder());
  }

  public boolean containsKey(BigInteger key) {
    return aggregationMap.containsKey(key);
  }

  public Set<BigInteger> getKeySet() {
    return aggregationMap.keySet();
  }

  /**
   * Returns true if the fact is a null fact. Null facts have both keys and values to 0.
   *
   * @param fact
   */
  private static boolean isNullFact(Fact fact) {
    return fact.value() == 0 && fact.bucket().equals(BigInteger.ZERO);
  }

  /** Calculates Privacy Budget Keys for the report for the filteringId. */
  private void addPrivacyBudgetKey(SharedInfo sharedInfo, UnsignedLong filteringId) {
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
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
        PrivacyBudgetUnit.create(
            privacyBudgetKey,
            sharedInfo.scheduledReportTime().truncatedTo(HOURS),
            sharedInfo.reportingOrigin());
    privacyBudgetUnits.add(budgetUnitId);
  }

  /**
   * Creates the materialized, in-memory aggregation for the accumulated facts (through reports).
   */
  // TODO: investigate enforcing call of makeAggregation strictly after all accepts.
  public ImmutableMap<BigInteger, AggregatedFact> makeAggregation() {
    return aggregationMap.entrySet().stream()
        .map(factAggr -> AggregatedFact.create(factAggr.getKey(), factAggr.getValue().longValue()))
        .collect(toImmutableMap(AggregatedFact::getBucket, Function.identity()));
  }

  /** Gets a set of distinct privacy budget units observed during the aggregation */
  public ImmutableList<PrivacyBudgetUnit> getPrivacyBudgetUnits() {
    return ImmutableList.copyOf(privacyBudgetUnits);
  }

  /**
   * Upserts (updates or inserts) an aggregation for a fact
   *
   * <p>If the fact key has not been encountered before, a new entry will be created in the
   * aggregation map, and started with the given fact's info. Otherwise, the aggregation for the
   * fact is just updated.
   */
  private void upsertAggregationForFact(Fact fact) {
    aggregationMap.computeIfAbsent(fact.bucket(), unused -> new LongAdder()).add(fact.value());
  }

  AggregationEngine(
      PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory,
      ConcurrentMap<BigInteger, LongAdder> aggregationMap,
      Set<PrivacyBudgetUnit> privacyBudgetUnits,
      Set<UUID> reportIdSet,
      ImmutableSet<UnsignedLong> filteringIds) {
    this.privacyBudgetKeyGeneratorFactory = privacyBudgetKeyGeneratorFactory;
    this.aggregationMap = aggregationMap;
    this.privacyBudgetUnits = privacyBudgetUnits;
    this.reportIdSet = reportIdSet;
    this.filteringIds = filteringIds;
  }
}
