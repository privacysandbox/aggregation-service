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

import com.google.auto.value.AutoValue;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;

import java.time.Duration;
import java.util.Optional;

/** A class for defining parameters used to request retrying a job. */
@AutoValue
public abstract class JobRetryRequest {

  /** Returns a new instance of the {@code JobRetryRequest.Builder} class. */
  public static Builder builder() {
    return new AutoValue_JobRetryRequest.Builder();
  }

  /** Returns the JobKey. */
  public abstract JobKey getJobKey();

  /** Returns the desired wait period before allowing available workers to pick up the job. */
  public abstract Optional<Duration> getDelay();

  /** Returns the ResultInfo used to update the JobMetadata. */
  public abstract Optional<ResultInfo> getResultInfo();

  /** Builder class for the {@code JobRetryRequest} class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the JobKey. */
    public abstract Builder setJobKey(JobKey jobKey);

    /** Set the desired wait period before allowing available workers to pick up the job. */
    public abstract Builder setDelay(Duration delay);

    /** Set the job's ResultInfo. */
    public abstract Builder setResultInfo(ResultInfo delay);

    /** Returns a new instance of the {@code JobRetryRequest} class from the builder. */
    public abstract JobRetryRequest build();
  }
}
