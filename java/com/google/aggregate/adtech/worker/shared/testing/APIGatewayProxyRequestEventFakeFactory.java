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

package com.google.aggregate.adtech.worker.shared.testing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.Map;

/**
 * Creates an instance of {@code APIGatewayProxyRequestEvent} from a POST or GET. Used for testing.
 */
public final class APIGatewayProxyRequestEventFakeFactory {

  public static APIGatewayProxyRequestEvent createFromProtoPost(Message proto) {
    APIGatewayProxyRequestEvent proxyRequestEvent = new APIGatewayProxyRequestEvent();
    try {
      JsonFormat.Printer printer = JsonFormat.printer();
      proxyRequestEvent.setBody(printer.print(proto));
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    return proxyRequestEvent;
  }

  /**
   * Creates an instance of {@code APIGatewayProxyRequestEvent} with the query parameters from the
   * input.
   */
  public static APIGatewayProxyRequestEvent createFromGet(Map<String, String> queryParams) {
    APIGatewayProxyRequestEvent proxyRequestEvent = new APIGatewayProxyRequestEvent();
    proxyRequestEvent.setQueryStringParameters(queryParams);
    return proxyRequestEvent;
  }
}
