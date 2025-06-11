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

package com.google.aggregate.adtech.worker.frontend.tasks.gcp;

import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DUPLICATE_JOB_MESSAGE;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;

import com.google.inject.Inject;
import com.google.protobuf.Timestamp;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTaskBase;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobKeyExistsException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerMetadataDb.MetadataDbSpannerTtlDays;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/** Task to create a job in GCP. */
public final class GcpCreateJobTask extends CreateJobTaskBase {

  private final JobMetadataDb jobMetadataDb;
  private final JobQueue jobQueue;
  private final Clock clock;
  private final int ttlDays;

  /** Creates a new instance of the {@code GcpCreateJobTask} class. */
  @Inject
  public GcpCreateJobTask(
      JobMetadataDb jobMetadataDb,
      JobQueue jobQueue,
      Clock clock,
      Set<RequestInfoValidator> requestInfoValidators,
      @MetadataDbSpannerTtlDays int ttlDays) {
    super(requestInfoValidators);
    this.jobMetadataDb = jobMetadataDb;
    this.jobQueue = jobQueue;
    this.clock = clock;
    this.ttlDays = ttlDays;
  }

  @Override
  public void createJob(RequestInfo requestInfo) throws ServiceException {
    validate(requestInfo);

    Instant now = clock.instant();
    Timestamp currentTime = ProtoUtil.toProtoTimestamp(now);
    Instant ttl = now.plus(ttlDays, ChronoUnit.DAYS);

    JobKey key = JobKey.newBuilder().setJobRequestId(requestInfo.getJobRequestId()).build();
    String serverJobId = UUID.randomUUID().toString();
    JobMetadata jobMetadata =
        JobMetadata.newBuilder()
            .setJobKey(key)
            .setRequestReceivedAt(currentTime)
            .setRequestUpdatedAt(currentTime)
            .setNumAttempts(0)
            .setJobStatus(RECEIVED)
            .setServerJobId(serverJobId)
            .setRequestInfo(requestInfo)
            .setTtl(ttl.getEpochSecond())
            .build();

    try {
      // Since we are enqueueing the job first, check if job already exists. Worker will handle edge
      // cases that get through this through race condition by checking the server job id.
      if (jobMetadataDb.getJobMetadata(key.getJobRequestId()).isPresent()) {
        throw new JobKeyExistsException("Job already exists.");
      }
      // It's important to enqueue the job first to make sure the job is processed
      jobQueue.sendJob(key, serverJobId);
      jobMetadataDb.insertJobMetadata(jobMetadata);
    } catch (JobMetadataDbException | JobQueueException e) {
      throw new ServiceException(
          Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE, e);
    } catch (JobKeyExistsException e) {
      throw new ServiceException(
          Code.ALREADY_EXISTS,
          ErrorReasons.DUPLICATE_JOB_KEY.toString(),
          String.format(DUPLICATE_JOB_MESSAGE, requestInfo.getJobRequestId()));
    }
  }
}
