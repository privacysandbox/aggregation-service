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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Scope;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Manages {@link Span}.
 *
 * <p>Starts a new, current {@link Span}. Span closes at the end of the try block.
 *
 * <p>Example usage:
 *
 * <pre><code>
 *   SpanBuilder spanBuilder;
 *   try (Timer t = new Timer(spanBuilder)) {
 *     // do stuff
 *     t.addEvent("event");
 *     // do stuff
 *   }
 * </code></pre>
 */
public class TimerImpl implements Timer {
  private final Span span;
  private final Scope scope;
  private final TimerUnit timeUnit;

  @SuppressWarnings("MustBeClosedChecker")
  TimerImpl(SpanBuilder sb) {
    timeUnit = TimerUnit.NANOSECONDS;
    span = sb.startSpan();
    scope = span.makeCurrent();
  }

  @SuppressWarnings("MustBeClosedChecker")
  TimerImpl(SpanBuilder sb, String jobID, TimerUnit unit) {
    timeUnit = unit;
    if (timeUnit.equals(TimerUnit.SECONDS)) {
      long timeMillis = System.currentTimeMillis();
      long timeSecond = TimeUnit.SECONDS.convert(timeMillis, TimeUnit.MILLISECONDS);
      sb.setStartTimestamp(timeSecond, TimeUnit.SECONDS);
    }
    span = sb.startSpan().setAttribute("job-id", jobID);
    scope = span.makeCurrent();
  }

  @SuppressWarnings("MustBeClosedChecker")
  TimerImpl(SpanBuilder sb, Attributes attributes) {
    timeUnit = TimerUnit.NANOSECONDS;
    span = sb.startSpan().setAllAttributes(attributes);
    scope = span.makeCurrent();
  }

  /**
   * Adds a new event within a {@link Span}
   *
   * @param text {@link String}
   */
  @Override
  public void addEvent(String text) {
    span.addEvent(text);
  }

  @Override
  public void close() {
    scope.close();
    if (timeUnit.equals(TimerUnit.SECONDS)) {
      long timeMillis = System.currentTimeMillis();
      long timeSecond = TimeUnit.SECONDS.convert(timeMillis, TimeUnit.MILLISECONDS);
      span.end(Instant.ofEpochSecond(timeSecond));
    } else {
      span.end();
    }
  }
}
