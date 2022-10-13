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

import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;

/** Produces AvroDebugResultsReader for given input streams */
public final class AvroDebugResultsReaderFactory {

  /** Returns an AvroDebugResultsReader based on the injected AvroDebugResultsSchemaSupplier. */
  private final AvroDebugResultsSchemaSupplier schemaSupplier;

  @Inject
  public AvroDebugResultsReaderFactory(AvroDebugResultsSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
  }

  public AvroDebugResultsReader create(InputStream in) throws IOException {
    return new AvroDebugResultsReader(
        new DataFileStream<>(in, new GenericDatumReader<>(schemaSupplier.get())));
  }
}
