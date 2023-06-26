/*
 * Copyright 2023 Google LLC
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

package com.google.privacysandbox.otel;

import com.google.inject.AbstractModule;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;

/**
 * A module which binds necessary and common classes for initializing all types of {@link
 * OTelConfiguration}.
 */
public class OTelConfigurationModule extends AbstractModule {

  @Override
  public void configure() {
    bind(Resource.class).toInstance(Resource.getDefault());
    bind(Clock.class).toInstance(Clock.getDefault());
    bind(Duration.class).toInstance(Duration.ofMinutes(1L));
  }
}
