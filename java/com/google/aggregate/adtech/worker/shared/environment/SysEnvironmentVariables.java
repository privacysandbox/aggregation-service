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

package com.google.aggregate.adtech.worker.shared.environment;

/**
 * Provides static access to environment variables via the EnvironmentVariablesProvider interface.
 * The default provider accesses {@code System.getenv} variables.
 */
public final class SysEnvironmentVariables {

  /** Provider for environment variables, with a default {@code System.getenv} provider. */
  static EnvironmentVariablesProvider Provider = new SystemEnvironmentVariablesProviderImpl();

  /** Gets the value of a given environment variable. */
  public static String getenv(String name) {
    return Provider.getenv(name);
  }

  /** Sets the environment variable provider used to service environment variable requests. */
  public static void setProvider(EnvironmentVariablesProvider provider) {
    Provider = provider;
  }
}
