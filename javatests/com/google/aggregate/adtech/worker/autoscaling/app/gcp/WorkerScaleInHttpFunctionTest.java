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
import com.google.scp.shared.api.model.HttpMethod;
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

import static com.google.scp.shared.api.model.Code.NOT_FOUND;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class WorkerScaleInHttpFunctionTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpRequest httpRequest;
  @Mock private HttpResponse httpResponse;
  @Mock WorkerScaleInRequestHandler workerScaleInRequestHandler;

  private WorkerScaleInHttpFunction cloudFunction;

  @Before
  public void setUp() throws IOException {
    cloudFunction = new WorkerScaleInHttpFunction(workerScaleInRequestHandler);
    StringWriter httpResponseOut = new StringWriter();
    BufferedWriter writerOut = new BufferedWriter(httpResponseOut);
    lenient().when(httpResponse.getWriter()).thenReturn(writerOut);
  }

  @Test
  public void service_manageInstancesApiSupported() throws Exception {
    when(httpRequest.getMethod()).thenReturn(HttpMethod.POST.name());
    when(httpRequest.getPath()).thenReturn("/manageInstances");

    cloudFunction.service(httpRequest, httpResponse);

    verify(workerScaleInRequestHandler).handleRequest(httpRequest, httpResponse);
  }

  @Test
  public void service_manageInstancesTrailingCharactersFails() throws Exception {
    when(httpRequest.getMethod()).thenReturn(HttpMethod.POST.name());
    when(httpRequest.getPath()).thenReturn("/manageInstancesss");

    cloudFunction.service(httpRequest, httpResponse);

    verify(httpResponse).setStatusCode(eq(NOT_FOUND.getHttpStatusCode()));
    verify(workerScaleInRequestHandler, never()).handleRequest(httpRequest, httpResponse);
  }
}
