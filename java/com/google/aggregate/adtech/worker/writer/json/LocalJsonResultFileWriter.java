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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * Local writer result implementation in json format. This helps standalone library to be in
 * human-readable format.
 */
public final class LocalJsonResultFileWriter implements LocalResultFileWriter {

  private final ObjectMapper mapper = new ObjectMapper();
  private final SimpleModule module = new SimpleModule();

  @Inject
  LocalJsonResultFileWriter() {
    module.addSerializer(EncryptedReport.class, new EncryptedReportSerializer());
    module.addSerializer(AggregatedFact.class, new AggregatedFactSerializer());
    mapper.registerModule(module);
  }

  @Override
  public void writeLocalFile(Stream<AggregatedFact> results, Path resultFile)
      throws FileWriteException {

    try {
      List<AggregatedFact> aggregatedFactList = results.collect(Collectors.toList());
      String prettyJson =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(aggregatedFactList);
      Files.writeString(resultFile, prettyJson, StandardOpenOption.CREATE);
    } catch (Exception e) {
      throw new FileWriteException("Failed to write local Json file", e);
    }
  }

  @Override
  public void writeLocalReportFile(Stream<EncryptedReport> reports, Path resultFilePath)
      throws FileWriteException {
    try {
      List<EncryptedReport> encryptedReportsList = reports.collect(Collectors.toList());
      String prettyJson =
          mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encryptedReportsList);
      Files.writeString(resultFilePath, prettyJson, StandardOpenOption.CREATE);
    } catch (Exception e) {
      throw new FileWriteException("Failed to write reports to local Json file", e);
    }
  }

  @Override
  public String getFileExtension() {
    return ".json";
  }

  private static class AggregatedFactSerializer extends StdSerializer<AggregatedFact> {

    AggregatedFactSerializer() {
      this(null);
    }

    AggregatedFactSerializer(Class<AggregatedFact> t) {
      super(t);
    }

    @Override
    public void serialize(
        AggregatedFact aggregatedFact, JsonGenerator jgen, SerializerProvider serializerProvider)
        throws IOException {
      jgen.writeStartObject();
      jgen.writeBinaryField("bucket", aggregatedFact.bucket().toByteArray());
      jgen.writeNumberField("metric", aggregatedFact.metric());
      jgen.writeEndObject();
    }
  }

  private static class EncryptedReportSerializer extends StdSerializer<EncryptedReport> {

    EncryptedReportSerializer() {
      super(EncryptedReport.class);
    }

    EncryptedReportSerializer(Class<EncryptedReport> t) {
      super(t);
    }

    @Override
    public void serialize(
        EncryptedReport encryptedReport, JsonGenerator jgen, SerializerProvider serializerProvider)
        throws IOException {
      jgen.writeStartObject();
      jgen.writeStringField("key_id", encryptedReport.keyId());
      jgen.writeBinaryField("payload", encryptedReport.payload().read());
      jgen.writeStringField("shared_info", encryptedReport.sharedInfo());
      jgen.writeEndObject();
    }
  }
}
