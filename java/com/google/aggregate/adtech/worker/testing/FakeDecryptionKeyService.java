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

package com.google.aggregate.adtech.worker.testing;

import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fake {@code DecryptionKeyService} that returns hybrid decryption keys. Keys returned by getKey
 * will be consistent between multiple invocations for the same key id (tied to the lifespan of the
 * service). Can be set to throw exceptions and saves the last keyId it was called with.
 */
public class FakeDecryptionKeyService implements DecryptionKeyService {

  private boolean shouldThrow;
  private boolean shouldThrowPermissionException;
  private String lastKeyIdUsed;
  private Map<String, KeysetHandle> keyMap = new HashMap<String, KeysetHandle>();

  public FakeDecryptionKeyService() {
    this.shouldThrow = false;
  }

  /** Generates a new hybrid decryption key. */
  private static KeysetHandle createKey() {
    try {
      HybridConfig.register();
      return KeysetHandle.generateNew(
          EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to create fake key", e);
    }
  }

  @Override
  public HybridDecrypt getDecrypter(String keyId) throws KeyFetchException {
    if (shouldThrowPermissionException) {
      throw new KeyFetchException(
          new RuntimeException("Permission Denied"), ErrorReason.PERMISSION_DENIED);
    }
    if (shouldThrow) {
      throw new KeyFetchException(
          new IllegalStateException("FakeDecryptionKeyService was set to throw"),
          ErrorReason.UNKNOWN_ERROR);
    }

    lastKeyIdUsed = keyId;

    try {
      return getKeysetHandle(keyId).getPrimitive(HybridDecrypt.class);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unexpected key generation error", e);
    }
  }

  public void setShouldThrow(boolean shouldThrow) {
    this.shouldThrow = shouldThrow;
  }

  public void setShouldThrowPermissionException(boolean shouldThrowPermissionException) {
    this.shouldThrowPermissionException = shouldThrowPermissionException;
  }

  public String getLastKeyIdUsed() {
    return lastKeyIdUsed;
  }

  /** Returns the keyset handle with the specified key id, generating a new key if necessary. */
  public KeysetHandle getKeysetHandle(String keyId) {
    return keyMap.computeIfAbsent(keyId, (k) -> createKey());
  }

  /** Helper function for generating ciphertext encrypted with the specified key id. */
  public ByteSource generateCiphertext(String keyId, ByteSource plaintext, String sharedInfo)
      throws KeyFetchException, GeneralSecurityException, IOException {
    var keysetHandle = getKeysetHandle(keyId).getPublicKeysetHandle();

    var hybridEncrypt = keysetHandle.getPrimitive(HybridEncrypt.class);
    /**
     * TODO(b/238842472) More info at-
     * java/com/google/aggregate/simulation/encryption/RecordEncrypter.java
     */
    byte[] contextInfo = (ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR + sharedInfo).getBytes(UTF_8);
    return ByteSource.wrap(hybridEncrypt.encrypt(plaintext.read(), contextInfo));
  }
}
