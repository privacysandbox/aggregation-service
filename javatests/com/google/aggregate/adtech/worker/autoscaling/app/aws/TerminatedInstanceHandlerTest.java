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

package com.google.aggregate.adtech.worker.autoscaling.app.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.ManageTerminatedInstanceTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class TerminatedInstanceHandlerTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock ManageTerminatedInstanceTask manageTerminatedInstanceTaskMock;
  @Mock Context contextMock;
  @Mock LambdaLogger loggerMock;

  @Before
  public void setup() {
    setupContext();
  }

  @Test
  public void handleRequest_success() {
    Map<String, Object> eventInfo = setupEventInfo();
    when(manageTerminatedInstanceTaskMock.manageTerminatedInstance(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    var terminatedInstanceHandler = new TerminatedInstanceHandler(manageTerminatedInstanceTaskMock);
    String result = terminatedInstanceHandler.handleRequest(eventInfo, contextMock);

    verify(manageTerminatedInstanceTaskMock)
        .manageTerminatedInstance(anyString(), anyString(), anyString(), anyString());
    Assert.assertEquals("Success!", result);
  }

  @Test
  public void handleRequest_failureToCompleteLifecycleAction() {
    Map<String, Object> eventInfo = setupEventInfo();
    AutoScalingException autoScalingException =
        (AutoScalingException)
            AutoScalingException.builder().message("Failed to complete lifecycle action").build();
    when(manageTerminatedInstanceTaskMock.manageTerminatedInstance(
            anyString(), anyString(), anyString(), anyString()))
        .thenThrow(autoScalingException);

    var terminatedInstanceHandler = new TerminatedInstanceHandler(manageTerminatedInstanceTaskMock);

    Assert.assertThrows(
        AutoScalingException.class,
        () -> terminatedInstanceHandler.handleRequest(eventInfo, contextMock));
  }

  private Map<String, Object> setupEventInfo() {
    Map<String, String> eventDetails = new LinkedHashMap<>();
    eventDetails.put("AutoScalingGroupName", "fakeGroup");
    eventDetails.put("EC2InstanceId", "fakeInstance");
    eventDetails.put("LifecycleHookName", "fakeLifecycleHook");
    eventDetails.put("LifecycleActionToken", "fakeLifecycleActionToken");

    Map<String, Object> eventInfo = new LinkedHashMap<>();
    eventInfo.put("detail", eventDetails);
    return eventInfo;
  }

  private void setupContext() {
    doNothing().when(loggerMock).log(anyString());
    when(contextMock.getLogger()).thenReturn(loggerMock);
  }
}
