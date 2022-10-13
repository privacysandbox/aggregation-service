/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.testing.utils;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

/** Util functions for generating Apache http clients. */
public final class HttpClientTestUtils {

  private HttpClientTestUtils() {}

  /**
   * Creates an http client that will retry a request up to maxRetryAttempts number of times. It
   * will wait retryWaitTime in between each retries. The status codes defined in
   * retryableStatusCodes determine whether a request should be retried.
   *
   * @param retryableStatusCodes http status codes to retry.
   * @param maxRetryAttempts maximum number of times a request can be retried.
   * @param retryWaitTime time to wait before retrying a request.
   * @return
   */
  public static CloseableHttpClient makeRetryableHttpClient(
      ImmutableSet<Integer> retryableStatusCodes, int maxRetryAttempts, long retryWaitTime) {
    return HttpClients.custom()
        .addInterceptorLast(
            (HttpResponseInterceptor)
                (httpResponse, httpContext) -> {
                  if (retryableStatusCodes.contains(httpResponse.getStatusLine().getStatusCode())) {
                    throw new IOException("Retry request");
                  }
                })
        .setServiceUnavailableRetryStrategy(
            new ServiceUnavailableRetryStrategy() {
              @Override
              public boolean retryRequest(
                  HttpResponse httpResponse, int executionCount, HttpContext httpContext) {
                return executionCount < maxRetryAttempts
                    && retryableStatusCodes.contains(httpResponse.getStatusLine().getStatusCode());
              }

              @Override
              public long getRetryInterval() {
                return retryWaitTime;
              }
            })
        .build();
  }
}
