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

package com.google.aggregate.adtech.worker.model.serdes;

import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroDebugResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroDebugResultsSerdesTest {
  @Rule public final Acai acai = new Acai(AvroDebugResultsSerdesTest.TestEnv.class);

  @Inject AvroDebugResultsSerdes debugResultsConverter;

  @Test
  public void convertDebugFactsToBinaryAndBack() {
    byte[] encodedFacts =
        debugResultsConverter.convert(
            ImmutableList.of(
                AggregatedFact.create(
                    NumericConversions.createBucketFromInt(1),
                    10,
                    5L,
                    List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN)),
                AggregatedFact.create(
                    NumericConversions.createBucketFromInt(2),
                    20,
                    15L,
                    List.of(DebugBucketAnnotation.IN_REPORTS))));

    ImmutableList<AggregatedFact> deserializedFacts =
        debugResultsConverter.reverse().convert(encodedFacts);

    assertThat(deserializedFacts)
        .containsExactly(
            AggregatedFact.create(
                NumericConversions.createBucketFromInt(1),
                10,
                5L,
                List.of(DebugBucketAnnotation.IN_REPORTS, DebugBucketAnnotation.IN_DOMAIN)),
            AggregatedFact.create(
                NumericConversions.createBucketFromInt(2),
                20,
                15L,
                List.of(DebugBucketAnnotation.IN_REPORTS)));
  }

  private static final class TestEnv extends AbstractModule {
    @Override
    protected void configure() {
      AvroDebugResultsSchemaSupplier debugResultsSchemaSupplier =
          new AvroDebugResultsSchemaSupplier();
      bind(AvroDebugResultsSerdes.class)
          .toInstance(new AvroDebugResultsSerdes(debugResultsSchemaSupplier));
    }
  }
}
