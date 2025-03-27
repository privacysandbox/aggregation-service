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

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetUnit;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Data engine for centrally aggregating by bucket or key coming in from different threads The
 * engine aggregates by keeping a map of aggregation data, keyed by facts' buckets.
 *
 * <p>The engine should be used for *one* aggregation batch and new instance should be created for
 * each aggregation.
 *
 * <p>This implementation is thread-safe.
 */
public final class KeyAggregationEngine extends AbstractAggregationEngine {

  // Tracks distinct privacy budget unit identifiers for the reports aggregated and the
  // corresponding PrivacyBudgetKeyInput.
  private final ConcurrentMap<PrivacyBudgetUnit, PrivacyBudgetKeyInput>
      privacyBudgetUnitToPrivacyBudgetKeyInput;

  // Track aggregations for individual facts, keyed by fact buckets that are 128-bit integers.
  private final ConcurrentMap<BigInteger, LongAdder> aggregationMap;
  // Tracks distinct privacy budget unit identifiers for the reports aggregated.
  private final Set<PrivacyBudgetUnit> privacyBudgetUnits;

  /**
   * Insert a new key with an empty fact. PBKs are not calculated for keys added using this method.
   */
  @Override
  public void accept(AggregationKey key) {
    aggregationMap.computeIfAbsent(key.bucket(), unused -> new LongAdder());
  }

  /** Get the aggregated value for a key or return the default value. */
  @Override
  public long getAggregatedValueOrDefault(AggregationKey key, long defaultValue) {
    LongAdder aggregatedValue = aggregationMap.get(key.bucket());
    return aggregatedValue == null ? defaultValue : aggregatedValue.longValue();
  }

  /**
   * Remove the key and aggregated value from the aggregation engine map.
   *
   * @return The aggregated value for the key.
   */
  @Override
  public OptionalLong remove(AggregationKey key) {
    LongAdder removedVal = aggregationMap.remove(key.bucket());
    return removedVal != null ? OptionalLong.of(removedVal.longValue()) : OptionalLong.empty();
  }

  @Override
  public Stream<Entry<AggregationKey, LongAdder>> getEntries() {
    return aggregationMap.entrySet().stream()
        .map(entry -> Map.entry(AggregationKey.create(entry.getKey()), entry.getValue()));
  }

  @Override
  public boolean containsKey(AggregationKey key) {
    return aggregationMap.containsKey(key.bucket());
  }

  @Override
  public Stream<AggregationKey> getKeySet() {
    return aggregationMap.keySet().stream().map(key -> AggregationKey.create(key));
  }

  /**
   * Creates the materialized, in-memory aggregation for the accumulated facts (through reports).
   */
  @Override
  public ImmutableMap<AggregationKey, AggregatedFact> makeAggregation() {
    return aggregationMap.entrySet().stream()
        .map(factAggr -> AggregatedFact.create(factAggr.getKey(), factAggr.getValue().longValue()))
        .collect(
            toImmutableMap(entry -> AggregationKey.create(entry.getBucket()), Function.identity()));
  }

  /** Gets a set of distinct privacy budget units observed during the aggregation */
  @Override
  public ImmutableList<PrivacyBudgetUnit> getPrivacyBudgetUnits() {
    return ImmutableList.copyOf(privacyBudgetUnitToPrivacyBudgetKeyInput.keySet());
  }

  /** Upserts (updates or inserts) an aggregation. */
  @Override
  void upsertAggregationForFact(
      ImmutableSet<UnsignedLong> unusedFilteringId, BigInteger bucket, long value) {
    aggregationMap.computeIfAbsent(bucket, unused -> new LongAdder()).add(value);
  }

  /**
   * Collects Privacy Budget Units for the report being processed.
   *
   * <p>This implementation ignores the filtering ID.
   */
  @Override
  void addPrivacyBudgetUnit(
      SharedInfo sharedInfo, UnsignedLong filteringId, PrivacyBudgetUnit privacyBudgetUnit) {
    privacyBudgetUnits.add(privacyBudgetUnit);
    addPrivacyBudgetUnitToPrivacyBudgetKeyInput(privacyBudgetUnit, sharedInfo, filteringId);
  }

  /**
   * Returns corresponding unordered list of PrivacyBudgetKeyInput for given PrivacyBudgetUnit list.
   */
  @Override
  public ImmutableList<PrivacyBudgetKeyInput> getPrivacyBudgetKeyInputsFromPrivacyBudgetUnits(
      ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits) {
    return privacyBudgetUnits.stream()
        .filter(privacyBudgetUnitToPrivacyBudgetKeyInput::containsKey)
        .map(privacyBudgetUnitToPrivacyBudgetKeyInput::get)
        .collect(ImmutableList.toImmutableList());
  }

  /** Calculates Privacy Budget Keys for the report for the filteringId. */
  private void addPrivacyBudgetUnitToPrivacyBudgetKeyInput(
      PrivacyBudgetUnit privacyBudgetUnit, SharedInfo sharedInfo, UnsignedLong filteringId) {
    PrivacyBudgetKeyInput privacyBudgetKeyInput =
        PrivacyBudgetKeyInput.builder()
            .setSharedInfo(sharedInfo)
            .setFilteringId(filteringId)
            .build();

    privacyBudgetUnitToPrivacyBudgetKeyInput.putIfAbsent(privacyBudgetUnit, privacyBudgetKeyInput);
  }

  KeyAggregationEngine(
      PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory,
      ConcurrentMap<BigInteger, LongAdder> aggregationMap,
      ConcurrentMap<PrivacyBudgetUnit, PrivacyBudgetKeyInput>
          privacyBudgetUnitToPrivacyBudgetKeyInput,
      Set<UUID> reportIdSet,
      ImmutableSet<UnsignedLong> filteringIds,
      Set<PrivacyBudgetUnit> privacyBudgetUnits) {
    super(reportIdSet, filteringIds, privacyBudgetKeyGeneratorFactory);
    this.aggregationMap = aggregationMap;
    this.privacyBudgetUnitToPrivacyBudgetKeyInput = privacyBudgetUnitToPrivacyBudgetKeyInput;
    this.privacyBudgetUnits = privacyBudgetUnits;
  }
}
