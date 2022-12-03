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

package com.google.aggregate.adtech.worker;

import com.google.aggregate.privacy.noise.DpNoisedAggregationModule;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule;
import com.google.inject.Module;

/** CLI enum to select the noising implementation. */
public enum NoisingSelector {
  DP_NOISING(new DpNoisedAggregationModule()),
  CONSTANT_NOISING(new ConstantNoiseModule());

  private final Module noisingModule;

  NoisingSelector(Module noisingModule) {
    this.noisingModule = noisingModule;
  }

  public Module getNoisingModule() {
    return noisingModule;
  }
}
