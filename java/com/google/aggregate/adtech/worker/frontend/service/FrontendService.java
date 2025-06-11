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

package com.google.aggregate.adtech.worker.frontend.service;

import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.CreateJobResponseProto.CreateJobResponse;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto.GetJobResponse;
import com.google.scp.shared.api.exception.ServiceException;

/** Interface for service that handles business logic for front end. */
public interface FrontendService {

  /**
   * Creates a job for the aggregation service to process.
   *
   * @param createJobRequest the deserialized request body
   * @throws ServiceException if any errors occur, either due to system issues or from invalid user
   *     input (e.g. malformed request fields)
   */
  CreateJobResponse createJob(CreateJobRequest createJobRequest) throws ServiceException;

  /**
   * Retrieves the status of the job according to the metadata DB.
   *
   * @param jobRequestId the ID of the job
   * @throws ServiceException if any errors occur, either due to system issues or from invalid user
   *     input (e.g non-existent job).
   */
  GetJobResponse getJob(String jobRequestId) throws ServiceException;
}
