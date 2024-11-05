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
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroResultsSerdesTest {
  @Rule public final Acai acai = new Acai(AvroResultsSerdesTest.TestEnv.class);

  @Inject AvroResultsSerdes avroResultsSerdes;

  @Test
  public void convertFactsToBinaryAndBack() {
    ImmutableList<AggregatedFact> testFacts =
        ImmutableList.of(
            AggregatedFact.create(NumericConversions.createBucketFromInt(1), 10),
            AggregatedFact.create(NumericConversions.createBucketFromInt(2), 20));

    byte[] encodedFacts = avroResultsSerdes.convert(testFacts);

    ImmutableList<AggregatedFact> deserializedFacts =
        avroResultsSerdes.reverse().convert(encodedFacts);

    assertThat(deserializedFacts)
        .containsExactly(
            AggregatedFact.create(NumericConversions.createBucketFromInt(1), 10),
            AggregatedFact.create(NumericConversions.createBucketFromInt(2), 20));
  }

  private static final class TestEnv extends AbstractModule {
    @Override
    protected void configure() {
      AvroResultsSchemaSupplier resultsSchemaSupplier = new AvroResultsSchemaSupplier();
      bind(AvroResultsSerdes.class).toInstance(new AvroResultsSerdes(resultsSchemaSupplier));
    }
  }
}
