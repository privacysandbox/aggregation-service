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

package com.google.aggregate.adtech.worker.decryption;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher.PayloadDecryptionException;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipherFactory.CipherCreationException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import java.util.Optional;

/**
 * Decrypts and deserializes {@code EncryptedReports} using {@code DecryptionCipher} and {@code
 * ReportSerdes}
 */
public final class DeserializingReportDecrypter implements RecordDecrypter {

  private final DecryptionCipherFactory decryptionCipherFactory;
  private final PayloadSerdes payloadSerdes;
  private final SharedInfoSerdes sharedInfoSerdes;

  @Inject
  public DeserializingReportDecrypter(
      DecryptionCipherFactory decryptionCipherFactory,
      PayloadSerdes payloadSerdes,
      SharedInfoSerdes sharedInfoSerdes) {
    this.decryptionCipherFactory = decryptionCipherFactory;
    this.payloadSerdes = payloadSerdes;
    this.sharedInfoSerdes = sharedInfoSerdes;
  }

  @Override
  public Report decryptSingleReport(EncryptedReport encryptedReport) throws DecryptionException {
    try {
      // Deserialize the sharedInfo
      Optional<SharedInfo> sharedInfo = sharedInfoSerdes.convert(encryptedReport.sharedInfo());
      if (sharedInfo.isEmpty()) {
        throw new DecryptionException(
            new IllegalArgumentException(
                "Couldn't deserialize shared_info. shared_info was: "
                    + encryptedReport.sharedInfo()));
      }

      // Decrypt the payload to plaintext bytes
      DecryptionCipher decryptionCipher =
          decryptionCipherFactory.decryptionCipherFor(encryptedReport);
      ByteSource decryptedPayload =
          decryptionCipher.decrypt(
              encryptedReport.payload(), encryptedReport.sharedInfo(), sharedInfo.get().version());

      // Deserialize the payload
      Optional<Payload> plaintextPayload = payloadSerdes.convert(decryptedPayload);
      if (plaintextPayload.isEmpty()) {
        throw new PayloadDecryptionException(
            new IllegalArgumentException("Decrypted payload could not be deserialized"));
      }

      return Report.builder()
          .setPayload(plaintextPayload.get())
          .setSharedInfo(sharedInfo.get())
          .build();

    } catch (PayloadDecryptionException | CipherCreationException e) {
      throw new DecryptionException(e);
    }
  }
}
