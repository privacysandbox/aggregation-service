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

package com.google.aggregate.privacy.noise.testing;

import com.google.aggregate.privacy.noise.JobScopedPrivacyParams;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.NoisedAggregationRunnerImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.function.Supplier;
import javax.inject.Singleton;

/** The Guice module to be installed when constant noise needs to be added to the results. */
public final class ConstantNoiseModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(NoisedAggregationRunner.class).to(NoisedAggregationRunnerImpl.class);
  }

  /** Provides the same object for fake noising (so parameters can be set dynamically) */
  @Singleton
  @Provides
  ConstantNoiseApplier provideZeroNoiseApplier() {
    // TODO: enable passing of noising values through CLI
    return new ConstantNoiseApplier(/* noise= */ 0);
  }

  /** Provides the supplier for the noise applier */
  @Singleton
  @Provides
  FakeNoiseApplierSupplier provideZeroNoiseApplierSupplier(
      ConstantNoiseApplier constantNoiseApplier) {
    FakeNoiseApplierSupplier supplier = new FakeNoiseApplierSupplier();
    supplier.setFakeNoiseApplier(constantNoiseApplier);
    return supplier;
  }

  @Singleton
  @Provides
  Supplier<NoiseApplier> provideApplierSupplier(FakeNoiseApplierSupplier supplier) {
    return supplier;
  }

  /** Noise applier that applies provides constant noise, always */
  public static final class ConstantNoiseApplier implements NoiseApplier {

    private final long noise;

    public ConstantNoiseApplier(long noise) {
      this.noise = noise;
    }

    @Override
    public Long noiseMetric(Long metric, JobScopedPrivacyParams unused) {
      return metric + noise;
    }
  }
}
