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
import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.aggregate.protocol.avro.AvroDebugResultsSchemaSupplier;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.EnumSymbol;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;

/***
 * Converts debug {@link AggregatedFact} to/from AVRO encoded summary report file bytes.
 */
public class AvroDebugResultsSerdes extends Converter<ImmutableList<AggregatedFact>, byte[]> {
  private final AvroDebugResultsSchemaSupplier debugSchemaSupplier;

  @Inject
  AvroDebugResultsSerdes(AvroDebugResultsSchemaSupplier debugSchemaSupplier) {
    this.debugSchemaSupplier = debugSchemaSupplier;
  }

  /** Convert the stream of Debug AggregatedFacts to a byte[] of an AVRO file. */
  @Override
  protected byte[] doForward(ImmutableList<AggregatedFact> aggregatedFacts) {
    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(debugSchemaSupplier.get());
    ByteArrayOutputStream factsByteOutputStream = new ByteArrayOutputStream();

    try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
      dataFileWriter.create(debugSchemaSupplier.get(), factsByteOutputStream);

      for (AggregatedFact aggregatedFact : aggregatedFacts) {
        dataFileWriter.append(debugFactToGenericRecord(aggregatedFact));
      }
    } catch (IOException e) {
      // TODO: Handle exception from serdes to map to a meaningful return code.
      throw new RuntimeException(e);
    }

    return factsByteOutputStream.toByteArray();
  }

  /** Convert a debug AVRO file byte[] to AggregatedFacts. */
  @Override
  protected ImmutableList<AggregatedFact> doBackward(byte[] avroBytes) {
    ByteArrayInputStream avroInputStream = new ByteArrayInputStream(avroBytes);
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(debugSchemaSupplier.get());

    try {
      DataFileStream<GenericRecord> streamReader =
          new DataFileStream<>(avroInputStream, datumReader);

      return Stream.generate(() -> genericRecordToDebugFact(streamReader))
          .takeWhile(Optional::isPresent)
          .map(Optional::get)
          .collect(toImmutableList());
    } catch (IOException e) {
      // TODO: Handle exception from serdes to map to a meaningful return code.
      throw new RuntimeException(e);
    }
  }

  private GenericRecord debugFactToGenericRecord(AggregatedFact aggregatedFact) {
    GenericRecord record = new GenericData.Record(debugSchemaSupplier.get());
    ByteBuffer bucketBytes =
        ByteBuffer.wrap(NumericConversions.toUnsignedByteArray(aggregatedFact.getBucket()));

    ImmutableList<EnumSymbol> enumList =
        ((List<DebugBucketAnnotation>) aggregatedFact.getDebugAnnotations().orElse(List.of()))
            .stream()
                .map(
                    annotation ->
                        new GenericData.EnumSymbol(
                            debugSchemaSupplier.get(), annotation.toString()))
                .collect(toImmutableList());

    record.put("bucket", bucketBytes);
    record.put("unnoised_metric", aggregatedFact.getUnnoisedMetric().orElse(0L));
    record.put("noise", aggregatedFact.getMetric() - aggregatedFact.getUnnoisedMetric().orElse(0L));
    record.put("annotations", enumList);

    return record;
  }

  private static Optional<AggregatedFact> genericRecordToDebugFact(
      DataFileStream<GenericRecord> streamReader) {
    if (streamReader.hasNext()) {
      GenericRecord genericRecord = streamReader.next();

      byte[] bucketBytes = ((ByteBuffer) genericRecord.get("bucket")).array();
      BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);

      long unnoisedMetric = ((long) genericRecord.get("unnoised_metric"));
      long noise = ((long) genericRecord.get("noise"));

      List<EnumSymbol> annotations = (List<EnumSymbol>) genericRecord.get("annotations");

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
