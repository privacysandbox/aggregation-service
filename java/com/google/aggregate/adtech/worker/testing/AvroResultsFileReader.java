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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

/** Simple utility to read an Avro results file, used for testing. */
public final class AvroResultsFileReader {

  private final AvroResultsSchemaSupplier avroResultsSchemaSupplier;

  @Inject
  AvroResultsFileReader(AvroResultsSchemaSupplier avroResultsSchemaSupplier) {
    this.avroResultsSchemaSupplier = avroResultsSchemaSupplier;
  }

  /** Reads the Avro results file at the path given to a list. */
  public ImmutableList<AggregatedFact> readAvroResultsFile(Path path) throws IOException {
    DatumReader<GenericRecord> datumReader =
        new GenericDatumReader<>(avroResultsSchemaSupplier.get());
    DataFileStream<GenericRecord> streamReader =
        new DataFileStream<>(Files.newInputStream(path), datumReader);

    return Stream.generate(() -> readRecordToAggregatedFact(streamReader))
        .takeWhile(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  private static Optional<AggregatedFact> readRecordToAggregatedFact(
      DataFileStream<GenericRecord> streamReader) {
    if (streamReader.hasNext()) {
      GenericRecord genericRecord = streamReader.next();
      // Decode BigInteger from bytes
      byte[] bucketBytes = ((ByteBuffer) genericRecord.get("bucket")).array();
      BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);
      Long metric = (Long) genericRecord.get("metric");
      return Optional.of(AggregatedFact.create(bucket, metric));
    }

    return Optional.empty();
  }
}
