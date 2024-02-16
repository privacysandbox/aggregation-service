/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.encryption.hybrid.key.testing;

import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKey;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService;
import com.google.crypto.tink.KeysetHandle;
import com.google.inject.Inject;
import java.security.GeneralSecurityException;

/** Fake implementation of {@link ReEncryptionKeyService} for testing. */
public final class FakeReEncryptionKeyService implements ReEncryptionKeyService {

  private final KeysetHandle keysetHandle;

  private static final String ENCRYPTION_KEY_ID = "00000000-0000-0000-0000-000000000000";

  @Inject
  FakeReEncryptionKeyService(KeysetHandle keysetHandle) {
    this.keysetHandle = keysetHandle;
  }

  @Override
  public EncryptionKey getEncryptionPublicKey(String keyVendingUri)
      throws ReencryptionKeyFetchException {
    try {
      return EncryptionKey.builder()
          .setKey(keysetHandle.getPublicKeysetHandle())
          .setId(ENCRYPTION_KEY_ID)
          .build();
    } catch (GeneralSecurityException e) {
      throw new ReencryptionKeyFetchException(e);
    }
  }
}
