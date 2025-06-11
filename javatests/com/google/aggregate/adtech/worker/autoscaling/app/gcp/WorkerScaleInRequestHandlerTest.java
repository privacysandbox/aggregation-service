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

package com.google.aggregate.adtech.worker.autoscaling.app.gcp;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.ManageTerminatingWaitInstancesTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.RequestScaleInTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.RequestUpdateTask;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class WorkerScaleInRequestHandlerTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpRequest httpRequest;
  @Mock private HttpResponse httpResponse;
  @Mock private ManageTerminatingWaitInstancesTask manageInstancesTask;
  @Mock private RequestScaleInTask requestScaleInTask;
  @Mock private RequestUpdateTask requestUpdateTask;

  private StringWriter httpResponseOut;
  private BufferedWriter writerOut;
  private WorkerScaleInRequestHandler workerScaleInRequestHandler;

  @Before
  public void setUp() throws IOException {
    workerScaleInRequestHandler =
        new WorkerScaleInRequestHandler(manageInstancesTask, requestScaleInTask, requestUpdateTask);
    httpResponseOut = new StringWriter();
    writerOut = new BufferedWriter(httpResponseOut);
    when(httpResponse.getWriter()).thenReturn(writerOut);
  }

  @Test
  public void handleRequest_success() throws Exception {
    when(manageInstancesTask.manageInstances()).thenReturn(new HashMap<>());
    when(requestUpdateTask.requestUpdate(anyMap())).thenReturn(new HashMap<>());
    doNothing().when(requestScaleInTask).requestScaleIn(anyMap());

    workerScaleInRequestHandler.handleRequest(httpRequest, httpResponse);
    verify(requestUpdateTask, times(1)).requestUpdate(anyMap());
    verify(requestScaleInTask, times(1)).requestScaleIn(anyMap());
  }
}
