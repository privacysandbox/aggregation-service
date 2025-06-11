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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.InvalidAttributeNameException;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

@RunWith(JUnit4.class)
public final class GetMessageCountTaskTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock SqsClient sqsClientMock;
  private static final String queueUrl = "test-queue";

  @Test
  public void getTotalMessages_checkValue() {
    setupSqsClient("2", "3");

    var getMessageCountTask = new GetMessageCountTask(sqsClientMock, queueUrl);
    Integer totalMessages = getMessageCountTask.getTotalMessages();

    Assert.assertEquals((Integer) 5, totalMessages);
  }

  @Test
  public void getTotalMessages_failToGetAttributes() {
    InvalidAttributeNameException exception =
        InvalidAttributeNameException.builder().message("Error getting attributes.").build();
    when(sqsClientMock.getQueueAttributes(any(GetQueueAttributesRequest.class)))
        .thenThrow(exception);

    var getMessageCountTask = new GetMessageCountTask(sqsClientMock, queueUrl);
    Assert.assertThrows(
        InvalidAttributeNameException.class, () -> getMessageCountTask.getTotalMessages());
  }

  private void setupSqsClient(String visibleMessages, String nonvisibleMessages) {
    Map<QueueAttributeName, String> attributeMap = new HashMap<>();
    attributeMap.put(APPROXIMATE_NUMBER_OF_MESSAGES, visibleMessages);
    attributeMap.put(APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, nonvisibleMessages);

    GetQueueAttributesResponse queueAttributesResponse =
        GetQueueAttributesResponse.builder().attributes(attributeMap).build();
    when(sqsClientMock.getQueueAttributes(any(GetQueueAttributesRequest.class)))
        .thenReturn(queueAttributesResponse);
  }
}
