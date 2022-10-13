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
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * Aggregator for a single fact, tracks count and sum
 *
 * <p>The aggregator is thread safe, leveraging highly optimized Java 8+ long accumulators.
 */
final class SingleFactAggregation implements Consumer<Fact> {

  private final LongAdder sum;

  SingleFactAggregation() {
    sum = new LongAdder();
  }

  @Override
  public void accept(Fact fact) {
    sum.add(fact.value());
  }

  /**
   * Gets the sum recorded for the aggregation, at the time of call (if other threads are still
   * pushing data into the aggregation, it may be inaccurate, but it is expected that sum is
   * collected only after everything is pushed).
   */
  long getSum() {
    return sum.longValue();
  }
}
