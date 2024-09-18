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

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.privacysandbox.otel.Annotations.EnableOTelLogs;
import com.google.privacysandbox.otel.Annotations.GrpcOtelCollectorEndpoint;
import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.Random;

/** GRPC configuration module which would send metric to the Otel collector endpoint. */
public final class OtlpGrpcOtelConfigurationModule extends OTelConfigurationModule {

  @Provides
  @Singleton
  OTelConfiguration provideOtelConfig(
      @GrpcOtelCollectorEndpoint String collectorEndpoint,
      @EnableOTelLogs Boolean enableOTelLogs,
      Duration duration,
      Resource resource,
      Clock clock) {
    Sampler traceSampler = TraceSampler.create(/* sampleRatio= */ 0.001, new Random());
    SpanExporter spanExporter =
        OtlpGrpcSpanExporter.builder().setEndpoint(collectorEndpoint).build();
    MetricReader metricReader =
        PeriodicMetricReader.builder(
                OtlpGrpcMetricExporter.builder().setEndpoint(collectorEndpoint).build())
            .setInterval(duration)
            .build();
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setSampler(traceSampler)
            .setResource(resource)
            .setClock(clock)
            .setIdGenerator(AwsXrayIdGenerator.getInstance())
            .build();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .setResource(resource)
            .setClock(clock)
            .registerMetricReader(metricReader)
            .build();

    OpenTelemetrySdkBuilder oTel =
        OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .setTracerProvider(sdkTracerProvider);
    if (enableOTelLogs) {
      SdkLoggerProvider sdkLoggerProvider =
          SdkLoggerProvider.builder()
              .setResource(resource)
              .addLogRecordProcessor(
                  BatchLogRecordProcessor.builder(
                          OtlpGrpcLogRecordExporter.builder()
                              .setEndpoint(collectorEndpoint)
                              .build())
                      .build())
              .build();
      oTel.setLoggerProvider(sdkLoggerProvider);
    }
    return new OTelConfigurationImpl(oTel.build());
  }
}
