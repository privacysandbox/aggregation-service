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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/** Used for testing REST requests. */
@JsonDeserialize(builder = TestCreateRequest.Builder.class)
@AutoValue
public abstract class TestCreateRequest {

  /** Returns a new builder instance for this class. */
  public static Builder builder() {
    return new AutoValue_TestCreateRequest.Builder();
  }

  /** Get the name. */
  @JsonProperty("name")
  public abstract String name();

  /** Get the ID. */
  @JsonProperty("id")
  public abstract int id();

  /** Builder class for creating instances of the {@code TestCreateRequest} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Returns a new builder instance for the {@code TestCreateRequest} class. */
    @JsonCreator
    public static Builder builder() {
      return new AutoValue_TestCreateRequest.Builder();
    }

    /** Set the name. */
    @JsonProperty("name")
    public abstract Builder name(String name);

    /** Set the ID. */
    @JsonProperty("id")
    public abstract Builder id(int id);

    /** Create a new instance of the {@code TestCreateRequest} class from the builder. */
    public abstract TestCreateRequest build();
  }
}
