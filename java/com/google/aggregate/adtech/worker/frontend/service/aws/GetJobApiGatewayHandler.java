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

package com.google.aggregate.adtech.worker.frontend.service.aws;

import static com.google.scp.shared.api.model.Code.OK;
import static com.google.scp.shared.aws.util.LambdaHandlerUtil.createApiGatewayResponseFromProtoPreservingFieldNames;
import static java.util.function.Predicate.not;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.aggregate.adtech.worker.frontend.injection.factories.FrontendServicesFactory;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.protos.frontend.api.v1.GetJobRequestProto.GetJobRequest;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto.GetJobResponse;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.aws.util.ApiGatewayHandler;
import java.util.Map;
import java.util.Optional;

/** The API Gateway handler for the GetJobRequest method for the front end REST api service. */
public final class GetJobApiGatewayHandler
    extends ApiGatewayHandler<GetJobRequest, GetJobResponse> {

  private static final String JOB_REQUEST_ID_PARAM = "job_request_id";
  private static final ImmutableList<String> REQUIRED_PARAMS =
      ImmutableList.of(JOB_REQUEST_ID_PARAM);

  private final FrontendService frontendService;

  /** Creates a new instance of the {@code GetJobApiGatewayHandler} class. */
  public GetJobApiGatewayHandler() {
    frontendService = FrontendServicesFactory.getFrontendService();
  }

  @Override
  protected GetJobRequest toRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context)
      throws ServiceException {
    Map<String, String> queryParams =
        Optional.ofNullable(apiGatewayProxyRequestEvent.getQueryStringParameters())
            .orElse(ImmutableMap.of());
    ImmutableList<String> missingParams =
        REQUIRED_PARAMS.stream()
            .filter(not(queryParams::containsKey))
            .collect(ImmutableList.toImmutableList());

    if (!missingParams.isEmpty()) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.ARGUMENT_MISSING.toString(),
          String.format("Missing required query parameters: %s must be specified", missingParams));
    }

    if (queryParams.get(JOB_REQUEST_ID_PARAM).isBlank()) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.VALIDATION_FAILED.toString(),
          String.format("Required query parameter '%s' must not be empty", JOB_REQUEST_ID_PARAM));
    }
    return GetJobRequest.newBuilder()
        .setJobRequestId(queryParams.get(JOB_REQUEST_ID_PARAM))
        .build();
  }

  @Override
  protected GetJobResponse processRequest(GetJobRequest getJobRequest) throws ServiceException {
    return frontendService.getJob(getJobRequest.getJobRequestId());
  }

  @Override
  protected APIGatewayProxyResponseEvent toApiGatewayResponse(GetJobResponse response) {
    // Need to preserve field names because the old POJO model used snake case instead of camel case
    // for JSON field names, and by default proto would convert field names to camel case.
    return createApiGatewayResponseFromProtoPreservingFieldNames(
        response, OK.getHttpStatusCode(), allHeaders());
  }
}
