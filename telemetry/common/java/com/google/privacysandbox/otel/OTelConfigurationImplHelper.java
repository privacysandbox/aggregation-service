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
import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import java.lang.management.ManagementFactory;
import java.util.Map;

/** Implements helper methods for {@link OTelConfiguration} implementations */
public final class OTelConfigurationImplHelper {
  private final Meter meter;
  private final Tracer tracer;

  OTelConfigurationImplHelper(Meter meter, Tracer tracer) {
    this.meter = meter;
    this.tracer = tracer;
  }

  /** Creates a gauge meter that periodically exports memory utilization ratio */
  public void createMemoryUtilizationRatioGauge() {
    meter
        .gaugeBuilder("process.runtime.jvm.memory.utilization_ratio")
        .setDescription("Memory utilization ratio")
        .setUnit("percent")
        .buildWithCallback(
            measurement -> {
              double usedMemory =
                  (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
              double ratio = usedMemory / Runtime.getRuntime().maxMemory();
              measurement.record((ratio) * 100.0);
            });
  }

  /** Creates a gauge meter that periodically exports memory utilization */
  public void createMemoryUtilizationGauge() {
    meter
        .gaugeBuilder("process.runtime.jvm.memory.utilization")
        .setDescription("Memory utilization")
        .setUnit("MiB")
        .buildWithCallback(
            measurement -> {
              double usedMemory =
                  (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
              measurement.record(usedMemory / (1024 * 1024));
            });
  }

  /** Creates a gauge meter that periodically exports CPU utilization */
  public void createCPUUtilizationGauge() {
    meter
        .gaugeBuilder("process.runtime.jvm.CPU.utilization")
        .setDescription("CPU utilization")
        .setUnit("percent")
        .buildWithCallback(
            measurement -> {
              OperatingSystemMXBean osBean =
                  ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
              Double CpuUsage = osBean.getProcessCpuLoad() * 100;
              measurement.record(CpuUsage);
            });
  }

  /**
   * Creates a {@link LongCounter} meter
   *
   * @param name {@link String}
   * @return {@link LongCounter}
   */
  public LongCounter createCounter(String name) {
    return meter.counterBuilder(name).build();
  }

  /**
   * Creates a {@link Timer}
   *
   * @param name {@link String}
   * @return {@link Timer}
   */
  @MustBeClosed
  public Timer createTimerStarted(String name) {
    SpanBuilder sp = tracer.spanBuilder(name).setNoParent();
    return new TimerImpl(sp);
  }

  /**
   * Creates a {@link Timer} and adds jobID to the span attributes
   *
   * @param name {@link String}
   * @param jobID {@link String}
   * @return {@link Timer}
   */
  @MustBeClosed
  public Timer createTimerStarted(String name, String jobID) {
    SpanBuilder sp = tracer.spanBuilder(name).setNoParent();
    return new TimerImpl(sp, jobID);
  }

  /**
   * Creates a {@link Timer} and adds attributes to the span attributes
   *
   * @param name {@link String}
   * @param attributesMap {@link Map}
   * @return {@link Timer}
   */
  @MustBeClosed
  public Timer createTimerStarted(String name, Map<String, String> attributesMap) {
    AttributesBuilder attributes = Attributes.empty().toBuilder();
    attributesMap.forEach((k, v) -> attributes.put(k, v));
    SpanBuilder sp = tracer.spanBuilder(name).setNoParent();
    return new TimerImpl(sp, attributes.build());
  }
}
