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
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService.ReencryptionKeyFetchException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import java.util.Optional;

/** {@link RecordEncrypter} implementation. */
public final class RecordEncrypterImpl implements RecordEncrypter {

  private final EncryptionCipherFactory encryptionCipherFactory;
  private final EncryptionKeyService encryptionKeyService;
  private final ReEncryptionKeyService reEncryptionKeyService;
  private final PayloadSerdes payloadSerdes;
  private final SharedInfoSerdes sharedInfoSerdes;

  @Inject
  public RecordEncrypterImpl(
      EncryptionCipherFactory encryptionCipherFactory,
      EncryptionKeyService encryptionKeyService,
      ReEncryptionKeyService reEncryptionKeyService,
      PayloadSerdes payloadSerdes,
      SharedInfoSerdes sharedInfoSerdes) {
    this.encryptionCipherFactory = encryptionCipherFactory;
    this.encryptionKeyService = encryptionKeyService;
    this.reEncryptionKeyService = reEncryptionKeyService;
    this.payloadSerdes = payloadSerdes;
    this.sharedInfoSerdes = sharedInfoSerdes;
  }

  @Override
  public EncryptedReport encryptSingleReport(
      ByteSource report, String sharedInfo, String reportVersion) throws EncryptionException {

    try {
      EncryptionKey encryptionKey = encryptionKeyService.getKey();
      EncryptionCipher encryptionCipher =
          encryptionCipherFactory.encryptionCipherFor(encryptionKey.key());
      return EncryptedReport.builder()
          .setPayload(encryptionCipher.encryptReport(report, sharedInfo, reportVersion))
          .setKeyId(encryptionKey.id())
          .setSharedInfo(sharedInfo)
          .build();
    } catch (PayloadEncryptionException | CipherCreationException | KeyFetchException e) {
      throw new EncryptionException(e);
    }
  }

  @Override
  public EncryptedReport encryptReport(Report report, String cloudEncryptionKeyVendingUri)
      throws EncryptionException {
    try {
      EncryptionKey encryptionKey =
          reEncryptionKeyService.getEncryptionPublicKey(cloudEncryptionKeyVendingUri);
      EncryptionCipher encryptionCipher =
          encryptionCipherFactory.encryptionCipherFor(encryptionKey.key());
      String sharedInfoString =
          sharedInfoSerdes.reverse().convert(Optional.of(report.sharedInfo()));
      return EncryptedReport.builder()
          .setPayload(
              encryptionCipher.encryptReport(
                  payloadSerdes.reverse().convert(Optional.of(report.payload())),
                  sharedInfoString,
                  report.sharedInfo().version()))
          .setKeyId(encryptionKey.id())
          .setSharedInfo(sharedInfoString)
          .build();
    } catch (CipherCreationException | ReencryptionKeyFetchException e) {
      throw new EncryptionException(e);
    } catch (PayloadEncryptionException e) {
      throw new EncryptionException("Encountered PayloadEncryptionException.");
    }
  }
}
