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
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Representation of a single job.
 *
 * <p>Each aggregation request produces one job, thus the job contains fields from the request.
 */
@AutoValue
public abstract class Job {

  /** Returns a new instance of the builder for this class. */
  public static Builder builder() {
    return new com.google.aggregate.adtech.worker.jobclient.model.AutoValue_Job.Builder();
  }

  /** Job primary key. */
  @JsonProperty("job_key")
  public abstract JobKey jobKey();

  /** The status for the job. */
  @JsonProperty("job_status")
  public abstract JobStatus jobStatus();

  /** Job processing timeout. */
  @JsonProperty("job_processing_timeout")
  public abstract Duration jobProcessingTimeout();

  /** The request info for the job. */
  @JsonProperty("request_info")
  public abstract RequestInfo requestInfo();

  /** The result info for the job. */
  @JsonProperty("result_info")
  public abstract Optional<ResultInfo> resultInfo();

  /** The creation time for the job. */
  @JsonProperty("create_time")
  public abstract Instant createTime();

  /** The last updated time for the job. */
  @JsonProperty("update_time")
  public abstract Instant updateTime();

  /** The number of job processing attempts. */
  @JsonProperty("num_attempts")
  public abstract Integer numAttempts();

  /** The worker starts processing time for the job during the last attempt. */
  @JsonProperty("processing_start_time")
  public abstract Optional<Instant> processingStartTime();

  /** Returns a new builder instance from a {@code Job} instance. */
  public abstract Builder toBuilder();

  /** Builder for the {@code Job} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the job primary key. */
    public abstract Builder setJobKey(JobKey jobKey);

    /** Set the job status. */
    public abstract Builder setJobStatus(JobStatus jobStatus);

    /** Set the job processing timeout. */
    public abstract Builder setJobProcessingTimeout(Duration jobProcessingTimeout);

    /** Set the request info for the job. */
    public abstract Builder setRequestInfo(RequestInfo requestInfo);

    /** Set the result info for the job. */
    public abstract Builder setResultInfo(ResultInfo resultInfo);

    /** Set the result info for the job. */
    public abstract Builder setResultInfo(Optional<ResultInfo> resultInfo);

    /** Set the creation time for the job. */
    public abstract Builder setCreateTime(Instant createTime);

    /** Set the last updated time for the job. */
    public abstract Builder setUpdateTime(Instant updateTime);

    /** Set the number of job processing attempts. */
    public abstract Builder setNumAttempts(Integer numAttempts);

    /** Set the processing start time for the job during the last attempt. */
    public abstract Builder setProcessingStartTime(Optional<Instant> processingStartTime);

    /** Creates a new instance of the {@code Job} class from the builder. */
    public abstract Job build();
  }
}
