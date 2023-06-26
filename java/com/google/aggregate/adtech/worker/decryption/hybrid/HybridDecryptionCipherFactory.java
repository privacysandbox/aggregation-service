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

package com.google.aggregate.adtech.worker.decryption.hybrid;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipherFactory;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import java.security.AccessControlException;

/**
 * {@code DecryptionCipherFactory} that provides {@code DecryptionCipher}s for the hybrid decryption
 * scheme.
 *
 * <p>Inspects the provided {@code EncryptedReport} for the key used to decrypt, retrieves the key
 * it needs from the {@code DecryptionKeyService}, and constructs a decryption cipher using that
 * key.
 */
public final class HybridDecryptionCipherFactory implements DecryptionCipherFactory {

  private final DecryptionKeyService decryptionKeyService;

  @Inject
  public HybridDecryptionCipherFactory(DecryptionKeyService decryptionKeyService) {
    this.decryptionKeyService = decryptionKeyService;
  }

  /** Retrieves the key needed to decrypt the report and constucts a decryption cipher for it. */
  @Override
  public DecryptionCipher decryptionCipherFor(EncryptedReport encryptedReport)
      throws CipherCreationException {

    try {
      var decryptionKey = decryptionKeyService.getDecrypter(encryptedReport.keyId());
      return HybridDecryptionCipher.of(decryptionKey);
    } catch (KeyFetchException e) {
      switch (e.getReason()) {
        case PERMISSION_DENIED:
          throw new AccessControlException("Permission denied in fetching decryption keys.");
        default:
          throw new CipherCreationException(e, e.getReason());
      }
    }
  }
}
