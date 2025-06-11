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

package com.google.aggregate.adtech.worker.frontend.service.aws.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/**
 * POJO for the "DDBStreamBatchInfo" object used in a DynamoDb Stream Dead-letter-queue. Note: this
 * object does not use all of the fields in the DDBStreamBatchInfo object, it only stores those that
 * are needed to lookup from the stream. Additionally this object is from AWS so it uses camelCase
 * field names to match the JSON from AWS, unlike other JSON fields in this project which use
 * snake_case.
 *
 * <p>https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html#services-dynamodb-errors
 */
@AutoValue
@JsonDeserialize(builder = DdbStreamBatchInfo.Builder.class)
public abstract class DdbStreamBatchInfo {

  /** Returns a builder instance for this class. */
  public static Builder builder() {
    return new AutoValue_DdbStreamBatchInfo.Builder();
  }

  /** Returns the shard ID. */
  @JsonProperty("shardId")
  public abstract String shardId();

  /** Returns the start sequence number. */
  @JsonProperty("startSequenceNumber")
  public abstract String startSequenceNumber();

  /** Returns the batch size. */
  @JsonProperty("batchSize")
  public abstract int batchSize();

  /** Returns the stream ARN. */
  @JsonProperty("streamArn")
  public abstract String streamArn();

  /** Builder class for constructing {@code DdbStreamBatchInfo} instances. */
  @AutoValue.Builder
  @JsonIgnoreProperties(ignoreUnknown = true) // ignoreUnknown since not all fields are used
  public abstract static class Builder {

    /** Returns a builder instance for constructing {@code DdbStreamBatchInfo} instances. */
    @JsonCreator
    public static Builder builder() {
      return new AutoValue_DdbStreamBatchInfo.Builder();
    }

    /** Set the shard ID. */
    @JsonProperty("shardId")
    public abstract Builder shardId(String shardId);

    /** Set the start sequence number. */
    @JsonProperty("startSequenceNumber")
    public abstract Builder startSequenceNumber(String startSequenceNumber);

    /** Set the batch size. */
    @JsonProperty("batchSize")
    public abstract Builder batchSize(int batchSize);

    /** Set the stream ARN. */
    @JsonProperty("streamArn")
    public abstract Builder streamArn(String streamArn);

    /** Returns a new {@code DdbStreamBatchInfo} instance from the builder. */
    public abstract DdbStreamBatchInfo build();
  }
}
