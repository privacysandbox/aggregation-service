/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.privacysandbox.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Random;

/**
 * A sampler that samples traces with specified sample rate. This sampler helps to conserve
 * resources by only generating and sending a subset of traces. It would sample all job level
 * traces. For example, all "total_execution_time" time would be sampled and sent. And it would
 * sample event or request level traces with specified sample rate. For example, only x% of
 * "decryption" time traces would be generated and sent.
 */
public class TraceSampler implements Sampler {

  private final double sampleRatio;
  private final Random random;

  public static TraceSampler create(double sampleRatio, Random random) {
    return new TraceSampler(sampleRatio, random);
  }

  private TraceSampler(double sampleRatio, Random random) {
    this.sampleRatio = sampleRatio;
    this.random = random;
  }

  /**
   * The sampler would always sample when there is no "per_report" in the span name. Otherwise, the
   * sampler would sample data at sample rate probability.
   */
  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceID,
      String spanName,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    if (spanName.contains("per_report")) {
      return random.nextDouble() < sampleRatio
          ? SamplingResult.recordAndSample()
          : SamplingResult.drop();
    } else {
      return SamplingResult.recordAndSample();
    }
  }

  @Override
  public String getDescription() {
    return "A sampler that samples based on trace's name.";
  }
}
