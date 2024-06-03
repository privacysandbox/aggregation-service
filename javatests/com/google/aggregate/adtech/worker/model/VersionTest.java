/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VersionTest {

  @Test
  public void getBetweenVersionPredicate() {
    Version lowerVersion = Version.create(/* major= */ 0, /* minor= */ 1);
    Version higherVersion = Version.create(/* major= */ 25, /* minor= */ 1);
    Predicate<Version> betweenPredicate =
        Version.getBetweenVersionPredicate(lowerVersion, higherVersion);

    assertThat(betweenPredicate.test(lowerVersion)).isTrue();
    assertThat(betweenPredicate.test(Version.create(/* major= */ 25, /* minor= */ 0))).isTrue();
    assertThat(betweenPredicate.test(Version.create(/* major= */ 6, /* minor= */ 99999))).isTrue();
    assertThat(betweenPredicate.test(higherVersion)).isFalse();
  }

  @Test
  public void getBetweenVersionPredicate_withIllegalRange_throwsIllegalArgument() {
    Version lowerVersion = Version.create(/* major= */ 0, /* minor= */ 1);
    Version higherVersion = Version.create(/* major= */ 25, /* minor= */ 1);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            Version.getBetweenVersionPredicate(
                /* higherExclusiveVersion= */ higherVersion,
                /* lowerInclusiveVersion= */ lowerVersion));
  }

  @Test
  public void getGreaterThanOrEqualToVersionPredicate() {
    Version compareToVersion = Version.create(/* major= */ 75, /* minor= */ 30);
    Predicate<Version> gePredicate =
        Version.getGreaterThanOrEqualToVersionPredicate(compareToVersion);

    assertThat(gePredicate.test(compareToVersion)).isTrue();
    assertThat(gePredicate.test(Version.create(/* major= */ 75, /* minor= */ 29))).isFalse();
    assertThat(gePredicate.test(Version.create(/* major= */ 6, /* minor= */ 99999))).isFalse();
    assertThat(gePredicate.test(Version.create(/* major= */ 200, /* minor= */ 0))).isTrue();
  }
}
