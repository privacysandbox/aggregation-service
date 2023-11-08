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

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * GRPC configuration module which would send metric to the Otel collector endpoint but it's in
 * no-op library so no need to call any otel related SDK .
 */
public final class OtlpGrpcOtelConfigurationModule extends OTelConfigurationModule {
  @Provides
  @Singleton
  OTelConfiguration provideOtelConfig() {
    return new OTelConfigurationImpl();
  }
}
