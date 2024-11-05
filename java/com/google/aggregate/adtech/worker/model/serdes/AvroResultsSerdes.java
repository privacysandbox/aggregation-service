/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.model.serdes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;

/***
 * Converts {@link AggregatedFact} to/from AVRO encoded summary report file bytes.
 */
public class AvroResultsSerdes extends Converter<ImmutableList<AggregatedFact>, byte[]> {
  private final AvroResultsSchemaSupplier schemaSupplier;

  @Inject
  AvroResultsSerdes(AvroResultsSchemaSupplier schemaSupplier) {
    this.schemaSupplier = schemaSupplier;
  }

  /** Convert the stream of AggregatedFacts to a byte[] of an AVRO file. */
  @Override
  protected byte[] doForward(ImmutableList<AggregatedFact> aggregatedFacts) {
    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schemaSupplier.get());
    ByteArrayOutputStream factsByteOutputStream = new ByteArrayOutputStream();

    try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
      dataFileWriter.create(schemaSupplier.get(), factsByteOutputStream);

      for (AggregatedFact aggregatedFact : aggregatedFacts) {
        dataFileWriter.append(factToGenericRecord(aggregatedFact));
      }
    } catch (IOException e) {
      // TODO: Handle exception from serdes to map to a meaningful return code.
      throw new RuntimeException(e);
    }

    return factsByteOutputStream.toByteArray();
  }

  /** Convert an AVRO file byte[] to AggregatedFacts. */
  @Override
  protected ImmutableList<AggregatedFact> doBackward(byte[] avroBytes) {
    ByteArrayInputStream avroInputStream = new ByteArrayInputStream(avroBytes);
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schemaSupplier.get());

    try {
      DataFileStream<GenericRecord> streamReader =
          new DataFileStream<>(avroInputStream, datumReader);

      return Stream.generate(() -> genericRecordToFact(streamReader))
          .takeWhile(Optional::isPresent)
          .map(Optional::get)
          .collect(toImmutableList());

    } catch (IOException e) {
      // TODO: Handle exception from serdes to map to a meaningful return code.
      throw new RuntimeException(e);
    }
  }

  private GenericRecord factToGenericRecord(AggregatedFact aggregatedFact) {
    GenericRecord genericRecord = new GenericData.Record(schemaSupplier.get());
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnsignedByteArray(aggregatedFact.getBucket()));
    genericRecord.put("bucket", bucketBytes);
    genericRecord.put("metric", aggregatedFact.getMetric());
    return genericRecord;
  }

  private Optional<AggregatedFact> genericRecordToFact(DataFileStream<GenericRecord> streamReader) {
    if (streamReader.hasNext()) {
      GenericRecord genericRecord = streamReader.next();

      byte[] bucketBytes = ((ByteBuffer) genericRecord.get("bucket")).array();
      BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);
      long metric = (long) genericRecord.get("metric");

      return Optional.of(AggregatedFact.create(bucket, metric));
    }

    return Optional.empty();
  }
}
