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

package com.google.aggregate.protocol.avro;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.common.collect.ImmutableList;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.EnumSymbol;
import org.apache.avro.generic.GenericRecord;

/** Writer that writes results to an Avro file following the defined schema. */
public final class AvroDebugResultsWriter extends AvroRecordWriter<AvroDebugResultsRecord> {

  /** Creates a writer based on the given Avro writer and schema supplier. */
  AvroDebugResultsWriter(
      DataFileWriter<GenericRecord> avroWriter,
      OutputStream outStream,
      AvroDebugResultsSchemaSupplier schemaSupplier) {
    super(avroWriter, outStream, schemaSupplier);
  }

  @Override
  public GenericRecord serializeRecordToGeneric(
      AvroDebugResultsRecord avroDebugResultRecord, Schema avroSchema) {
    GenericRecord record = new GenericData.Record(avroSchema);
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnSignedByteArray(avroDebugResultRecord.bucket()));

    ImmutableList<EnumSymbol> enumList =
        avroDebugResultRecord.debugAnnotations().stream()
            .map(annotation -> new GenericData.EnumSymbol(avroSchema, annotation.toString()))
            .collect(toImmutableList());

    record.put("bucket", bucketBytes);
    record.put("unnoised_metric", avroDebugResultRecord.unnoisedMetric());
    record.put("noise", avroDebugResultRecord.metric() - avroDebugResultRecord.unnoisedMetric());
    record.put("annotations", enumList.asList());

    return record;
  }
}
