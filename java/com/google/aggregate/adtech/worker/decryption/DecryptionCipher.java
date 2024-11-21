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

package com.google.aggregate.adtech.worker.decryption;

import com.google.common.io.ByteSource;

/** Interface responsible for decrypting raw bytes */
public interface DecryptionCipher {

  /** Decrypt the provided payload */
  public ByteSource decrypt(
      ByteSource encryptedPayload, String sharedInfo, String sharedInfoVersion)
      throws PayloadDecryptionException;

  class PayloadDecryptionException extends Exception {

    public PayloadDecryptionException(Throwable cause) {
      super(cause);
    }
  }
}
