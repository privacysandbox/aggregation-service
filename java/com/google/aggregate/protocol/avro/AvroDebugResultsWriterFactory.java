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

import java.io.OutputStream;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;

/** Produces {@code AvroDebugResultsWriter}s */
public final class AvroDebugResultsWriterFactory {

  /** Returns an AvroDebugResultsWriter based on the injected AvroDebugResultsSchemaSupplier. */
  private final AvroDebugResultsSchemaSupplier schemaSupplier;

  @Inject
  public AvroDebugResultsWriterFactory(AvroDebugResultsSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
  }

  public Schema getSchema() {
    return schemaSupplier.get();
  }

  public AvroDebugResultsWriter create(OutputStream outStream) {
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schemaSupplier.get()));
    return new AvroDebugResultsWriter(avroWriter, outStream, schemaSupplier);
  }
}
