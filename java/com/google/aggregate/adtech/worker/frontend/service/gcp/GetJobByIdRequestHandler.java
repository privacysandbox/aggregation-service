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

import static com.google.scp.shared.api.model.Code.OK;
import static com.google.scp.shared.gcp.util.CloudFunctionUtil.createCloudFunctionResponseFromProtoPreservingFieldNames;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cmrt.sdk.job_service.v1.GetJobByIdRequest;
import com.google.cmrt.sdk.job_service.v1.GetJobByIdResponse;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandlerBase;
import java.io.IOException;

/** Handles requests to GetJob Http Cloud Function and returns HTTP Response. */
public class GetJobByIdRequestHandler
    extends CloudFunctionRequestHandlerBase<GetJobByIdRequest, GetJobByIdResponse> {

  private static final String JOB_REQUEST_ID_PARAM = "job_request_id";
  private final FrontendService frontendService;

  /** Creates a new instance of the {@code GetJobRequestHandler} class. */
  @Inject
  public GetJobByIdRequestHandler(FrontendService frontendService) {
    this.frontendService = frontendService;
  }

  @Override
  protected GetJobByIdRequest toRequest(HttpRequest httpRequest) throws ServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected GetJobByIdResponse processRequest(GetJobByIdRequest getJobRequest)
      throws ServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void toCloudFunctionResponse(HttpResponse httpResponse, GetJobByIdResponse response)
      throws IOException {
    createCloudFunctionResponseFromProtoPreservingFieldNames(
        httpResponse, response, OK.getHttpStatusCode(), allHeaders());
  }
}
