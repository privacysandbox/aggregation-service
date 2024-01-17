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

package com.google.aggregate.adtech.worker.encryption.hybrid;

import com.google.aggregate.adtech.worker.encryption.EncryptionCipher;
import com.google.aggregate.adtech.worker.encryption.EncryptionCipherFactory;
import com.google.crypto.tink.KeysetHandle;

/**
 * {@link EncryptionCipherFactory} that provides {@link EncryptionCipher}s for the hybrid decryption
 * scheme.
 */
public final class HybridEncryptionCipherFactory implements EncryptionCipherFactory {

  /** Constructs an encryption cipher for a {@link KeysetHandle}. */
  @Override
  public EncryptionCipher encryptionCipherFor(KeysetHandle keysetHandle) {
    return HybridEncryptionCipher.of(keysetHandle);
  }
}
