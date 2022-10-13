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

import com.google.aggregate.adtech.worker.util.NumericConversions;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

/**
 * Reader that writes reports to an Avro file following the defined schema.
 *
 * <p>The schema is provided by the schema supplier. For convenience, the writer object can be
 * created through the factory, which allows the supplier to be bound with dependency injection,
 * thus requiring only the input stream to be passed.
 */
public final class AvroOutputDomainWriter extends AvroRecordWriter<AvroOutputDomainRecord> {

  /**
   * Creates a writer based on the given Avro writer and schema supplier (where Avro writer should
   * *NOT* be open, just initialized; check the Avro docs for details)
   */
  AvroOutputDomainWriter(
      DataFileWriter<GenericRecord> avroWriter,
      OutputStream outStream,
      AvroOutputDomainSchemaSupplier schemaSupplier) {
    super(avroWriter, outStream, schemaSupplier);
  }

  @Override
  public GenericRecord serializeRecordToGeneric(
      AvroOutputDomainRecord avroOutputDomainRecord, Schema avroSchema) {
    GenericRecord record = new GenericData.Record(avroSchema);
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnSignedByteArray(avroOutputDomainRecord.bucket()));
    record.put("bucket", bucketBytes);
    return record;
  }
}
