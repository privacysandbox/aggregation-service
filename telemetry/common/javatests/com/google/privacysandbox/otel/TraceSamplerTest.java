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

package com.google.privacysandbox.otel;

import static com.google.common.truth.Truth.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceSamplerTest {
  private static final Context sampledContext =
      Context.root()
          .with(
              Span.wrap(
                  SpanContext.create("", "", TraceFlags.getDefault(), TraceState.getDefault())));
  private static final Random random = new Random(123);

  @Test
  public void shouldSample_withoutPerReportKeyWord_acceptsAllSamples() {
    TraceSampler traceSampler = TraceSampler.create(/* sampleRatio= */ 0.1, random);
    // The sampler would always sample because there no "per_report" in span name.
    String[] spanNames = {
      "total_execution_time", "pbs_latency", "summary_write_time", "reports_process_time"
    };

    for (String spanName : spanNames) {
      assertSampleProbability(traceSampler, spanName, 1, false);
    }
  }

  @Test
  public void shouldSample_withPerReportKeyWord_highSampleRate() {
    TraceSampler traceSampler = TraceSampler.create(/* sampleRatio= */ 0.9, random);
    String spanName = "decryption_time_per_report";

    assertSampleProbability(traceSampler, spanName, 0.9, true);
  }

  @Test
  public void shouldSample_withPerReportKeyWord_lowSampleRate() {
    TraceSampler traceSampler = TraceSampler.create(/* sampleRatio= */ 0.1, random);
    String spanName = "decryption_time_per_report";

    assertSampleProbability(traceSampler, spanName, 0.1, true);
  }

  private static void assertSampleProbability(
      Sampler sampler, String spanName, double probability, boolean allowTolerance) {
    int sampleCount = 0;
    int numberOfTries = 1000;
    double tolerance = allowTolerance ? 0.1 : 0;

    for (int i = 0; i < numberOfTries; i++) {
      if (sampler
          .shouldSample(
              sampledContext,
              "",
              spanName,
              SpanKind.INTERNAL,
              Attributes.empty(),
              Collections.emptyList())
          .equals(SamplingResult.recordAndSample())) {
        sampleCount += 1;
      }
    }
    double sampleRatio = (double) sampleCount / (double) numberOfTries;
    if (tolerance == 0) {
      assertThat(sampleRatio).isEqualTo(probability);
    } else {
      assertThat(sampleRatio).isGreaterThan(probability - tolerance);
      assertThat(sampleRatio).isLessThan(probability + tolerance);
    }
  }
}
