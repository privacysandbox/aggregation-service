/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.testing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;

/** Used for testing serialization. */
@AutoValue
public abstract class TestSerializedObject {

  /** Returns a new builder instance for this class. */
  public static Builder builder() {
    return new AutoValue_TestSerializedObject.Builder();
  }

  /** Get the timestamp. */
  @JsonProperty("timestamp")
  public abstract Instant timestamp();

  /** Get the name. */
  @JsonProperty("name")
  public abstract Optional<String> name();

  /** Get the address. */
  @JsonProperty("address")
  public abstract Optional<String> address();

  /** Builder class for creating instances of the {@code TestSerializedObject} class. */
  @AutoValue.Builder
  public interface Builder {

    /** Set the timestamp. */
    @JsonProperty("timestamp")
    Builder timestamp(Instant timestamp);

    /** Set the name. */
    @JsonProperty("name")
    Builder name(String name);

    /** Set the address. */
    @JsonProperty("address")
    Builder address(String address);

    /** Create a new instance of the {@code TestCreateRequest} class from the builder. */
    TestSerializedObject build();
  }
}
