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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.common.collect.ImmutableList;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeNoiseApplierTest {

  private static final ImmutableList<Long> NEXT_VALUE_NOISE_TO_ADD = ImmutableList.of(10L, -10L);

  private static FakeNoiseApplierSupplier fakeNoiseApplierSupplier;

  @Before
  public void setUp() {
    fakeNoiseApplierSupplier = new FakeNoiseApplierSupplier();
  }

  @Test
  public void noiseValue() {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextValueNoiseToAdd(NEXT_VALUE_NOISE_TO_ADD.iterator())
            .build());

    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(10L)).isEqualTo(20L);
    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(15L)).isEqualTo(5L);

    // Runs out of noise.
    assertThrows(
        NoSuchElementException.class, () -> fakeNoiseApplierSupplier.get().noiseMetric(5L));
  }

  @Test
  public void noiseCountAndValue() {
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextValueNoiseToAdd(NEXT_VALUE_NOISE_TO_ADD.iterator())
            .build());

    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(10L)).isEqualTo(20L);
    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(15L)).isEqualTo(5L);

    assertThrows(
        NoSuchElementException.class, () -> fakeNoiseApplierSupplier.get().noiseMetric(5L));
  }

  @Test
  public void noiseCountAndValueWithResettingIterator() {
    // Setup.
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextCountNoiseToAdd(ImmutableList.of(0L).iterator())
            .setNextValueNoiseToAdd(ImmutableList.of(0L).iterator())
            .build());

    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(10L)).isEqualTo(10L);

    // Reset.
    fakeNoiseApplierSupplier.setFakeNoiseApplier(
        FakeNoiseApplier.builder()
            .setNextCountNoiseToAdd(ImmutableList.of(2L).iterator())
            .setNextValueNoiseToAdd(ImmutableList.of(4L).iterator())
            .build());

    // Assert.
    assertThat(fakeNoiseApplierSupplier.get().noiseMetric(0L)).isEqualTo(4L);

    // Runs out of noise.
    assertThrows(
        NoSuchElementException.class, () -> fakeNoiseApplierSupplier.get().noiseMetric(0L));
  }
}
