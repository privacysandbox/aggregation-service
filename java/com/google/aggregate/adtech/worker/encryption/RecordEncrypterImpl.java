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
import com.google.common.io.ByteSource;
import com.google.inject.Inject;

/** {@link RecordEncrypter} implementation. */
public final class RecordEncrypterImpl implements RecordEncrypter {

  private final EncryptionCipherFactory encryptionCipherFactory;
  private final EncryptionKeyService encryptionKeyService;

  @Inject
  public RecordEncrypterImpl(
      EncryptionCipherFactory encryptionCipherFactory, EncryptionKeyService encryptionKeyService) {
    this.encryptionCipherFactory = encryptionCipherFactory;
    this.encryptionKeyService = encryptionKeyService;
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
}
