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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

/**
 * Local writer result implementation in json format. This helps standalone library to be in
 * human-readable format.
 */
public abstract class AbstractLocalJsonResultFileWriter implements LocalResultFileWriter {

  private final Converter<ImmutableList<AggregatedFact>, byte[]> avroResultsSerdes;

  protected AbstractLocalJsonResultFileWriter(
      Converter<ImmutableList<AggregatedFact>, byte[]> resultsSerdes) {
    this.avroResultsSerdes = resultsSerdes;
  }

  @Override
  public void writeLocalFile(Stream<AggregatedFact> results, Path resultFilePath)
      throws FileWriteException {
    Schema schema = getSchema();

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
  public void writeLocalFile(byte[] avroFileBytes, Path resultFilePath) throws FileWriteException {
    ImmutableList<AggregatedFact> aggregatedFacts =
        avroResultsSerdes.reverse().convert(avroFileBytes);
    this.writeLocalFile(aggregatedFacts.stream(), resultFilePath);
  }

  @Override
  public String getFileExtension() {
    return ".json";
  }

  abstract GenericRecord aggregatedFactToGenericRecord(AggregatedFact aggregatedFact);

  abstract Schema getSchema();
}
