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

package com.google.aggregate.adtech.worker.aggregation.engine;

import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator.FakeFactGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SingleFactAggregationTest {

  @Test
  public void aggregateSingle() {
    SingleFactAggregation aggr = new SingleFactAggregation();
    Fact fact = makeFact(/* value= */ 5);

    aggr.accept(fact);

    assertThat(aggr.getSum()).isEqualTo(5L);
  }

  @Test
  public void aggregateNone() {
    SingleFactAggregation aggr = new SingleFactAggregation();

    // Nothing sent for aggregation, testing an edge case.

    assertThat(aggr.getSum()).isEqualTo(0L);
  }

  @Test
  public void aggregateMultiple() {
    SingleFactAggregation aggr = new SingleFactAggregation();
    Fact firstFact = makeFact(/* value= */ 5);
    Fact secondFact = makeFact(/* value= */ 7);
    Fact thirdFact = makeFact(/* value= */ 13);

    aggr.accept(firstFact);
    aggr.accept(secondFact);
    aggr.accept(thirdFact);

    assertThat(aggr.getSum()).isEqualTo(25L);
  }

  private static Fact makeFact(int value) {
    // ID is hardcoded to foo. This is a single fact aggregator, it doesn't use ID for anything.
    return FakeFactGenerator.generate(/* bucket= */ 1, value);
  }
}
