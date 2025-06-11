/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.shared.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Fake for clock that allows time and zone to be set to a fixed time. */
public final class FakeClock extends Clock {

  private Clock fixed;
  private ZoneId zoneId = ZoneId.systemDefault();

  /** Creates a new instance of the {@code FakeCLock} class. */
  public FakeClock() {
    setTime(Instant.parse("2021-11-01T18:00:00.00Z"));
  }

  /** Set the time of the clock in the current time-zone. */
  public void setTime(Instant time) {
    fixed = Clock.fixed(time, zoneId);
  }

  /** Set the time and time-zone of the clock. */
  public void setTime(Instant time, ZoneId zoneId) {
    this.zoneId = zoneId;
    fixed = Clock.fixed(time, zoneId);
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return fixed.withZone(zone);
  }

  @Override
  public Instant instant() {
    return fixed.instant();
  }
}
