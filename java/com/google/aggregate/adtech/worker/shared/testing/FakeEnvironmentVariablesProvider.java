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

import com.google.aggregate.adtech.worker.shared.environment.EnvironmentVariablesProvider;
import java.util.HashMap;
import java.util.Map;

/** Provides a way to fake environment variables in testing. */
public class FakeEnvironmentVariablesProvider implements EnvironmentVariablesProvider {

  private final Map<String, String> environmentVariables;

  /** Creates a new instance of the {@code FakeEnvironmentVariablesProvider} class. */
  public FakeEnvironmentVariablesProvider() {
    environmentVariables = new HashMap<String, String>();
  }

  /** Gets the value of the environment variable at the given key. */
  public String getenv(String key) {
    return environmentVariables.get(key);
  }

  /** Sets the value of the environment variable at the given key. */
  public void setEnv(String key, String value) {
    environmentVariables.put(key, value);
  }
}
