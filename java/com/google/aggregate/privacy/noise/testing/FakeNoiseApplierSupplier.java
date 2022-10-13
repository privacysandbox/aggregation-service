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

import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.auto.value.AutoValue;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Supplier of {@link NoiseApplier} that creates a new {@link FakeNoiseApplier} from noise and count
 * iterators for every call to {@code setFakeNoiseApplier}.
 */
public final class FakeNoiseApplierSupplier implements Supplier<NoiseApplier> {

  private NoiseApplier noiseApplier;

  @Override
  public NoiseApplier get() {
    return noiseApplier;
  }

  public void setFakeNoiseApplier(NoiseApplier fakeNoiseApplier) {
    noiseApplier = fakeNoiseApplier;
  }

  /**
   * Test Implementation of {@code NoiseApplier} that noises count/value from its respective
   * iterator.
   */
  @AutoValue
  public abstract static class FakeNoiseApplier implements NoiseApplier {

    @Override
    public Long noiseMetric(Long metric) {
      return metric + nextValueNoiseToAdd().get().next();
    }

    public static FakeNoiseApplier.Builder builder() {
      return new AutoValue_FakeNoiseApplierSupplier_FakeNoiseApplier.Builder();
    }

    abstract Optional<Iterator<Long>> nextCountNoiseToAdd();

    abstract Optional<Iterator<Long>> nextValueNoiseToAdd();

    @AutoValue.Builder
    public interface Builder {
      Builder setNextCountNoiseToAdd(Iterator<Long> nextCountNoiseToAdd);

      Builder setNextValueNoiseToAdd(Iterator<Long> nextValueNoiseToAdd);

      FakeNoiseApplier build();
    }
  }
}
