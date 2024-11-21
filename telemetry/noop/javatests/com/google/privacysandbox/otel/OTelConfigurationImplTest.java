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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class OTelConfigurationImplTest {

  private static final Clock CLOCK = Clock.getDefault();
  private static final AttributeKey<String> JOB_ID_KEY = AttributeKey.stringKey("job-id");
  // SPAN_ATTRIBUTES map allows user to add multiple attributes such as job-id or report-id to span.
  private static final ImmutableMap<String, String> SPAN_ATTRIBUTES =
      ImmutableMap.of("job-id", "testJob", "report-id", "abcd");
  private final Resource RESOURCE = Resource.getDefault();
  private InMemorySpanExporter spanExporter;
  private InMemoryMetricReader metricReader;
  private OTelConfiguration oTelConfigurationImpl;

  @Before
  public void setUp() {
    // Reset the OpenTelemetry object for tests
    OTelConfiguration.resetForTest();

    // Setup trace provider
    spanExporter = InMemorySpanExporter.create();
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .setResource(RESOURCE)
            .setClock(CLOCK)
            .build();

    // Setup meter provider
    metricReader = InMemoryMetricReader.create();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .setResource(RESOURCE)
            .setClock(CLOCK)
            .registerMetricReader(metricReader)
            .build();

    oTelConfigurationImpl = new OTelConfigurationImpl();
  }

  @Test
  public void createDebugCounter_isAlwaysEmpty() {
    String counterName = "counter";
    long counterValue1 = 3;
    long counterValue2 = 4;
    LongCounter counter = oTelConfigurationImpl.createDebugCounter(counterName);

    counter.add(counterValue1);
    counter.add(counterValue2);

    List<MetricData> allMetrics =
        metricReader.collectAllMetrics().stream().collect(toImmutableList());
    assertThat(allMetrics).isEmpty();
  }

  @Test
  public void createProdCounter_isAlwaysEmpty() {
    String counterName = "counter";
    long counterValue1 = 3;
    long counterValue2 = 4;
    LongCounter counter = oTelConfigurationImpl.createProdCounter(counterName);

    counter.add(counterValue1);
    counter.add(counterValue2);

    List<MetricData> allMetrics =
        metricReader.collectAllMetrics().stream().collect(toImmutableList());
    assertThat(allMetrics).isEmpty();
  }

  @Test
  public void createProdMemoryUtilizationGauge_isNull() {
    oTelConfigurationImpl.createProdMemoryUtilizationGauge();

    assertGaugeIsNull();
  }

  @Test
  public void createProdMemoryUtilizationRatioGauge_isNull() {
    oTelConfigurationImpl.createProdMemoryUtilizationRatioGauge();

    assertGaugeIsNull();
  }

  @Test
  public void createProdCPUUtilizationGauge_isNull() {
    oTelConfigurationImpl.createProdCPUUtilizationGauge();

    assertGaugeIsNull();
  }

  @Test
  public void createDebugTimerStarted_isAlwaysEmpty() {
    String timerName = "debugTimer";

    try (Timer ignore = oTelConfigurationImpl.createDebugTimerStarted(timerName)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createProdTimerStarted_isAlwaysEmpty() {
    String timerName = "prodTimer";

    try (Timer ignore = oTelConfigurationImpl.createProdTimerStarted(timerName)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createDebugTimerStarted_isAlwaysEmptyEvenWithJobID() {
    String timerName = "debugTimer";
    String jobID = "1234";

    try (Timer ignore = oTelConfigurationImpl.createDebugTimerStarted(timerName, jobID)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createProdTimerStarted_isAlwaysEmptyEvenWithJobID() {
    String timerName = "prodTimer";
    String jobID = "2234";

    try (Timer ignore = oTelConfigurationImpl.createProdTimerStarted(timerName, jobID)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createDebugTimerStarted_isEmptyWhenSetsAttributes() {
    String timerName = "debugTimer";

    try (Timer ignore =
        oTelConfigurationImpl.createDebugTimerStarted(timerName, SPAN_ATTRIBUTES)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createProdTimerStarted_isEmptyWhenSetsAttributes() {
    String timerName = "prodTimer";

    try (Timer ignore = oTelConfigurationImpl.createProdTimerStarted(timerName, SPAN_ATTRIBUTES)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createDebugTimerStarted_isEmptyInNested() {
    String timerName1 = "debugTimer1";
    String timerName2 = "debugTimer2";

    try (Timer ignore = oTelConfigurationImpl.createDebugTimerStarted(timerName1)) {
      try (Timer ignored = oTelConfigurationImpl.createDebugTimerStarted(timerName2)) {}
    }
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createProdTimerStarted_isEmptyInNested() {
    try (Timer ignore = oTelConfigurationImpl.createProdTimerStarted("prodTimer1")) {
      try (Timer ignored = oTelConfigurationImpl.createProdTimerStarted("prodTimer2")) {}
    }
    try (Timer ignored = oTelConfigurationImpl.createProdTimerStarted("prodTimer3")) {}

    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createDebugTimerStarted_isEmptyForAddEvent() {
    String timerName = "debugTimer";
    String eventName = "debugEvent";

    try (Timer timer = oTelConfigurationImpl.createDebugTimerStarted(timerName)) {
      timer.addEvent(eventName);
    }
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  @Test
  public void createProdTimerStarted_isEmptyForAddEvent() {
    String timerName = "prodTimer";
    String eventName = "prodEvent";

    try (Timer timer = oTelConfigurationImpl.createProdTimerStarted(timerName)) {
      timer.addEvent(eventName);
    }
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }

  private void assertGaugeIsNull() {
    List metric = metricReader.collectAllMetrics().stream().collect(toImmutableList());

    assertThat(metric.size()).isEqualTo(0);
  }

  @Test
  public void createProdTimerStart_setTimeUnitSeconds() {
    String timerName = "prodTimer";
    TimerUnit timeUnit = TimerUnit.SECONDS;
    String jobID = "job1";

    try (Timer timer = oTelConfigurationImpl.createProdTimerStarted(timerName, jobID, timeUnit)) {}
    List<SpanData> spanItems = spanExporter.getFinishedSpanItems();

    assertThat(spanItems).isEmpty();
  }
}
