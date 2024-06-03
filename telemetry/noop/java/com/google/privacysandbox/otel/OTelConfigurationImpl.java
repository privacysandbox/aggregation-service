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

import com.google.errorprone.annotations.MustBeClosed;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Map;

/**
 * Implements {@link OTelConfiguration} for production use. Provides concrete implementations for
 * "no-op" methods. Uses {@link OTelConfigurationImplHelper} helper class.
 */
public final class OTelConfigurationImpl implements OTelConfiguration {

  private static final NoopLongCounter NOOP_LONG_COUNTER = new NoopLongCounter();
  private static final NoopTimer NOOP_TIMER = new NoopTimer();

  @Override
  public void createProdMemoryUtilizationRatioGauge() {}

  @Override
  public void createProdMemoryUtilizationGauge() {}

  @Override
  public void createProdCPUUtilizationGauge() {}

  @Override
  public LongCounter createProdCounter(String name) {
    return NOOP_LONG_COUNTER;
  }

  @Override
  public LongCounter createDebugCounter(String name) {
    return NOOP_LONG_COUNTER;
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name, String jobID) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createDebugTimerStarted(String name, Map attributeMap) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, String jobID) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, String jobID, TimerUnit unit) {
    return NOOP_TIMER;
  }

  @Override
  @MustBeClosed
  public Timer createProdTimerStarted(String name, Map attributeMap) {
    return NOOP_TIMER;
  }
}
