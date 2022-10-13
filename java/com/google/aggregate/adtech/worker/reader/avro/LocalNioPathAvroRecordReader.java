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

package com.google.aggregate.adtech.worker.reader.avro;

import com.google.aggregate.adtech.worker.RecordReader;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.protocol.avro.AvroReportsReader;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.avro.AvroRuntimeException;

/** Reads records using avro. */
public class LocalNioPathAvroRecordReader implements RecordReader {

  private final AvroReportsReader avroReportsReader;
  private static final AvroRecordEncryptedReportConverter AVRO_RECORD_ENCRYPTED_REPORT_CONVERTER =
      new AvroRecordEncryptedReportConverter();

  LocalNioPathAvroRecordReader(AvroReportsReader avroReportsReader) {
    this.avroReportsReader = avroReportsReader;
  }

  @Override
  public Stream<EncryptedReport> readEncryptedReports(DataLocation dataLocation)
      throws RecordReadException {
    try {
      return avroReportsReader.streamRecords().map(AVRO_RECORD_ENCRYPTED_REPORT_CONVERTER::convert);
    } catch (AvroRuntimeException e) {
      throw new RecordReadException(e);
    }
  }

  @Override
  public void close() throws RecordReadException {
    try {
      avroReportsReader.close();
    } catch (IOException e) {
      throw new RecordReadException(e);
    }
  }
}
