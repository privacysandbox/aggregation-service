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

package com.google.aggregate.adtech.worker.configs;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Supplies the privacy parameters config parsed from the text proto integrated into the JAR */
@Singleton
public final class PrivacyParametersSupplier implements Supplier<PrivacyParameters> {

  private final Distribution noisingDistribution;
  private final double noisingEpsilon;
  private final long noisingL1Sensitivity;
  private final double noisingDelta;

  @Inject
  PrivacyParametersSupplier(
      @NoisingDistribution Distribution noisingDistribution,
      @NoisingEpsilon double noisingEpsilon,
      @NoisingL1Sensitivity long noisingL1Sensitivity,
      @NoisingDelta double noisingDelta) {
    this.noisingDistribution = noisingDistribution;
    this.noisingEpsilon = noisingEpsilon;
    this.noisingL1Sensitivity = noisingL1Sensitivity;
    this.noisingDelta = noisingDelta;
  }

  @Override
  public PrivacyParameters get() {
    return PrivacyParameters.newBuilder()
        .setNoiseParameters(
            NoiseParameters.newBuilder().setDistribution(noisingDistribution).build())
        .setDelta(noisingDelta)
        .setEpsilon(noisingEpsilon)
        .setL1Sensitivity(noisingL1Sensitivity)
        .build();
  }

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface NoisingDistribution {}

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface NoisingEpsilon {}

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface NoisingL1Sensitivity {}

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface NoisingDelta {}
}
