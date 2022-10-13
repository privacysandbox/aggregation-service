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

package com.google.aggregate.privacy.noise.testing.integration;

import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier.FakeNoiseApplier;
import com.google.aggregate.privacy.noise.testing.proto.NoiseTester.DpNoiseTesterParams;
import com.google.aggregate.privacy.noise.testing.proto.NoiseTester.DpNoiseTesterParams.NoiseValueType;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class NoisedSamplesGeneratorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject DpNoiseTesterParamsSupplier dpNoiseTesterParamsSupplier;

  // Under test.
  @Inject NoisedSamplesGenerator noisedSamplesGenerator;

  @TestParameter NoiseValueType noiseValueType;

  private FakeNoiseApplier.Builder fakeNoiseBuilder;
  private Consumer<Iterator<Long>> fakeNoiseSetter;

  @Before
  public void setUp() {
    // Call noise or count FakeNoiseApplier based on test parameter noiseValueType.
    fakeNoiseBuilder = FakeNoiseApplier.builder();
    fakeNoiseSetter = fakeNoiseBuilder::setNextValueNoiseToAdd;
  }

  @Test
  public void generateNoisedSamples() {
    dpNoiseTesterParamsSupplier.setDpNoiseTesterParams(
        DpNoiseTesterParams.newBuilder()
            .setNumberOfSamples(2)
            .setRawValue(2)
            .setDistance(1)
            .setNoiseValueType(noiseValueType)
            .build());
    fakeNoiseSetter.accept(ImmutableList.of(5L, -5L).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(fakeNoiseBuilder.build());

    Long[] result = noisedSamplesGenerator.generateNoisedSamples();

    // rawValue + noise
    assertThat(result).asList().containsExactly(7L, -3L);
  }

  @Test
  public void generateShiftedNoisedSamples() {
    dpNoiseTesterParamsSupplier.setDpNoiseTesterParams(
        DpNoiseTesterParams.newBuilder()
            .setNumberOfSamples(2)
            .setRawValue(2)
            .setDistance(1)
            .setNoiseValueType(noiseValueType)
            .build());
    fakeNoiseSetter.accept(ImmutableList.of(5L, -5L).iterator());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(fakeNoiseBuilder.build());

    Long[] shiftedResult = noisedSamplesGenerator.generateShiftedNoisedSamples();

    // rawValue + noise + shift
    assertThat(shiftedResult).asList().containsExactly(8L, -2L);
  }

  /**
   * Guice Module to configure {@link NoisedSamplesGenerator} to generates samples from {@link
   * FakeNoiseApplierSupplier}.
   */
  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(DpNoiseTesterParamsSupplier.class).in(TestScoped.class);
    }

    @Provides
    Supplier<DpNoiseTesterParams> provideDpNoiseTesterParams(DpNoiseTesterParamsSupplier supplier) {
      return supplier;
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }
  }
}
