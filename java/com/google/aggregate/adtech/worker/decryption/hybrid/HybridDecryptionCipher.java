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

import static com.google.aggregate.adtech.worker.model.SharedInfo.DEFAULT_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Implementation of {@code DecryptionCipher} that uses a key provided by the aggregate KMS service.
 *
 * <p>Intended to match spec here:
 * https://chromium.googlesource.com/chromium/src/+/main/content/browser/aggregation_service/payload_encryption.md
 */
public final class HybridDecryptionCipher implements DecryptionCipher {

  // Prefix for payloads intended for aggregate service
  public static final String ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR = "aggregation_service\0";
  public static final String ASSOCIATED_DATA_PREFIX = "aggregation_service";

  private final HybridDecrypt hybridDecrypt;

  public static HybridDecryptionCipher of(HybridDecrypt hybridDecrypt) {
    return new HybridDecryptionCipher(hybridDecrypt);
  }

  private HybridDecryptionCipher(HybridDecrypt hybridDecrypt) {
    this.hybridDecrypt = hybridDecrypt;
  }

  @Override
  public ByteSource decrypt(
      ByteSource encryptedPayload, String sharedInfo, String sharedInfoVersion)
      throws PayloadDecryptionException {
    try {
      String associatedDataString = "";

      /*
       * Only reports with empty version need NULL terminator in associatedDataString while decryption
       */
      if (sharedInfoVersion.isEmpty() || sharedInfoVersion.equals(DEFAULT_VERSION)) {
        associatedDataString = ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR + sharedInfo;
      } else if (sharedInfoVersion.equals(VERSION_0_1)) {
        associatedDataString = ASSOCIATED_DATA_PREFIX + sharedInfo;
      }

      byte[] associatedData = associatedDataString.getBytes(UTF_8);
      return ByteSource.wrap(hybridDecrypt.decrypt(encryptedPayload.read(), associatedData));
    } catch (GeneralSecurityException | IOException e) {
      throw new PayloadDecryptionException(e);
    }
  }
}
