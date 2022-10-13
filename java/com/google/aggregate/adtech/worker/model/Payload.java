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

package com.google.aggregate.adtech.worker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * The report's payload data to be aggregated.
 *
 * <p>Jackson JSON annotations are added for converting to/from CBOR with Jackson. CBOR uses the
 * same annotations as JSON.
 *
 * <p>Intended to match the spec here:
 * https://github.com/WICG/conversion-measurement-api/blob/d732ca597b5efbfdeb44523879a2c9cf2fcf4aa1/AGGREGATE.md#encrypted-payload
 */
@AutoValue
@JsonDeserialize(builder = Payload.Builder.class)
@JsonSerialize(as = Payload.class)
public abstract class Payload {

  // The only allowed value for operation
  public static final String HISTOGRAM_OPERATION = "histogram";

  public static Builder builder() {
    return new AutoValue_Payload.Builder().setOperation(HISTOGRAM_OPERATION);
  }

  // Aggregation operation to be performed, only "histogram" is supported
  @JsonProperty("operation")
  public abstract String operation();

  @JsonProperty("data")
  public abstract ImmutableList<Fact> data();

  @AutoValue.Builder
  @JsonIgnoreProperties(ignoreUnknown = true) // "padding" field is ignored
  public abstract static class Builder {

    // Used by Jackson for CBOR deserialization
    @JsonCreator
    public static Payload.Builder builder() {
      return new AutoValue_Payload.Builder();
    }

    @JsonProperty("operation")
    public abstract Builder setOperation(String operation);

    abstract ImmutableList.Builder<Fact> dataBuilder();

    public Builder addFact(Fact fact) {
      dataBuilder().add(fact);
      return this;
    }

    @JsonProperty("data")
    public Builder addAllFact(Iterable<Fact> facts) {
      dataBuilder().addAll(facts);
      return this;
    }

    public abstract Payload build();
  }
}
