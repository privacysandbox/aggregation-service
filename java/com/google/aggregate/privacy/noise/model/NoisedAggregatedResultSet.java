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

import com.google.auto.value.AutoValue;
import java.util.Optional;

/** Aggregated result set (for one job) */
@AutoValue
public abstract class NoisedAggregatedResultSet {

  public abstract NoisedAggregationResult noisedResult();

  public abstract Optional<NoisedAggregationResult> noisedDebugResult();

  public static Builder builder() {
    return new AutoValue_NoisedAggregatedResultSet.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public static Builder builder() {
      return new AutoValue_NoisedAggregatedResultSet.Builder();
    }

    public abstract Builder setNoisedResult(NoisedAggregationResult value);

    public abstract Builder setNoisedDebugResult(NoisedAggregationResult value);

    public abstract NoisedAggregatedResultSet build();
  }
}
