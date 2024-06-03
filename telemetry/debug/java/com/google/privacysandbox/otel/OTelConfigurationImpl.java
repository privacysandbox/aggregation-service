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

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.MustBeClosed;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Map;

/**
 * Implements {@link OTelConfiguration} for debug use. Provides concrete implementations for both
 * "prod" and "debug" methods. Uses {@link OTelConfigurationImplHelper} helper class.
 */
public final class OTelConfigurationImpl implements OTelConfiguration {

  private final OTelConfigurationImplHelper oTelConfigurationImplHelper;

  OTelConfigurationImpl(OpenTelemetry oTel) {
    this.oTelConfigurationImplHelper =
        new OTelConfigurationImplHelper(
            oTel.getMeter(OTelConfigurationImpl.class.getName()),
            oTel.getTracer(OTelConfigurationImpl.class.getName()));
  }

  @VisibleForTesting
  OTelConfigurationImpl(OTelConfigurationImplHelper oTelConfigurationImplHelper) {
    this.oTelConfigurationImplHelper = oTelConfigurationImplHelper;
  }

  @Override
  public void createProdMemoryUtilizationRatioGauge() {
    oTelConfigurationImplHelper.createMemoryUtilizationRatioGauge();
  }

  @Override
  public void createProdMemoryUtilizationGauge() {
    oTelConfigurationImplHelper.createMemoryUtilizationGauge();
  }

  @Override
  public void createProdCPUUtilizationGauge() {
    oTelConfigurationImplHelper.createCPUUtilizationGauge();
  }

  @Override
  public LongCounter createProdCounter(String name) {
    return oTelConfigurationImplHelper.createCounter(name);
  }

  @Override
  public LongCounter createDebugCounter(String name) {
    return oTelConfigurationImplHelper.createCounter(name);
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name) {
    return oTelConfigurationImplHelper.createTimerStarted(name);
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name) {
    return oTelConfigurationImplHelper.createTimerStarted(name);
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name, String jobID) {
    return oTelConfigurationImplHelper.createTimerStarted(name, jobID);
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name, Map attributeMap) {
    return oTelConfigurationImplHelper.createTimerStarted(name, attributeMap);
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, String jobID) {
    return oTelConfigurationImplHelper.createTimerStarted(name, jobID, TimerUnit.NANOSECONDS);
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, String jobID, TimerUnit timeUnit) {
    return oTelConfigurationImplHelper.createTimerStarted(name, jobID, timeUnit);
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, Map attributeMap) {
    return oTelConfigurationImplHelper.createTimerStarted(name, attributeMap);
  }
}
