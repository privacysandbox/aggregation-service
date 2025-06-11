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

package com.google.aggregate.adtech.worker.frontend.service.gcp;

import static com.google.scp.shared.api.exception.SharedErrorReason.INVALID_URL_PATH_OR_VARIABLE;
import static com.google.scp.shared.api.model.Code.INVALID_ARGUMENT;
import static com.google.scp.shared.api.model.Code.OK;
import static com.google.scp.shared.api.model.HttpMethod.GET;
import static com.google.scp.shared.api.util.RequestUtil.validateHttpMethod;
import static com.google.scp.shared.gcp.util.CloudFunctionUtil.createCloudFunctionResponseFromProtoPreservingFieldNames;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.protos.frontend.api.v1.GetJobRequestProto.GetJobRequest;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto.GetJobResponse;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandlerBase;
import java.io.IOException;
import java.util.Optional;

/** Handles requests to GetJob Http Cloud Function and returns HTTP Response. */
public class GetJobRequestHandler
    extends CloudFunctionRequestHandlerBase<GetJobRequest, GetJobResponse> {

  private static final String JOB_REQUEST_ID_PARAM = "job_request_id";
  private final FrontendService frontendService;

  /** Creates a new instance of the {@code GetJobRequestHandler} class. */
  @Inject
  public GetJobRequestHandler(FrontendService frontendService) {
    this.frontendService = frontendService;
  }

  @Override
  protected GetJobRequest toRequest(HttpRequest httpRequest) throws ServiceException {
    validateHttpMethod(httpRequest.getMethod(), GET);
    Optional<String> jobRequestId = httpRequest.getFirstQueryParameter(JOB_REQUEST_ID_PARAM);
    if (jobRequestId.isEmpty()) {
      throw new ServiceException(
          /* code= */ INVALID_ARGUMENT,
          /* reason= */ INVALID_URL_PATH_OR_VARIABLE.name(),
          /* message= */ String.format("%s query parameter is missing", JOB_REQUEST_ID_PARAM));
    }
    return GetJobRequest.newBuilder().setJobRequestId(jobRequestId.get()).build();
  }

  @Override
  protected GetJobResponse processRequest(GetJobRequest getJobRequest) throws ServiceException {
    return this.frontendService.getJob(getJobRequest.getJobRequestId());
  }

  @Override
  protected void toCloudFunctionResponse(HttpResponse httpResponse, GetJobResponse response)
      throws IOException {
    // Need to preserve field names because the old POJO model used snake case instead of camel case
    // for JSON field names, and by default proto would convert field names to camel case.
    createCloudFunctionResponseFromProtoPreservingFieldNames(
        httpResponse, response, OK.getHttpStatusCode(), allHeaders());
  }
}
