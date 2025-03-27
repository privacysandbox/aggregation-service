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

package com.google.aggregate.adtech.worker.encryption;

import com.google.aggregate.adtech.worker.encryption.EncryptionCipher.PayloadEncryptionException;
import com.google.aggregate.adtech.worker.encryption.EncryptionCipherFactory.CipherCreationException;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKey;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService.KeyFetchException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Optional;

/** {@link ReportEncrypter} implementation. */
public final class ReportEncrypterImpl implements ReportEncrypter {

  private final EncryptionCipherFactory encryptionCipherFactory;
  private final EncryptionKeyService encryptionKeyService;
  private final PayloadSerdes payloadSerdes;
  private final SharedInfoSerdes sharedInfoSerdes;

  @Inject
  public ReportEncrypterImpl(
      EncryptionCipherFactory encryptionCipherFactory,
      EncryptionKeyService encryptionKeyService,
      PayloadSerdes payloadSerdes,
      SharedInfoSerdes sharedInfoSerdes) {
    this.encryptionCipherFactory = encryptionCipherFactory;
    this.encryptionKeyService = encryptionKeyService;
    this.payloadSerdes = payloadSerdes;
    this.sharedInfoSerdes = sharedInfoSerdes;
  }

  @Override
  public EncryptedReport encryptSingleReport(Report report, String reportVersion)
      throws EncryptionException {
    try {
      ByteSource serializedPayload = payloadSerdes.reverse().convert(Optional.of(report.payload()));
      String serializedSharedInfo =
          sharedInfoSerdes.reverse().convert(Optional.of(report.sharedInfo()));

      if (serializedPayload.isEmpty()) {
        // <important> For privacy reasons, payload is not to be appended to the exception message.
        throw new EncryptionException("Serialization of report's payload failed.");
      }
      if (Strings.isNullOrEmpty(serializedSharedInfo)) {
        throw new EncryptionException("Serialization of report's shared_info failed. " + report.sharedInfo());
      }

      EncryptionKey encryptionKey = encryptionKeyService.getKey();
      EncryptionCipher encryptionCipher =
          encryptionCipherFactory.encryptionCipherFor(encryptionKey.key());
      return EncryptedReport.builder()
          .setPayload(
              encryptionCipher.encryptReport(
                  serializedPayload, serializedSharedInfo, reportVersion))
          .setKeyId(encryptionKey.id())
          .setSharedInfo(serializedSharedInfo)
          .build();
    } catch (PayloadEncryptionException
        | CipherCreationException
        | KeyFetchException
        | IOException e) {
      throw new EncryptionException(e);
    }
  }
}
