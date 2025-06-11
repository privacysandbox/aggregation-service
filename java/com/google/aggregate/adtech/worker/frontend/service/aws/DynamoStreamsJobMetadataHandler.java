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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.google.aggregate.adtech.worker.frontend.injection.factories.AwsChangeHandlerFactory;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import java.util.Optional;
import java.util.Set;

/**
 * Lambda function handler that is called when a Dynamo stream event is triggered for the
 * JobMetadata table.
 */
public final class DynamoStreamsJobMetadataHandler
    implements RequestHandler<DynamodbEvent, String> {

  private final DynamoStreamsJobMetadataUpdateChecker jobMetadataUpdateChecker;
  private final Set<JobMetadataChangeHandler> changeHandlers;

  /**
   * Creates a new instance of the {@code DynamoStreamsJobMetadataHandler} class. This constructor
   * is invoked by the AWS runtime so fields are supplied via factory.
   */
  public DynamoStreamsJobMetadataHandler() {
    this.jobMetadataUpdateChecker = AwsChangeHandlerFactory.getJobMetadataUpdateChecker();
    this.changeHandlers = AwsChangeHandlerFactory.getJobMetadataChangeHandlers();
  }

  /** Called when a Dynamo stream event is triggered for the JobMetadata table. */
  @Override
  public String handleRequest(DynamodbEvent dynamodbEvent, Context context) {
    // Filter out any events not associated with inserts or updates or don't have new images
    // configured
    dynamodbEvent.getRecords().stream()
        .map(jobMetadataUpdateChecker::checkForUpdatedMetadata)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(
            jobMetadata ->
                changeHandlers.stream()
                    .filter(i -> i.canHandle(jobMetadata))
                    .forEach(i -> i.handle(jobMetadata)));
    // Just return empty string since there is no output
    return "";
  }
}
