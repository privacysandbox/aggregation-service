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
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericRecord;

/**
 * Reader that provides {@code Record} from an Avro file following the defined schema.
 *
 * <p>The schema is provided by the schema supplier. For convenience, the reader object can be
 * created through the factory, which allows the supplier to be bound ith dependency injection, thus
 * requiring only the input stream to be passed.
 */
public abstract class AvroRecordReader<Record> implements AutoCloseable {

  private final DataFileStream<GenericRecord> streamReader;

  AvroRecordReader(DataFileStream<GenericRecord> streamReader) {
    this.streamReader = streamReader;
  }

  /**
   * Generate a stream of records from the file.
   *
   * <p>WARNING: An {@link AvroRuntimeException} can be thrown when terminal operations happen on
   * the stream later. This can happen if the Avro file has the wrong schema if the Avro file was
   * written improperly. Callers need to catch this unchecked exception later if special handling is
   * needed for malformed avro files.
   */
  public Stream<Record> streamRecords() {
    return Stream.generate(this::readRecordForStreaming)
        .takeWhile(Optional::isPresent)
        .map(Optional::get);
  }

  /** Reads metadata string specified by the key (returns empty optional if not available) */
  public Optional<String> getMeta(String key) {
    return Optional.ofNullable(streamReader.getMetaString(key));
  }

  private Optional<Record> readRecordForStreaming() {
    if (streamReader.hasNext()) {
      return Optional.of(deserializeRecordFromGeneric(streamReader.next()));
    }
    return Optional.empty();
  }

  @Override
  public void close() throws IOException {
    streamReader.close();
  }

  /** Deserializes avro {@code GenericRecord} to generic Java type {@code Record}. */
  abstract Record deserializeRecordFromGeneric(GenericRecord record);
}
