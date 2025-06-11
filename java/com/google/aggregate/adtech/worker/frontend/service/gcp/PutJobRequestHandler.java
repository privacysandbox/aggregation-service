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

import static com.google.scp.shared.api.model.Code.ACCEPTED;
import static com.google.scp.shared.gcp.util.CloudFunctionUtil.createCloudFunctionResponseFromProto;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cmrt.sdk.job_service.v1.PutJobRequest;
import com.google.cmrt.sdk.job_service.v1.PutJobResponse;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandlerBase;
import java.io.IOException;

/** Handles requests to CreateJob Http Cloud Function and returns HTTP Response. */
public class PutJobRequestHandler
    extends CloudFunctionRequestHandlerBase<PutJobRequest, PutJobResponse> {

  private final FrontendService frontendService;

  /** Creates a new instance of the {@code CreateJobRequestHandler} class. */
  @Inject
  public PutJobRequestHandler(FrontendService frontendService) {
    this.frontendService = frontendService;
  }

  @Override
  protected PutJobRequest toRequest(HttpRequest httpRequest) throws ServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected PutJobResponse processRequest(PutJobRequest createJobRequest) throws ServiceException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void toCloudFunctionResponse(HttpResponse httpResponse, PutJobResponse response)
      throws IOException {
    createCloudFunctionResponseFromProto(
        httpResponse, response, ACCEPTED.getHttpStatusCode(), allHeaders());
  }
}
