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

package com.google.aggregate.adtech.worker.writer.avro;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

/** Writes a local results file using the Avro format. */
public final class LocalAvroResultFileWriter implements LocalResultFileWriter {

  private AvroResultsSchemaSupplier schemaSupplier;

  @Inject
  LocalAvroResultFileWriter(AvroResultsSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
  }

  /**
   * Write the results to an Avro file at the {@code Path} given.
   *
   * <p>The {@code Path} does not need to point to an existing file, this method will create a new
   * file. If a file already exists at the {@code Path} given then it will be overwritten. This is
   * the behavior of the {@code Files.newOutputStream} method that this relies on.
   *
   * <p>If exceptions occur mid-way during writing this function will leave a partially written
   * file.
   */
  @Override
  public void writeLocalFile(Stream<AggregatedFact> results, Path resultFilePath)
      throws FileWriteException {
    Schema schema = schemaSupplier.get();

    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
    DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
    try {
      dataFileWriter.create(
          schema, Files.newOutputStream(resultFilePath, CREATE, TRUNCATE_EXISTING));

      // Write all results to an Avro file. .append() call can throw IOExceptions so using an
      // Iterator is cleaner for exception handling.
      Iterator<AggregatedFact> resultsIterator = results.iterator();
      while (resultsIterator.hasNext()) {
        AggregatedFact aggregatedFact = resultsIterator.next();
        GenericRecord aggregatedFactRecord = aggregatedFactToGenericRecord(aggregatedFact);
        dataFileWriter.append(aggregatedFactRecord);
      }

      dataFileWriter.close();
    } catch (IOException e) {
      throw new FileWriteException("Failed to write local Avro file", e);
    }
  }

  @Override
  public String getFileExtension() {
    return ".avro";
  }

  private GenericRecord aggregatedFactToGenericRecord(AggregatedFact aggregatedFact) {
    GenericRecord genericRecord = new GenericData.Record(schemaSupplier.get());
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnsignedByteArray(aggregatedFact.getBucket()));
    genericRecord.put("bucket", bucketBytes);
    genericRecord.put("metric", aggregatedFact.getMetric());
    return genericRecord;
  }
}
