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

package com.google.aggregate.privacy.noise;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.privacy.noise.model.NoisedAggregationResult;
import java.util.Optional;

/** Interface to apply Differential Private Noising to {@code AggregateFact}. */
public interface NoisedAggregationRunner {

  /**
   * Applies noise and optional threshold to values on a list of {@code AggregatedFact}.
   *
   * @return list of new {@code AggregationFact} with noising applied.
   */
  NoisedAggregationResult noise(
      Iterable<AggregatedFact> aggregatedFact,
      boolean doThreshold,
      Optional<Double> debugPrivacyEpsilon);
}
