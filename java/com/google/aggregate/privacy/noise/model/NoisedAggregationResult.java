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

package com.google.aggregate.privacy.noise.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;

/** AutoValue to store aggregation result after noising and associated {@code PrivacyParameters} */
@AutoValue
public abstract class NoisedAggregationResult {

  public static NoisedAggregationResult create(
      PrivacyParameters privacyParameters, ImmutableList<AggregatedFact> noisedAggregatedFacts) {
    return new AutoValue_NoisedAggregationResult(privacyParameters, noisedAggregatedFacts);
  }

  public abstract PrivacyParameters privacyParameters();

  public abstract ImmutableList<AggregatedFact> noisedAggregatedFacts();

  public static NoisedAggregationResult merge(
      NoisedAggregationResult first, NoisedAggregationResult second) {
    checkArgument(first.privacyParameters().equals(second.privacyParameters()));
    return NoisedAggregationResult.create(
        first.privacyParameters(),
        ImmutableList.copyOf(
            Iterables.concat(first.noisedAggregatedFacts(), second.noisedAggregatedFacts())));
  }

  // This methods will overwrite all original debugAnnotations in the aggregatedFact
  public static NoisedAggregationResult addDebugAnnotations(
      NoisedAggregationResult aggregationResult, List<DebugBucketAnnotation> debugAnnotations) {
    return NoisedAggregationResult.create(
        aggregationResult.privacyParameters(),
        aggregationResult.noisedAggregatedFacts().stream()
            .map(
                aggregatedFact ->
                    AggregatedFact.create(
                        aggregatedFact.getBucket(),
                        aggregatedFact.getMetric(),
                        aggregatedFact.getUnnoisedMetric().get(),
                        debugAnnotations))
            .collect(toImmutableList()));
  }
}
