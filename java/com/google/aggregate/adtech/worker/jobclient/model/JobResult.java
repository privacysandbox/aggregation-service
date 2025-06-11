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

package com.google.aggregate.adtech.worker.jobclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;

/**
 * Object used by the aggregation worker to ask the {@code JobHandlerModule} to record the job
 * completion.
 */
@AutoValue
public abstract class JobResult {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new com.google.aggregate.adtech.worker.jobclient.model.AutoValue_JobResult.Builder();
  }

  /** Creates a {@link Builder} with values copied over. Used for creating an updated object. */
  public abstract Builder toBuilder();

  /** Primary key of the job. */
  @JsonProperty("job_key")
  public abstract JobKey jobKey();

  /** The result information that will be put in the DB. */
  @JsonProperty("result_info")
  public abstract ResultInfo resultInfo();

  /** Builder for the {@code JobResult} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the primary key of the job. */
    public abstract Builder setJobKey(JobKey jobKey);

    /** Set the result information that will be put in the DB. */
    public abstract Builder setResultInfo(ResultInfo resultInfo);

    /** Create a new instance of the {@code JobResult} class from the builder. */
    public abstract JobResult build();
  }
}
