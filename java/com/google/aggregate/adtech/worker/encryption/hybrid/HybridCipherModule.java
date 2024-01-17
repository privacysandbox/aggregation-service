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

import com.google.aggregate.adtech.worker.encryption.CipherModule;
import com.google.aggregate.adtech.worker.encryption.EncryptionCipherFactory;
import com.google.crypto.tink.hybrid.HybridConfig;

import java.security.GeneralSecurityException;

/** Module to be used when the hybrid encryption/decryption scheme is in use. */
public final class HybridCipherModule extends CipherModule {

  @Override
  public Class<? extends EncryptionCipherFactory> getEncryptionCipherFactoryImplementation() {
    return HybridEncryptionCipherFactory.class;
  }

  @Override
  public void configureModule() {
    // Register the Tink HybridConfig so that hybrid decryption can be used
    try {
      HybridConfig.register();
    } catch (GeneralSecurityException e) {
      // Throw as unchecked exception since this should be fatal
      throw new IllegalStateException("Could not register Tink HybridConfig", e);
    }
  }
}
