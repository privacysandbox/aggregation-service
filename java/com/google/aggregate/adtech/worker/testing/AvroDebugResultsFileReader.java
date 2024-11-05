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

package com.google.aggregate.adtech.worker.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroDebugResultsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData.EnumSymbol;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

/** Simple utility to read an Avro debug results file used for testing. */
public final class AvroDebugResultsFileReader {
  private final AvroDebugResultsSchemaSupplier avroDebugResultsSchemaSupplier;

  @Inject
  AvroDebugResultsFileReader(AvroDebugResultsSchemaSupplier debugSchemaSupplier) {
    this.avroDebugResultsSchemaSupplier = debugSchemaSupplier;
  }

  public ImmutableList<AggregatedFact> readAvroResultsFile(Path path) throws IOException {
    DatumReader<GenericRecord> datumReader =
        new GenericDatumReader<>(avroDebugResultsSchemaSupplier.get());
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
      GenericRecord record = streamReader.next();

      byte[] bucketBytes = ((ByteBuffer) record.get("bucket")).array();
      BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);

      long unnoisedMetric = ((long) record.get("unnoised_metric"));
      long noise = ((long) record.get("noise"));
      List<EnumSymbol> annotations = (List<EnumSymbol>) record.get("annotations");

      List<DebugBucketAnnotation> annotationList =
          annotations.stream()
              .map(
                  annotation ->
                      DebugBucketAnnotation.valueOf(annotation.toString().toUpperCase(Locale.ROOT)))
              .collect(toImmutableList());

      return Optional.of(
          AggregatedFact.create(bucket, unnoisedMetric + noise, unnoisedMetric, annotationList));
    }

    return Optional.empty();
  }
}
