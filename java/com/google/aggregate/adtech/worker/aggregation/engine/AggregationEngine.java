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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Sets.newConcurrentHashSet;
import static java.time.temporal.ChronoUnit.HOURS;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.PrivacyBudgetKeyGeneratorFactory;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
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

  // Track aggregations for individual facts, keyed by fact buckets that are 128-bit integers.
  private final ConcurrentMap<BigInteger, SingleFactAggregation> aggregationMap;

  // Tracks distinct privacy budget unit identifiers for the reports aggregated.
  private final Set<PrivacyBudgetUnit> privacyBudgetUnits;

  /** reportIdSet tracks the unique report ids within a single aggregation batch. */
  private final Set<UUID> reportIdSet;

  public static AggregationEngine create() {
    // Number of logical cores available to the JVM is used to hint the concurrent map maker. Any
    // number will work, this is just a hint that is passed to the map maker, but different values
    // may result in different performance.
    //
    // NOTE: when JVM runtime is probed, it returns the *logical* number of cores. This can be
    // different from the number of physical cores available on the machine, e.g. if hyperthreading
    // is used, the number obtained here is 2x larger than the number of physical cores.
    int concurrentMapConcurrencyHint = Runtime.getRuntime().availableProcessors();

    ConcurrentMap<BigInteger, SingleFactAggregation> aggregationMap =
        new MapMaker().concurrencyLevel(concurrentMapConcurrencyHint).makeMap();
    Set<PrivacyBudgetUnit> privacyBudgetUnits = newConcurrentHashSet();
    Set<UUID> reportIdSet = newConcurrentHashSet();

    return new AggregationEngine(aggregationMap, privacyBudgetUnits, reportIdSet);
  }

  /**
   * Consumes a report by adding its individual facts to the aggregation Only reports with unique
   * report_id within a batch are used in aggregation
   */
  @Override
  public void accept(Report report) {
    if (report.sharedInfo().reportId().isPresent()
        && reportIdSet.add(UUID.fromString(report.sharedInfo().reportId().get()))) {
      Optional<String> privacyBudgetKey = getPrivacyBudgetKey(report.sharedInfo());
      if (privacyBudgetKey.isEmpty()) {
        return;
      }
      PrivacyBudgetUnit budgetUnitId =
          PrivacyBudgetUnit.create(
              privacyBudgetKey.get(), report.sharedInfo().scheduledReportTime().truncatedTo(HOURS));
      privacyBudgetUnits.add(budgetUnitId);
      report.payload().data().forEach(this::upsertAggregationForFact);
    }
  }

  // TODO: b/261728313 remove Optional from return type.
  private Optional<String> getPrivacyBudgetKey(SharedInfo sharedInfo) {
    Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
        PrivacyBudgetKeyGeneratorFactory.getPrivacyBudgetKeyGenerator(sharedInfo.api());
    if (privacyBudgetKeyGenerator.isEmpty()) {
      return Optional.empty();
    }
    return privacyBudgetKeyGenerator.get().generatePrivacyBudgetKey(sharedInfo);
  }

  /**
   * Creates the materialized, in-memory aggregation for the accumulated facts (through reports).
   */
  // TODO: investigate enforcing call of makeAggregation strictly after all accepts.
  public ImmutableMap<BigInteger, AggregatedFact> makeAggregation() {
    return aggregationMap.entrySet().stream()
        .map(
            factAggr -> {
              SingleFactAggregation aggregation = factAggr.getValue();
              return AggregatedFact.create(factAggr.getKey(), aggregation.getSum());
            })
        .collect(toImmutableMap(AggregatedFact::bucket, Function.identity()));
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
    aggregationMap
        .computeIfAbsent(fact.bucket(), unused -> new SingleFactAggregation())
        .accept(fact);
  }

  private AggregationEngine(
      ConcurrentMap<BigInteger, SingleFactAggregation> aggregationMap,
      Set<PrivacyBudgetUnit> privacyBudgetUnits,
      Set<UUID> reportIdSet) {
    this.aggregationMap = aggregationMap;
    this.privacyBudgetUnits = privacyBudgetUnits;
    this.reportIdSet = reportIdSet;
  }
}
