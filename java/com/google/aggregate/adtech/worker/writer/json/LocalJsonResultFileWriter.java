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

package com.google.aggregate.adtech.worker.writer.json;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.serdes.AvroResultsSerdes;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

/**
 * Local writer result implementation in json format. This helps standalone library to be in
 * human-readable format.
 */
public final class LocalJsonResultFileWriter extends AbstractLocalJsonResultFileWriter {

  private final AvroResultsSchemaSupplier schemaSupplier;

  @Inject
  LocalJsonResultFileWriter(
      AvroResultsSchemaSupplier schemaSupplier, AvroResultsSerdes resultsSerdes) {
    super(resultsSerdes);
    this.schemaSupplier = schemaSupplier;
  }

  @Override
  GenericRecord aggregatedFactToGenericRecord(AggregatedFact aggregatedFact) {
    GenericRecord genericRecord = new GenericData.Record(getSchema());
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnsignedByteArray(aggregatedFact.getBucket()));
    genericRecord.put("bucket", bucketBytes);
    genericRecord.put("metric", aggregatedFact.getMetric());
    return genericRecord;
  }

  @Override
  Schema getSchema() {
    return schemaSupplier.get();
  }
}
