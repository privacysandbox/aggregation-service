/*
 * Copyright 2025 Google LLC
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
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.protocol.avro.AvroReportWriter;
import com.google.aggregate.protocol.avro.AvroReportWriterFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

/** Writes encrypted reports in avro format. It is to be used for testing purposes. */
public final class FakeReportWriter {

  private final SharedInfoSerdes sharedInfoSerdes;
  private final PayloadSerdes payloadSerdes;
  private final AvroReportWriterFactory reportWriterFactory;
  private final AvroRecordEncryptedReportConverter avroConverter;
  private final FakeDecryptionKeyService decryptionKeyService;

  @Inject
  FakeReportWriter(
      SharedInfoSerdes sharedInfoSerdes,
      PayloadSerdes payloadSerdes,
      FakeDecryptionKeyService decryptionKeyService,
      AvroReportWriterFactory reportWriterFactory,
      AvroRecordEncryptedReportConverter avroConverter) {
    this.sharedInfoSerdes = sharedInfoSerdes;
    this.payloadSerdes = payloadSerdes;
    this.decryptionKeyService = decryptionKeyService;
    this.reportWriterFactory = reportWriterFactory;
    this.avroConverter = avroConverter;
  }

  /**
   * Serializes, encrypts and writes the reports in avro format. If there is already an existing
   * file at the given path, it will be overwritten.
   */
  public void writeReports(Path reportsPath, ImmutableList<Report> reports) {
    try {
      writeEncryptedReports(reportsPath, encryptReports(reports));
    } catch (IOException | KeyFetchException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes encrypted reports avro in the given path. If there is already an existing file at the *
   * given path, it will be overwritten.
   */
  public void writeEncryptedReports(
      Path reportsPath, ImmutableList<EncryptedReport> encryptedReports) throws IOException {
    try (OutputStream avroStream = Files.newOutputStream(reportsPath, CREATE, TRUNCATE_EXISTING);
        AvroReportWriter reportWriter = reportWriterFactory.create(avroStream)) {
      reportWriter.writeRecords(
          /* metadata= */ ImmutableList.of(),
          encryptedReports.stream().map(avroConverter.reverse()).collect(toImmutableList()));
    }
  }

  private ImmutableList<EncryptedReport> encryptReports(ImmutableList<Report> reports)
      throws KeyFetchException, GeneralSecurityException, IOException {
    ImmutableList.Builder<EncryptedReport> encryptedReportBuilder = ImmutableList.builder();
    for (Report report : reports) {
      encryptedReportBuilder.add(encrypt(report));
    }
    return encryptedReportBuilder.build();
  }

  private EncryptedReport encrypt(Report unencryptedreport)
      throws KeyFetchException, GeneralSecurityException, IOException {
    String sharedInfoString =
        sharedInfoSerdes.reverse().convert(Optional.of(unencryptedreport.sharedInfo()));
    String keyId = UUID.randomUUID().toString();
    ByteSource firstReportBytes =
        decryptionKeyService.generateCiphertext(
            keyId,
            payloadSerdes.reverse().convert(Optional.of(unencryptedreport.payload())),
            sharedInfoString);
    return EncryptedReport.builder()
        .setPayload(firstReportBytes)
        .setKeyId(keyId)
        .setSharedInfo(sharedInfoString)
        .build();
  }
}
