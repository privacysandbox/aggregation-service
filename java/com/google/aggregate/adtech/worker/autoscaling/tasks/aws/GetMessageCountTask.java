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

package com.google.aggregate.adtech.worker.autoscaling.tasks.aws;

import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsQueueUrl;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Gets the message count for the SQS job queue. */
public class GetMessageCountTask {
  private final SqsClient sqsClient;
  private final String queueUrl;

  @Inject
  public GetMessageCountTask(SqsClient sqsClient, @JobQueueSqsQueueUrl String queueUrl) {
    this.sqsClient = sqsClient;
    this.queueUrl = queueUrl;
  }

  /**
   * Returns the total messages from the SQS job queue using the sum of visible and non-visible
   * messages.
   */
  public Integer getTotalMessages() {
    List<QueueAttributeName> attributeNames =
        Arrays.asList(
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);
    GetQueueAttributesRequest getQueueAttributesRequest =
        GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(attributeNames)
            .build();
    GetQueueAttributesResponse getQueueAttributesResponse =
        sqsClient.getQueueAttributes(getQueueAttributesRequest);
    Map<QueueAttributeName, String> responseMap = getQueueAttributesResponse.attributes();

    Integer visibleMessages =
        Integer.parseInt(responseMap.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
    Integer nonvisibleMessages =
        Integer.parseInt(
            responseMap.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
    return visibleMessages + nonvisibleMessages;
  }
}
