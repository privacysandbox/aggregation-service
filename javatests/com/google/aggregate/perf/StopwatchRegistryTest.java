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

package com.google.aggregate.perf;

import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.FakeTicker;
import com.google.inject.AbstractModule;
import java.time.Duration;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StopwatchRegistryTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject FakeTicker fakeTicker;

  // Under test
  @Inject StopwatchRegistry stopwatchRegistry;

  @Test
  public void genericStopwatches() {
    // Setup by injection.

    Stopwatch first = stopwatchRegistry.createStopwatch("first");
    Stopwatch second = stopwatchRegistry.createStopwatch("second");
    first.start();
    fakeTicker.advance(Duration.ofMillis(5));
    second.start();
    fakeTicker.advance(Duration.ofMillis(3));
    first.stop();
    second.stop();
    ImmutableMap<String, Duration> times = stopwatchRegistry.collectStopwatchTimes();

    assertThat(times)
        .containsExactly("first", Duration.ofMillis(8), "second", Duration.ofMillis(3));
  }

  @Test
  public void stopwatchCreationInterleaved() {
    // Setup by injection.

    Stopwatch first = stopwatchRegistry.createStopwatch("first");
    first.start();
    fakeTicker.advance(Duration.ofMillis(5));
    Stopwatch second = stopwatchRegistry.createStopwatch("second");
    second.start();
    fakeTicker.advance(Duration.ofMillis(10));
    first.stop();
    second.stop();
    ImmutableMap<String, Duration> times = stopwatchRegistry.collectStopwatchTimes();

    assertThat(times)
        .containsExactly("first", Duration.ofMillis(15), "second", Duration.ofMillis(10));
  }

  @Test
  public void snapshotUnstoppedStopwatch() {
    // Setup by injection.

    Stopwatch stopwatch = stopwatchRegistry.createStopwatch("watch");
    stopwatch.start();
    fakeTicker.advance(Duration.ofMillis(12));
    ImmutableMap<String, Duration> times = stopwatchRegistry.collectStopwatchTimes();

    assertThat(times).containsExactly("watch", Duration.ofMillis(12));
  }

  @Test
  public void createsNewStopwatchWithSameName() {
    // Setup by injection.

    Stopwatch stopwatchFirst = stopwatchRegistry.createStopwatch("watch");
    Stopwatch stopwatchSecond = stopwatchRegistry.createStopwatch("watch");

    assertThat(stopwatchFirst).isNotSameInstanceAs(stopwatchSecond);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeTicker.class).in(TestScoped.class);
      bind(Ticker.class).to(FakeTicker.class);
    }
  }
}
