/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.privacy.noise.model;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * Object to store aggregated results from the streaming implementation or the partially-streaming
 * implementation.
 */
@AutoValue
public abstract class AggregatedResults {

  public abstract Optional<SummaryReportAvroSet> summaryReportAvroSet();

  public abstract Optional<NoisedAggregatedResultSet> noisedAggregatedResultSet();

  public static AggregatedResults create(SummaryReportAvroSet summaryReportAvroSet) {
    return new AutoValue_AggregatedResults(Optional.of(summaryReportAvroSet), Optional.empty());
  }

  public static AggregatedResults create(NoisedAggregatedResultSet noisedAggregatedResultSet) {
    return new AutoValue_AggregatedResults(
        Optional.empty(), Optional.of(noisedAggregatedResultSet));
  }
}
