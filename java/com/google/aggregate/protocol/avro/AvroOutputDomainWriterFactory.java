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
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;

/** Produces {@code AvroOutputDomainWriter}s */
public final class AvroOutputDomainWriterFactory {

  private final AvroOutputDomainSchemaSupplier schemaSupplier;

  @Inject
  public AvroOutputDomainWriterFactory(AvroOutputDomainSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
  }

  public AvroOutputDomainWriter create(OutputStream outStream) {
    DataFileWriter<GenericRecord> avroWriter =
        new DataFileWriter<>(new GenericDatumWriter<>(schemaSupplier.get()));
    return new AvroOutputDomainWriter(avroWriter, outStream, schemaSupplier);
  }
}
