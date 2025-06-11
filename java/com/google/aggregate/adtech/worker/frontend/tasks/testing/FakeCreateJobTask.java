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

package com.google.aggregate.adtech.worker.frontend.tasks.testing;

import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DUPLICATE_JOB_MESSAGE;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;

import com.google.inject.Inject;
import com.google.protobuf.Timestamp;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTask;
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
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Fake implementation of {@link CreateJobTask} for use in tests. */
public final class FakeCreateJobTask extends CreateJobTaskBase {

  private final JobMetadataDb jobMetadataDb;
  private final JobQueue jobQueue;
  private final Clock clock;

  /** Creates a new instance of the {@code FakeCreateJobTask} class. */
  @Inject
  public FakeCreateJobTask(
      JobMetadataDb jobMetadataDb,
      JobQueue jobQueue,
      Clock clock,
      Set<RequestInfoValidator> requestInfoValidators) {
    super(requestInfoValidators);
    this.jobMetadataDb = jobMetadataDb;
    this.jobQueue = jobQueue;
    this.clock = clock;
  }

  @Override
  public void createJob(RequestInfo requestInfo) throws ServiceException {
    validate(requestInfo);

    Timestamp currentTime = ProtoUtil.toProtoTimestamp(Instant.now(clock));
    JobKey key = JobKey.newBuilder().setJobRequestId(requestInfo.getJobRequestId()).build();
    String serverJobId = UUID.randomUUID().toString();
    JobMetadata jobMetadata =
        JobMetadata.newBuilder()
            .setJobKey(key)
            .setServerJobId(serverJobId)
            .setRequestReceivedAt(currentTime)
            .setRequestUpdatedAt(currentTime)
            .setNumAttempts(0)
            .setJobStatus(RECEIVED)
            .setRequestInfo(requestInfo)
            .build();

    try {
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
