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
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.GetMessageCountTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.SetDesiredCapacityTask;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class AsgCapacityHandlerTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock GetMessageCountTask getMessageCountTaskMock;
  @Mock SetDesiredCapacityTask setDesiredCapacityTaskMock;
  @Mock ScheduledEvent scheduledEvent;
  @Mock Context context;

  @Test
  public void handleRequest_success() throws ServiceException {
    setupGetMessageCountTask(1);
    setupSetDesiredCapacityTask(1, 2);

    var asgCapacityHandler =
        new AsgCapacityHandler(getMessageCountTaskMock, setDesiredCapacityTaskMock);
    String result = asgCapacityHandler.handleRequest(scheduledEvent, context);

    Mockito.verify(getMessageCountTaskMock).getTotalMessages();
    Mockito.verify(setDesiredCapacityTaskMock).setAsgDesiredCapacity(1);
    Assert.assertEquals("Success! Desired capacity:2", result);
  }

  @Test
  public void handleRequestTest_failToSetDesiredCapacity() throws ServiceException {
    setupGetMessageCountTask(1);
    ServiceException exception = new ServiceException(Code.ABORTED, "INVALID_STATE", "test");
    when(setDesiredCapacityTaskMock.setAsgDesiredCapacity(any(Integer.class))).thenThrow(exception);

    var asgCapacityHandler =
        new AsgCapacityHandler(getMessageCountTaskMock, setDesiredCapacityTaskMock);
    Assert.assertThrows(
        IllegalStateException.class,
        () -> asgCapacityHandler.handleRequest(scheduledEvent, context));
  }

  private void setupGetMessageCountTask(Integer totalMessages) {
    when(getMessageCountTaskMock.getTotalMessages()).thenReturn(totalMessages);
  }

  private void setupSetDesiredCapacityTask(Integer totalMessages, Integer desiredCapacity)
      throws ServiceException {
    when(setDesiredCapacityTaskMock.setAsgDesiredCapacity(totalMessages))
        .thenReturn(desiredCapacity);
  }
}
