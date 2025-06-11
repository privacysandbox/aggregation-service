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

package com.google.aggregate.adtech.worker.frontend.testing;

import static com.google.common.truth.Truth.assertThat;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

/** Contains common assertions used for testing APIGatewayProxyResponseEvents. */
public final class ResponseEventAssertions {
  private static final Printer JSON_PRINTER = JsonFormat.printer();
  /**
   * Asserts that the content body of an APIGatewayProxyResponseEvent contains the JSON serialized
   * String of a given input.
   */
  public static <TBody extends Message> void assertThatResponseBodyContains(
      APIGatewayProxyResponseEvent responseEvent, TBody equalTo)
      throws InvalidProtocolBufferException {
    String bodyJson = JSON_PRINTER.includingDefaultValueFields().print(equalTo);
    assertThat(responseEvent.getBody()).isEqualTo(bodyJson);
  }

  public static void assertThatResponseBodyContains(Message actual, Message expected) {
    assertThat(actual).isEqualTo(expected);
  }
}
