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

package com.google.aggregate.adtech.worker.decryption.noop;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher;
import com.google.common.io.ByteSource;

/** Cipher that does not do anything, i.e. just passes the bytes through */
public final class NoopDecryptionCipher implements DecryptionCipher {

  @Override
  public ByteSource decrypt(
      ByteSource encryptedPayload, String sharedInfo, String sharedInfoVersion)
      throws PayloadDecryptionException {
    return encryptedPayload;
  }
}
