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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Optional;

/**
 * n-Query fact holding the aggregation key and other data
 *
 * <p>Serialization to and from CBOR is done using the classes provided via the Jackson annotations
 * (the JSON annotations are used by Jackson for CBOR as well).
 */
@AutoValue
@JsonSerialize(using = FactSerializer.class)
@JsonDeserialize(using = FactDeserializer.class)
public abstract class Fact {

  public static Builder builder() {
    return new AutoValue_Fact.Builder();
  }

  /** 128-bit aggregation bucket */
  public abstract BigInteger bucket();

  /** Value associated with the bucket. */
  public abstract Long value();

  /** Filtering id for the contribution. */
  public abstract Optional<UnsignedLong> id();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setBucket(BigInteger aggregationKey);

    public abstract Builder setValue(long value);

    public abstract Builder setId(UnsignedLong id);

    public abstract Fact build();
  }
}
