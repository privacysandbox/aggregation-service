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
import static com.google.scp.shared.api.model.Code.INVALID_ARGUMENT;
import static com.google.scp.shared.api.model.HttpMethod.POST;
import static com.google.scp.shared.api.util.RequestUtil.validateHttpMethod;
import static com.google.scp.shared.gcp.util.CloudFunctionUtil.createCloudFunctionResponseFromProto;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.gson.GsonFactory;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.inject.Inject;
import com.google.protobuf.util.JsonFormat;
import com.google.aggregate.adtech.worker.frontend.service.FrontendService;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.frontend.api.v1.CreateJobResponseProto.CreateJobResponse;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.gcp.util.CloudFunctionRequestHandlerBase;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Handles requests to CreateJob Http Cloud Function and returns HTTP Response. */
public class CreateJobRequestHandler
    extends CloudFunctionRequestHandlerBase<CreateJobRequest, CreateJobResponse> {

  private final FrontendService frontendService;

  /** Creates a new instance of the {@code CreateJobRequestHandler} class. */
  @Inject
  public CreateJobRequestHandler(FrontendService frontendService) {
    this.frontendService = frontendService;
  }

  @Override
  protected CreateJobRequest toRequest(HttpRequest httpRequest) throws ServiceException {
    try {
      validateHttpMethod(httpRequest.getMethod(), POST);
      String json = httpRequest.getReader().lines().collect(Collectors.joining());
      JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
      CreateJobRequest.Builder protoBuilder = CreateJobRequest.newBuilder();
      parser.merge(json, protoBuilder);

      // Gets the account identity (email) of the request and adds it to the protobuf.
      Optional<String> identity = getAccountIdentityFromHttpHeader(httpRequest);
      if (identity.isPresent()) {
        protoBuilder.setAccountIdentity(identity.get());
      }

      return protoBuilder.build();
    } catch (IOException exception) {
      throw new ServiceException(INVALID_ARGUMENT, ErrorReasons.JSON_ERROR.name(), exception);
    }
  }

  @Override
  protected CreateJobResponse processRequest(CreateJobRequest createJobRequest)
      throws ServiceException {
    return frontendService.createJob(createJobRequest);
  }

  @Override
  protected void toCloudFunctionResponse(HttpResponse httpResponse, CreateJobResponse response)
      throws IOException {
    createCloudFunctionResponseFromProto(
        httpResponse, response, ACCEPTED.getHttpStatusCode(), allHeaders());
  }

  /** Gets the account identity (email) of the request and adds it to the protobuf. */
  protected Optional<String> getAccountIdentityFromHttpHeader(HttpRequest httpRequest)
      throws IOException {
    // `email` is contained within a JWT which must be extracted from the auth portion of the http
    // header.
    List<String> auth = httpRequest.getHeaders().get("Authorization");
    if (auth != null && !auth.isEmpty()) {
      // Get the first element of the Authorization list. This list will be of the form of
      // `[bearer <JWT>]`.
      // We then split on the whitespace to extract the JWT alone.
      String authHeader = auth.get(0);
      String[] authHeaderParts = authHeader.split(" ");
      if (authHeaderParts.length == 2 && authHeaderParts[0].equals("bearer")) {
        // Decode the JWT and insert it into the protobuf.
        String jwt = (authHeaderParts[1]);
        GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
        GoogleIdToken idToken = GoogleIdToken.parse(gsonFactory, jwt);
        String accountIdentity = idToken.getPayload().getEmail();
        if (accountIdentity != null && !accountIdentity.isEmpty()) {
          return Optional.of(accountIdentity);
        }
      }
    }
    return Optional.empty();
  }
}
