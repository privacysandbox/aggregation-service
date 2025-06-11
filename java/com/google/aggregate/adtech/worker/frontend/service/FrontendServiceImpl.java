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

import com.google.common.base.Converter;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTask;
import com.google.aggregate.adtech.worker.frontend.tasks.GetJobTask;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.CreateJobResponseProto.CreateJobResponse;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto.GetJobResponse;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.scp.shared.api.exception.ServiceException;

/** Handles business logic for the frontend service */
public final class FrontendServiceImpl implements FrontendService {

  private final Converter<CreateJobRequest, RequestInfo> createJobRequestToRequestInfoConverter;
  private final Converter<JobMetadata, GetJobResponse> getJobResponseConverter;
  private final CreateJobTask createJobTask;
  private final GetJobTask getJobTask;

  /** Creates a new instance of the {@code FrontendServiceImpl} class. */
  @Inject
  FrontendServiceImpl(
      CreateJobTask createJobTask,
      GetJobTask getJobTask,
      Converter<JobMetadata, GetJobResponse> getJobResponseConverter,
      Converter<CreateJobRequest, RequestInfo> createJobRequestToRequestInfoConverter) {
    this.createJobTask = createJobTask;
    this.getJobTask = getJobTask;
    this.getJobResponseConverter = getJobResponseConverter;
    this.createJobRequestToRequestInfoConverter = createJobRequestToRequestInfoConverter;
  }

  /** Creates the job from the request, then returns the response. */
  public CreateJobResponse createJob(CreateJobRequest createJobRequest) throws ServiceException {
    createJobTask.createJob(createJobRequestToRequestInfoConverter.convert(createJobRequest));
    return CreateJobResponse.newBuilder().build();
  }

  /** Gets the job with the provided ID. */
  public GetJobResponse getJob(String jobRequestId) throws ServiceException {
    return this.getJobResponseConverter.convert(getJobTask.getJob(jobRequestId));
  }
}
