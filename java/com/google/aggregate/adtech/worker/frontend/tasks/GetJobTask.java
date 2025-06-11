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

package com.google.aggregate.adtech.worker.frontend.tasks;

import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.JOB_NOT_FOUND_MESSAGE;

import com.google.inject.Inject;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;

/** Task to get a Job. */
public final class GetJobTask {

  private final JobMetadataDb jobMetadataDb;

  /** Creates a new instance of the {@code GetJobTask} class. */
  @Inject
  public GetJobTask(JobMetadataDb jobMetadataDb) {
    this.jobMetadataDb = jobMetadataDb;
  }

  /** Gets an existing job. */
  public JobMetadata getJob(String jobRequestId) throws ServiceException {
    try {
      return jobMetadataDb
          .getJobMetadata(jobRequestId)
          .orElseThrow(
              () ->
                  new ServiceException(
                      Code.NOT_FOUND,
                      ErrorReasons.JOB_NOT_FOUND.toString(),
                      String.format(JOB_NOT_FOUND_MESSAGE, jobRequestId)));
    } catch (JobMetadataDbException e) {
      throw new ServiceException(
          Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE, e);
    }
  }
}
