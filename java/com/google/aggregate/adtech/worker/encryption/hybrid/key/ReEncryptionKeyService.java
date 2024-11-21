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

package com.google.aggregate.adtech.worker.encryption.hybrid.key;

/** Interface for retrieving public encryption keys from provided public key hosting URI */
public interface ReEncryptionKeyService {

  /** Retrieve a key from the aggregate service KMS */
  EncryptionKey getEncryptionPublicKey(String keyVendingUri) throws ReencryptionKeyFetchException;

  final class ReencryptionKeyFetchException extends Exception {
    public ReencryptionKeyFetchException(Throwable cause) {
      super(cause);
    }

    public ReencryptionKeyFetchException(String message) {
      super(message);
    }
  }
}
