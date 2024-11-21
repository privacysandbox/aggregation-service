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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Map;

/**
 * Interface for management of {@link OpenTelemetry} resources.
 *
 * <p>Exposes methods to create different meters - counter, gauges and timer
 */
public interface OTelConfiguration {

  /** Creates a gauge meter that periodically exports memory utilization ratio */
  void createProdMemoryUtilizationRatioGauge();

  /** Creates a gauge meter that periodically exports memory utilization */
  void createProdMemoryUtilizationGauge();

  /** Creates a gauge meter that periodically exports CPU utilization */
  void createProdCPUUtilizationGauge();

  /**
   * Creates a counter meter in debug and prod environments
   *
   * @param name {@link String}
   * @return {@link LongCounter}
   */
  LongCounter createProdCounter(String name);

  /**
   * Creates a counter meter in debug environments only
   *
   * @param name {@link String}
   * @return {@link LongCounter}
   */
  LongCounter createDebugCounter(String name);

  /**
   * Creates a {@link Timer} given name in both debug and prod environments
   *
   * @param name {@link String}
   * @return {@link Timer}
   */
  Timer createProdTimerStarted(String name);

  /**
   * Creates a {@link Timer} given name in both debug environments only
   *
   * @param name {@link String}
   * @return {@link Timer}
   */
  Timer createDebugTimerStarted(String name);

  /**
   * Creates a {@link Timer} given name in both debug environments only. Adds jobID to its
   * attributes.
   *
   * @param name {@link String}
   * @param jobID {@link String}
   * @return {@link Timer}
   */
  Timer createDebugTimerStarted(String name, String jobID);

  /**
   * Creates a {@link Timer} given name in both debug environments only. Add attributes to its span
   * attributes.
   *
   * @param name {@link String}
   * @param attributeMap {@link Map}
   * @return {@link Timer}
   */
  Timer createDebugTimerStarted(String name, Map attributeMap);

  /**
   * Creates a {@link Timer} given name in both debug and prod environments
   *
   * @param name {@link String}
   * @param jobID {@link String}
   * @return {@link Timer}
   */
  Timer createProdTimerStarted(String name, String jobID);

  /**
   * Creates a {@link Timer} given name in both debug and prod environments
   *
   * @param name {@link String}
   * @param jobID {@link String}
   * @param timeUnit {@link String}
   * @return {@link Timer}
   */
  Timer createProdTimerStarted(String name, String jobID, TimerUnit timeUnit);

  /**
   * Creates a {@link Timer} given name in both prod environments only. Add attributes to its span
   * attributes.
   *
   * @param name {@link String}
   * @param attributeMap {@link Map}
   * @return {@link Timer}
   */
  Timer createProdTimerStarted(String name, Map attributeMap);

  /**
   * Resets OTel object for unit tests
   *
   * <p>WARNING: Only use in unit tests and not in application code.
   */
  static void resetForTest() {
    GlobalOpenTelemetry.resetForTest();
  }
}
