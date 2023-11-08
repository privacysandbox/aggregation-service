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
 * Container for pub sub emulator. Reference: https://github.com/testcontainers/testcontainers-java
 */
public final class PubSubEmulatorContainer extends GenericContainer<PubSubEmulatorContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME =
      DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk");

  private static final String CMD = "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085";
  private static final int PORT = 8085;

  public PubSubEmulatorContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);

    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

    withExposedPorts(8085);
    setWaitStrategy(new LogMessageWaitStrategy().withRegEx("(?s).*started.*$"));
    withCommand("/bin/sh", "-c", CMD);
  }

  /**
   * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
   *     reachable from the test host machine. Directly usable as a parameter to the
   *     io.grpc.ManagedChannelBuilder#forTarget(java.lang.String) method.
   */
  public String getEmulatorEndpoint() {
    return getContainerIpAddress() + ":" + getMappedPort(PORT);
  }
}
