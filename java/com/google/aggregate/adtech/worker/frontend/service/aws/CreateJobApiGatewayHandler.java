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

import static com.google.scp.shared.api.model.Code.ACCEPTED;
import static com.google.scp.shared.aws.util.LambdaHandlerUtil.createApiGatewayResponseFromProto;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.aggregate.adtech.worker.frontend.injection.factories.FrontendServicesFactory;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.CreateJobResponseProto.CreateJobResponse;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.aws.util.ApiGatewayHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** CreateJobApiGatewayHandler handles CreateJobRequests for the front end service rest api */
public final class CreateJobApiGatewayHandler
    extends ApiGatewayHandler<CreateJobRequest, CreateJobResponse> {

  private static final int INPUT_PREFIXES_LIST_MAX_ITEMS = 50;

  public static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";

  public static final String JOB_PARAM_REPORTING_SITE = "reporting_site";

  public static final String JOB_PARAM_INPUT_REPORT_COUNT = "input_report_count";
  private final FrontendService frontendService;

  /** Creates a new instance of the {@code CreateJobApiGatewayHandler} class. */
  public CreateJobApiGatewayHandler() {
    frontendService = FrontendServicesFactory.getFrontendService();
  }

  @Override
  protected CreateJobRequest toRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context)
      throws ServiceException {
    String json = apiGatewayProxyRequestEvent.getBody();
    CreateJobRequest.Builder protoBuilder = CreateJobRequest.newBuilder();
    try {
      parser.merge(json, protoBuilder);
      CreateJobRequest request = protoBuilder.build();
      validateProperties(request, json);

      return request;
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT, ErrorReasons.JSON_ERROR.toString(), e.getMessage());
    }
  }

  @Override
  protected CreateJobResponse processRequest(CreateJobRequest createJobRequest)
      throws ServiceException {
    return frontendService.createJob(createJobRequest);
  }

  @Override
  protected APIGatewayProxyResponseEvent toApiGatewayResponse(CreateJobResponse response) {
    // Return ACCEPTED code as the job has been queued and may not have started
    return createApiGatewayResponseFromProto(response, ACCEPTED.getHttpStatusCode(), allHeaders());
  }

  private static void validateProperties(CreateJobRequest request, String json)
      throws ServiceException {
    List<String> missingProps = new ArrayList<>();
    if (request.getJobRequestId().isEmpty()) {
      missingProps.add("jobRequestId");
    }
    if (request.getInputDataBucketName().isEmpty()) {
      missingProps.add("inputDataBucketName");
    }
    if (request.getOutputDataBlobPrefix().isEmpty()) {
      missingProps.add("outputDataBlobPrefix");
    }
    if (request.getOutputDataBucketName().isEmpty()) {
      missingProps.add("outputDataBucketName");
    }

    if (missingProps.size() > 0) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "Missing required properties: " + String.join(" ", missingProps) + "\r\n in: " + json);
    }
    /**
     * Exactly one of "input_data_blob_prefix" and "input_data_blob_prefixes" should be specified.
     * Throw error otherwise
     */
    if (!request.getInputDataBlobPrefixesList().isEmpty()
        == !request.getInputDataBlobPrefix().isEmpty()) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "Exactly one of the properties input_data_blob_prefix and input_data_blob_prefixes"
              + " must be provided\r\n"
              + " in: "
              + json);
    }
    if (request.getInputDataBlobPrefixesCount() > INPUT_PREFIXES_LIST_MAX_ITEMS) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "Property input_data_blob_prefixes should contain a maximum of "
              + INPUT_PREFIXES_LIST_MAX_ITEMS
              + " items: "
              + json);
    }
    validateReportingOriginAndSite(request, json);
    validateJobParamInputReportCount(request, json);
  }

  private static void validateJobParamInputReportCount(CreateJobRequest request, String json)
      throws ServiceException {
    if (!isAValidCount(request.getJobParametersMap().get(JOB_PARAM_INPUT_REPORT_COUNT))) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "Job parameter input_report_count should have a valid non-negative value: " + json);
    }
  }

  /**
   * Validates that exactly one of the two fields 'JOB_PARAM_ATTRIBUTION_REPORT_TO' and
   * 'reporting_site' is specified and the specified field is non-empty and not a comma-separated
   * list.
   */
  private static void validateReportingOriginAndSite(CreateJobRequest request, String json)
      throws ServiceException {
    Map<String, String> jobParams = request.getJobParametersMap();
    boolean bothSiteAndOriginSpecified =
        jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && jobParams.containsKey(JOB_PARAM_REPORTING_SITE);
    boolean neitherSiteOrOriginSpecified =
        !jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && !jobParams.containsKey(JOB_PARAM_REPORTING_SITE);
    if (bothSiteAndOriginSpecified || neitherSiteOrOriginSpecified) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "Exactly one of attribution_report_to and reporting_site fields should be"
              + " specified for the job. It is recommended to use"
              + " reporting_site"
              + " parameter. Parameter attribution_report_to will be"
              + " deprecated in the next"
              + " major version upgrade of the API: "
              + json);
    }
    // Verify that either the field 'JOB_PARAM_ATTRIBUTION_REPORT_TO' is not specified or is
    // non-empty.
    boolean emptyAttributionReportToSpecified =
        jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && jobParams.get(JOB_PARAM_ATTRIBUTION_REPORT_TO).trim().isEmpty();
    if (emptyAttributionReportToSpecified) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "The attribution_report_to field in the job parameters is empty: " + json);
    }
    // Verify that either the field 'reporting_site' is not specified or is non-empty.
    boolean emptyReportingSiteSpecified =
        jobParams.containsKey(JOB_PARAM_REPORTING_SITE)
            && jobParams.get(JOB_PARAM_REPORTING_SITE).trim().isEmpty();
    if (emptyReportingSiteSpecified) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "The reporting_site field in the job parameters is empty: " + json);
    }
    // Verify that either the field 'attribution_report_to' is not specified or is
    // a single value and not a comma-separated list of strings.
    boolean commaSeparatedStringListAttributionReportToSpecified =
        jobParams.containsKey(JOB_PARAM_ATTRIBUTION_REPORT_TO)
            && jobParams.get(JOB_PARAM_ATTRIBUTION_REPORT_TO).contains(",");
    if (commaSeparatedStringListAttributionReportToSpecified) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "The attribution_report_to field in the job parameters should contain a single value: "
              + json);
    }
    // Verify that either the field 'reporting_site' is not specified or is
    // a single value and not a comma-separated list of strings.
    boolean commaSeparatedStringListReportingSiteSpecified =
        jobParams.containsKey(JOB_PARAM_REPORTING_SITE)
            && jobParams.get(JOB_PARAM_REPORTING_SITE).contains(",");
    if (commaSeparatedStringListReportingSiteSpecified) {
      throw new ServiceException(
          Code.INVALID_ARGUMENT,
          ErrorReasons.JSON_ERROR.toString(),
          "The reporting_site field in the job parameters should contain a single value: " + json);
    }
  }

  /** Checks if the string represents a non-negative number or is empty. */
  private static boolean isAValidCount(String countInString) {
    return countInString == null
        || countInString.trim().isEmpty()
        || (Longs.tryParse(countInString.trim()) != null
            && Longs.tryParse(countInString.trim()) >= 0);
  }
}
