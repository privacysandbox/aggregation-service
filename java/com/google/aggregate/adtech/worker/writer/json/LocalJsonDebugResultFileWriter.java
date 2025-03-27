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

package com.google.aggregate.adtech.worker.writer.json;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.model.serdes.AvroDebugResultsSerdes;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroDebugResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.List;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;

/**
 * Local writer debug result implementation in json format. This helps standalone library to be in
 * human-readable format.
 */
public final class LocalJsonDebugResultFileWriter extends AbstractLocalJsonResultFileWriter {

  private final AvroDebugResultsSchemaSupplier schemaSupplier;

  @Inject
  LocalJsonDebugResultFileWriter(
      AvroDebugResultsSchemaSupplier schemaSupplier, AvroDebugResultsSerdes debugResultsSerdes) {
    super(debugResultsSerdes);
    this.schemaSupplier = schemaSupplier;
  }

  @Override
  GenericRecord aggregatedFactToGenericRecord(AggregatedFact aggregatedFact) {
    Schema schema = getSchema();
    GenericRecord genericRecord = new GenericData.Record(schema);

    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnsignedByteArray(aggregatedFact.getBucket()));
    genericRecord.put("bucket", bucketBytes);

    long unnoisedMetric = aggregatedFact.getUnnoisedMetric().orElse(aggregatedFact.getMetric());
    genericRecord.put("unnoised_metric", unnoisedMetric);
    genericRecord.put("noise", aggregatedFact.getMetric() - unnoisedMetric);

    List<DebugBucketAnnotation> annotations =
        aggregatedFact.getDebugAnnotations().orElse(ImmutableList.of());
    ImmutableList<GenericEnumSymbol> enumList =
        annotations.stream()
            .map(annotation -> new GenericData.EnumSymbol(schema, annotation.toString()))
            .collect(toImmutableList());
    genericRecord.put("annotations", enumList);

    return genericRecord;
  }

  @Override
  Schema getSchema() {
    return schemaSupplier.get();
  }
}
