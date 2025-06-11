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

package com.google.aggregate.adtech.worker.jobclient.testing;

import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.JobGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Generates a fake Job for testing purposes. */
public final class FakeJobGenerator {

  private static final Instant REQUEST_RECEIVED_AT = Instant.parse("2019-10-01T08:25:24.00Z");
  private static final Instant REQUEST_PROCESSING_STARTED_AT =
      Instant.parse("2019-10-01T08:29:24.00Z");
  private static final Instant REQUEST_UPDATED_AT = Instant.parse("2019-10-01T08:29:24.00Z");

  /** Generates fake Job from {@code generateBuilder(id)}. */
  public static Job generate(String id) {
    return generateBuilder(id).build();
  }

  /** Generates fake Job Builder with {@param id} for all values for testing purposes. */
  public static Job.Builder generateBuilder(String id) {
    return Job.builder()
        .setJobKey(JobKey.newBuilder().setJobRequestId(id).build())
        .setJobProcessingTimeout(Duration.ofSeconds(3600))
        .setRequestInfo(JobGenerator.createFakeRequestInfo(id))
        .setCreateTime(REQUEST_RECEIVED_AT)
        .setUpdateTime(REQUEST_UPDATED_AT)
        .setProcessingStartTime(Optional.of(REQUEST_PROCESSING_STARTED_AT))
        .setJobStatus(JobStatus.IN_PROGRESS)
        .setNumAttempts(0);
  }
}
