/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.adtech.worker.aggregation.engine;

import static com.google.common.collect.Sets.newConcurrentHashSet;

import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import javax.inject.Inject;

/**
 * Factory for creating AggregationEngine object.
 */
public class AggregationEngineFactory {

  private final PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  @Inject
  AggregationEngineFactory(PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory) {
    this.privacyBudgetKeyGeneratorFactory = privacyBudgetKeyGeneratorFactory;
  }

  /**
   * Creates AggregationEngine object with queried filteringId.
   */
  public AggregationEngine create(ImmutableSet<UnsignedLong> filteringIds) {
    // Number of logical cores available to the JVM is used to hint the concurrent map maker. Any
    // number will work, this is just a hint that is passed to the map maker, but different values
    // may result in different performance.
    //
    // NOTE: when JVM runtime is probed, it returns the *logical* number of cores. This can be
    // different from the number of physical cores available on the machine, e.g. if hyperthreading
    // is used, the number obtained here is 2x larger than the number of physical cores.
    int concurrentMapConcurrencyHint = Runtime.getRuntime().availableProcessors();

    ConcurrentMap<BigInteger, LongAdder> aggregationMap =
        new MapMaker().concurrencyLevel(concurrentMapConcurrencyHint).makeMap();
    Set<PrivacyBudgetingServiceBridge.PrivacyBudgetUnit> privacyBudgetUnits =
        newConcurrentHashSet();
    Set<UUID> reportIdSet = newConcurrentHashSet();

    // null and zero are to be treated as the same.
    ImmutableSet.Builder<UnsignedLong> filteringIdsEnhanced = new ImmutableSet.Builder<>();
    filteringIdsEnhanced.addAll(filteringIds);
    if (filteringIds.isEmpty()) {
      filteringIdsEnhanced.add(UnsignedLong.ZERO);
    }

    return new AggregationEngine(
        privacyBudgetKeyGeneratorFactory,
        aggregationMap,
        privacyBudgetUnits,
        reportIdSet,
        filteringIdsEnhanced.build());
  }

  /**
   * Creates AggregationEngine object.
   * @deprecated Deprecated in favor of the {@code create(filteringIds)}.
   * <p>TODO(b/292494729): Remove this method with Privacy Budget Labels implementation.
   */
  public AggregationEngine create() {
    return create(ImmutableSet.of());
  }
}
