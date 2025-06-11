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
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.aggregate.adtech.worker.frontend.injection.factories.AwsFailedJobQueueWriteCleanupFactory;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import java.util.Collection;
import java.util.Set;

/** Lambda handler to cleanup jobs that failed to be written to the job queue for processing. */
public final class AwsFailedJobQueueWriteCleanup implements RequestHandler<SQSEvent, Void> {

  private final DdbStreamBatchInfoParser batchInfoParser;
  private final DdbStreamJobMetadataLookup jobMetadataLookup;
  private final Set<JobMetadataChangeHandler> changeHandlers;

  /** Creates a new instance of the {@code AwsFailedJobQueueWriteCleanup} class. */
  public AwsFailedJobQueueWriteCleanup() {
    changeHandlers = AwsFailedJobQueueWriteCleanupFactory.getJobMetadataChangeHandlers();
    batchInfoParser = AwsFailedJobQueueWriteCleanupFactory.getDdbStreamBatchInfoParser();
    jobMetadataLookup = AwsFailedJobQueueWriteCleanupFactory.getDdbStreamJobMetadataLookup();
  }

  /** Cleanup jobs that failed to be written to the job queue for processing. */
  @Override
  public Void handleRequest(SQSEvent sqsEvent, Context context) {
    context.getLogger().log("Handling SQSEvent: " + sqsEvent);

    sqsEvent.getRecords().stream()
        .map(SQSMessage::getBody)
        .map(batchInfoParser::batchInfoFromMessageBody)
        .map(jobMetadataLookup::lookupInStream)
        .flatMap(Collection::stream)
        .peek(
            jobMetadata ->
                context.getLogger().log("handling JobMetadata from stream: " + jobMetadata))
        .forEach(
            jobMetadata ->
                changeHandlers.stream()
                    .filter(changeHandler -> changeHandler.canHandle(jobMetadata))
                    .forEach(changeHandler -> changeHandler.handle(jobMetadata)));

    return null;
  }
}
