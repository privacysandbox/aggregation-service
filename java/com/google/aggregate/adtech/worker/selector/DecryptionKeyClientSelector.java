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

package com.google.aggregate.adtech.worker.selector;

import com.google.inject.Module;
import com.google.scp.operator.cpio.cryptoclient.aws.AwsEnclaveMultiPartyDecryptionKeyServiceModule;
import com.google.scp.operator.cpio.cryptoclient.aws.AwsKmsMultiPartyDecryptionKeyServiceModule;
import com.google.scp.operator.cpio.cryptoclient.gcp.GcpKmsMultiPartyDecryptionKeyServiceModule;
import com.google.scp.operator.cpio.cryptoclient.local.LocalFileDecryptionKeyServiceModule;

public enum DecryptionKeyClientSelector {
  LOCAL_FILE_DECRYPTION_KEY_SERVICE(new LocalFileDecryptionKeyServiceModule()),
  // GCP multiparty implementation
  GCP_KMS_MULTI_PARTY_DECRYPTION_KEY_SERVICE(new GcpKmsMultiPartyDecryptionKeyServiceModule()),
  // Multi-party Non-enclave implementation.
  AWS_KMS_MULTI_PARTY_DECRYPTION_KEY_SERVICE(new AwsKmsMultiPartyDecryptionKeyServiceModule()),
  // Multi-party enclave implementation.
  AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE(
      new AwsEnclaveMultiPartyDecryptionKeyServiceModule());

  private final Module decryptionKeyServiceModule;

  DecryptionKeyClientSelector(Module pullerGuiceModule) {
    this.decryptionKeyServiceModule = pullerGuiceModule;
  }

  public Module getDecryptionKeyClientModule() {
    return decryptionKeyServiceModule;
  }
}
