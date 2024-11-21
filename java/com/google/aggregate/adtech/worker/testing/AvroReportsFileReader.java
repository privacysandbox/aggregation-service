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

import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.protocol.avro.AvroReportsSchemaSupplier;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import java.io.IOException;
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

/** Simple utility to read an Avro reports file, used for testing. */
public final class AvroReportsFileReader {

  private final AvroReportsSchemaSupplier avroReportsSchemaSupplier;

  @Inject
  AvroReportsFileReader(AvroReportsSchemaSupplier avroReportsSchemaSupplier) {
    this.avroReportsSchemaSupplier = avroReportsSchemaSupplier;
  }

  /** Reads the Avro results file at the path given to a list. */
  public ImmutableList<EncryptedReport> readAvroReportsFile(Path path) throws IOException {
    DatumReader<GenericRecord> datumReader =
        new GenericDatumReader<>(avroReportsSchemaSupplier.get());
    DataFileStream<GenericRecord> streamReader =
        new DataFileStream<>(Files.newInputStream(path), datumReader);

    return Stream.generate(() -> readRecordToEncryptedReport(streamReader))
        .takeWhile(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  private static Optional<EncryptedReport> readRecordToEncryptedReport(
      DataFileStream<GenericRecord> streamReader) {
    if (streamReader.hasNext()) {
      GenericRecord genericRecord = streamReader.next();
      ByteSource payload = ByteSource.wrap(((ByteBuffer) genericRecord.get("payload")).array());
      String keyId = genericRecord.get("key_id").toString();
      String sharedInfo = genericRecord.get("shared_info").toString();
      return Optional.of(
          EncryptedReport.builder()
              .setPayload(payload)
              .setKeyId(keyId)
              .setSharedInfo(sharedInfo)
              .build());
    }

    return Optional.empty();
  }
}
