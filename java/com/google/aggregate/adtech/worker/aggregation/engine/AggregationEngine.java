/*
 * Copyright 2025 Google LLC
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

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

/** Data engine interface for centrally aggregating facts coming in from different threads. */
public interface AggregationEngine {
  /**
   * Consumes a report by adding its individual facts to the aggregation Only reports with unique
   * report_id within a batch are used in aggregation
   */
  void accept(Report report);

  /**
   * Insert a new key with an empty fact. PBKs are not calculated for keys added using this method.
   */
  void accept(AggregationKey key);

  /** Get the aggregated value for a key or return the default value. */
  long getAggregatedValueOrDefault(AggregationKey key, long defaultValue);

  /**
   * Remove the key and aggregated value from the aggregation engine map.
   *
   * @return The aggregated value for the key.
   */
  OptionalLong remove(AggregationKey key);

  Stream<Entry<AggregationKey, LongAdder>> getEntries();

  boolean containsKey(AggregationKey key);

  Stream<AggregationKey> getKeySet();

  /**
   * Creates the materialized, in-memory aggregation for the accumulated facts (through reports).
   */
  // TODO: investigate enforcing call of makeAggregation strictly after all accepts.
  ImmutableMap<AggregationKey, AggregatedFact> makeAggregation();

  /** Gets a set of distinct privacy budget units observed during the aggregation */
  ImmutableList<PrivacyBudgetUnit> getPrivacyBudgetUnits();

  /** Gets a list of corresponding privacy budget inputs for given privacy budget units*/
  ImmutableList<PrivacyBudgetKeyInput> getPrivacyBudgetKeyInputsFromPrivacyBudgetUnits(ImmutableList<PrivacyBudgetUnit> privacyBudgetUnits);

  /** Holds the keys to group by in aggregation. */
  @AutoValue
  abstract class AggregationKey {

    public abstract BigInteger bucket();

    public static AggregationKey create(BigInteger bucket) {
      return new AutoValue_AggregationEngine_AggregationKey(bucket);
    }
  }
}
