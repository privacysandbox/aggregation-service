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

package com.google.aggregate.perf;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Provider;

/** Registry for named stopwatches; parts of the binary performance can be measured with this */
public final class StopwatchRegistry {

  private final Provider<Ticker> tickerProvider;
  private final ConcurrentMap<String, Stopwatch> registry;

  @Inject
  public StopwatchRegistry(Provider<Ticker> tickerProvider) {
    this.tickerProvider = tickerProvider;
    registry = new MapMaker().makeMap();
  }

  /**
   * Creates a Guava stopwatch with the given name. Overwrites the existing stopwatch if one already
   * exists.
   */
  public Stopwatch createStopwatch(String stopwatchName) {
    Stopwatch stopwatch = Stopwatch.createUnstarted(tickerProvider.get());
    registry.put(stopwatchName, stopwatch);
    return stopwatch;
  }

  /** Snapshots the elapsed time of all stopwatches */
  public ImmutableMap<String, Duration> collectStopwatchTimes() {
    return registry.entrySet().stream()
        .collect(
            toImmutableMap(
                Map.Entry::getKey, stopwatchEntry -> stopwatchEntry.getValue().elapsed()));
  }
}
