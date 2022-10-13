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

package com.google.aggregate.privacy.noise;

import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.function.Supplier;

public final class ThresholdSupplierModule extends AbstractModule {

  @Provides
  @Threshold
  Supplier<Double> provideThresholdSupplier(Supplier<PrivacyParameters> privacyParametersSupplier) {
    return new ThresholdSupplier(privacyParametersSupplier);
  }
}
