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

package com.google.aggregate.shared.testutils.gcp;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * Container for spanner emulator. Reference: https://github.com/testcontainers/testcontainers-java
 */
public class SpannerEmulatorContainer extends GenericContainer<SpannerEmulatorContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME =
      DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator");

  private static final int GRPC_PORT = 9010;
  private static final int HTTP_PORT = 9020;

  public SpannerEmulatorContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);

    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

    withExposedPorts(GRPC_PORT, HTTP_PORT);
    setWaitStrategy(
        new LogMessageWaitStrategy().withRegEx(".*Cloud Spanner emulator running\\..*"));
  }

  /**
   * @return a <code>host:port</code> pair corresponding to the address on which the emulator's gRPC
   *     endpoint is reachable from the test host machine. Directly usable as a parameter to the
   *     com.google.cloud.spanner.SpannerOptions.Builder#setEmulatorHost(java.lang.String) method.
   */
  public String getEmulatorGrpcEndpoint() {
    return getContainerIpAddress() + ":" + getMappedPort(GRPC_PORT);
  }

  /**
   * @return a <code>host:port</code> pair corresponding to the address on which the emulator's HTTP
   *     REST endpoint is reachable from the test host machine.
   */
  public String getEmulatorHttpEndpoint() {
    return getContainerIpAddress() + ":" + getMappedPort(HTTP_PORT);
  }
}
