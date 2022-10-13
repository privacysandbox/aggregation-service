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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;

/** Materializes {@code AggregationResults} stream to ImmutableList. */
public final class MaterializedAggregationResults {

  volatile ImmutableList<AggregatedFact> materializedAggregations;

  /** Factory method to create {@code MaterializedAggregationResults} from a stream. */
  public static MaterializedAggregationResults of(Stream<AggregatedFact> aggregationResults) {
    return new MaterializedAggregationResults(aggregationResults.collect(toImmutableList()));
  }

  public synchronized ImmutableList<AggregatedFact> getMaterializedAggregations() {
    return materializedAggregations;
  }

  private MaterializedAggregationResults(ImmutableList<AggregatedFact> materializedAggregations) {
    this.materializedAggregations = materializedAggregations;
  }
}
