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

package com.google.aggregate.adtech.worker.shared.injection.modules;

import com.google.auto.service.AutoService;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Provides an implementation for AWS clients using the EnvironmentVariableCredentialsProvider and
 * uses the LocalStack endpoint override if ran as a Lambda via LocalStack.
 */
@AutoService(BaseAwsClientsModule.class)
public final class AwsClientsModule extends AwsClientsConfigurableModule {

  @Override
  protected void configureModule() {}

  @Override
  protected Optional<String> getOverrideEndpointUrl() {
    return Optional.ofNullable(System.getenv(EnvironmentVariables.AWS_ENDPOINT_URL_ENV_VAR));
  }

  @Override
  protected Region getRegion() {
    return Region.of(System.getenv(EnvironmentVariables.AWS_REGION_ENV_VAR));
  }

  @Override
  protected AwsCredentialsProvider getCredentialsProvider() {
    return EnvironmentVariableCredentialsProvider.create();
  }
}
