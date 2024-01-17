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

import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.aggregate.adtech.worker.encryption.EncryptionCipher;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Hybrid implementation of {@link EncryptionCipher} that performs encryption/decryption using
 * hybrid encryption. Note that the implementation for decryption comes from the worker.
 *
 * <p>Intended to match spec here:
 * https://chromium.googlesource.com/chromium/src/+/main/content/browser/aggregation_service/payload_encryption.md
 */
public final class HybridEncryptionCipher implements EncryptionCipher {

  private final KeysetHandle hybridKeysetHandle;

  private HybridEncryptionCipher(KeysetHandle keysetHandle) {
    this.hybridKeysetHandle = keysetHandle;
  }

  /**
   * Factory method to create {@link HybridEncryptionCipher}.
   *
   * @param keysetHandle must be a public key.
   */
  public static HybridEncryptionCipher of(KeysetHandle keysetHandle) {
    return new HybridEncryptionCipher(keysetHandle);
  }

  /** Encrypts Report based on LATEST_VERSION of sharedInfo. */
  @Override
  public ByteSource encryptReport(ByteSource report, String sharedInfo)
      throws PayloadEncryptionException {
    return encryptReport(report, sharedInfo, LATEST_VERSION);
  }

  @Override
  public ByteSource encryptReport(ByteSource report, String sharedInfo, String reportVersion)
      throws PayloadEncryptionException {
    try {
      HybridEncrypt hybridEncrypt = hybridKeysetHandle.getPrimitive(HybridEncrypt.class);
      String associatedDataString = ASSOCIATED_DATA_PREFIX + sharedInfo;
      byte[] encryptionAssociatedData = associatedDataString.getBytes(UTF_8);
      return ByteSource.wrap(hybridEncrypt.encrypt(report.read(), encryptionAssociatedData));
    } catch (GeneralSecurityException | IOException e) {
      throw new PayloadEncryptionException(e);
    }
  }
}
