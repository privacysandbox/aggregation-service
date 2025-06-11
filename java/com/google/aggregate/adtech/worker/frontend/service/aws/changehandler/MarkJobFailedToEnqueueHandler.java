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

package com.google.aggregate.adtech.worker.frontend.service.aws.changehandler;

import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.FINISHED;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;
import static com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode.INTERNAL_ERROR;

import com.google.inject.Inject;
import com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataConflictException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.aggregate.adtech.worker.shared.model.BackendModelUtil;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marks jobs as failed in the DB. Used for cleanup in scenarios where the job couldn't be inserted
 * to the processing queue.
 */
public final class MarkJobFailedToEnqueueHandler implements JobMetadataChangeHandler {

  public static final String RETURN_MESSAGE =
      "Failed to add job to processing queue. Please re-submit as a new job";

  private static final Logger logger = LoggerFactory.getLogger(MarkJobFailedToEnqueueHandler.class);

  private final JobMetadataDb jobMetadataDb;
  private final Clock clock;

  /** Creates a new instance of the {@code MarkJobFailedToEnqueueHandler} class. */
  @Inject
  MarkJobFailedToEnqueueHandler(JobMetadataDb jobMetadataDb, Clock clock) {
    this.jobMetadataDb = jobMetadataDb;
    this.clock = clock;
  }

  @Override
  public boolean canHandle(JobMetadata jobMetadata) {
    return jobMetadata.getJobStatus().equals(RECEIVED);
  }

  /**
   * Marks the entry as failed, aborting the operation and returning successfully if another service
   * has updated the record.
   *
   * @throws ChangeHandlerException if an error reaching the db occurs
   */
  @Override
  public void handle(JobMetadata jobMetadata) {
    logger.info(
        "Marking job " + BackendModelUtil.toJobKeyString(jobMetadata.getJobKey()) + " as failed");
    JobMetadata failedJobMetadata =
        jobMetadata.toBuilder()
            .setJobStatus(FINISHED)
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setReturnCode(INTERNAL_ERROR.name())
                    .setReturnMessage(RETURN_MESSAGE)
                    .setErrorSummary(ErrorSummary.getDefaultInstance())
                    .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
                    .build())
            .build();
    try {
      jobMetadataDb.updateJobMetadata(failedJobMetadata);
    } catch (JobMetadataDbException e) {
      // Throw an unchecked exception for general DB failures
      throw new ChangeHandlerException(e);
    } catch (JobMetadataConflictException e) {
      // Swallow exception and log that a DB conflict occurred. DB conflicts will occur when another
      // service has updated the record so the cleanup is no longer required, thus no exception
      // should be thrown. This exception will occur in the scenario described here:
      // go/agg-frontend-cleanup#bookmark=id.t7eqfjz0iosw
      logDbConflict(jobMetadata);
    }
  }

  /**
   * Logs that a DB conflict exception had occurred with the updated record information.
   *
   * @throws ChangeHandlerException if an error reaching the db occurs
   */
  private void logDbConflict(JobMetadata jobMetadata) {
    try {
      Optional<JobMetadata> updatedJobMetadata =
          jobMetadataDb.getJobMetadata(BackendModelUtil.toJobKeyString(jobMetadata.getJobKey()));
      if (updatedJobMetadata.isPresent()) {
        logger.warn(
            "Detected DB conflict when marking job as failed. Job may have been updated by another"
                + " service. Updated JobMetadata from DB: "
                + updatedJobMetadata
                + " Outdated JobMetadata in stream: "
                + jobMetadata);
      }
    } catch (JobMetadataDbException e) {
      throw new ChangeHandlerException(e);
    }
  }
}
