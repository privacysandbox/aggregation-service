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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

/**
 * Local writer result implementation in json format. This helps standalone library to be in
 * human-readable format.
 */
public final class LocalJsonResultFileWriter implements LocalResultFileWriter {

  private AvroResultsSchemaSupplier schemaSupplier;
  private final ObjectMapper mapper = new ObjectMapper();
  private final SimpleModule module = new SimpleModule();

  @Inject
  LocalJsonResultFileWriter(AvroResultsSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
    mapper.registerModule(module);
  }

  @Override
  public void writeLocalFile(Stream<AggregatedFact> results, Path resultFilePath)
      throws FileWriteException {
    Schema schema = schemaSupplier.get();

    DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
    try {
      OutputStream outStream = Files.newOutputStream(resultFilePath, CREATE, TRUNCATE_EXISTING);
      PrintWriter printWriter = new PrintWriter(outStream);
      JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outStream, true);

      Iterator<AggregatedFact> resultsIterator = results.iterator();
      printWriter.print('[');
      printWriter.flush();
      while (resultsIterator.hasNext()) {
        AggregatedFact aggregatedFact = resultsIterator.next();
        GenericRecord aggregatedFactRecord = aggregatedFactToGenericRecord(aggregatedFact);
        writer.write(aggregatedFactRecord, jsonEncoder);
        jsonEncoder.flush();
        outStream.flush();
        if (resultsIterator.hasNext()) {
          printWriter.print(',');
          printWriter.flush();
        }
      }
      printWriter.print(']');
      printWriter.flush();
      printWriter.close();
    } catch (IOException e) {
      throw new FileWriteException("Failed to write local JSON file", e);
    }
  }

  @Override
  public String getFileExtension() {
    return ".json";
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
