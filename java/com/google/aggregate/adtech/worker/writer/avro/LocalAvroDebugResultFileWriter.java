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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.protocol.avro.AvroDebugResultsRecord;
import com.google.aggregate.protocol.avro.AvroDebugResultsWriter;
import com.google.aggregate.protocol.avro.AvroDebugResultsWriterFactory;
import com.google.aggregate.protocol.avro.AvroRecordWriter.MetadataElement;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.inject.Inject;

/** Writes a local debug results file using the Avro format. */
public final class LocalAvroDebugResultFileWriter implements LocalResultFileWriter {

  private final AvroDebugResultsWriterFactory writerFactory;

  @Inject
  LocalAvroDebugResultFileWriter(AvroDebugResultsWriterFactory writerFactory) {
    this.writerFactory = writerFactory;
  }

  @Override
  public void writeLocalFile(Stream<AggregatedFact> results, Path resultFilePath)
      throws FileWriteException {

    try (OutputStream outputAvroStream =
            Files.newOutputStream(resultFilePath, CREATE, TRUNCATE_EXISTING);
        AvroDebugResultsWriter avroDebugResultsWriter = writerFactory.create(outputAvroStream)) {
      ImmutableList<MetadataElement> metaData = ImmutableList.of();
      Stream<AvroDebugResultsRecord> resultsRecords =
          results.map(
              (fact ->
                  AvroDebugResultsRecord.create(
                      fact.getBucket(),
                      fact.getMetric(),
                      fact.getUnnoisedMetric().get(),
                      fact.getDebugAnnotations().get())));
      avroDebugResultsWriter.writeRecords(metaData, resultsRecords.collect(toImmutableList()));
    } catch (IOException e) {
      throw new FileWriteException("Failed to write local Avro debug file", e);
    }
  }

  @Override
  public String getFileExtension() {
    return ".avro";
  }
}
