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
   * Applies noise to values in a list of {@code AggregatedFact}.
   *
   * @return new {@code NoisedAggregationResult} and {@code AggregatedFact} with noising applied.
   */
  NoisedAggregationResult noise(
      Iterable<AggregatedFact> aggregatedFact, Optional<Double> debugPrivacyEpsilon);

  /**
   * Thresholds aggregated facts, only returning AggregatedFact with noised values greater than the
   * threshold. The Threshold value is determined by the privacy parameters.
   *
   * @param aggregatedFacts
   * @param debugPrivacyEpsilon
   * @return new {@code NoisedAggregationResult} and {@code AggregatedFact} thresholded.
   */
  NoisedAggregationResult threshold(
      Iterable<AggregatedFact> aggregatedFacts, Optional<Double> debugPrivacyEpsilon);
}
