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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher.PayloadDecryptionException;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher;
import com.google.aggregate.adtech.worker.encryption.EncryptionCipher.PayloadEncryptionException;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.scp.operator.shared.testing.StringToByteSourceConverter;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HybridEncryptionCipherTest {

  private KeysetHandle keysetHandle;
  // Under test.
  private HybridEncryptionCipher hybridEncryptionCipher;

  private StringToByteSourceConverter converter;

  @Before
  public void setUp() throws Exception {
    HybridConfig.register(); // Config must be registered before calling encryption or decryption
    keysetHandle =
        KeysetHandle.generateNew(
            EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());
    hybridEncryptionCipher = HybridEncryptionCipher.of(keysetHandle.getPublicKeysetHandle());
    converter = new StringToByteSourceConverter();
  }

  @Test
  public void sampleEncryptLongReportSharedInfo()
      throws PayloadEncryptionException, PayloadDecryptionException, GeneralSecurityException {
    String message = Strings.repeat("This is a secret", 1000);
    String sharedInfo = "Associated";

    ByteSource encryptedReport =
        hybridEncryptionCipher.encryptReport(
            converter.convert(message), sharedInfo, LATEST_VERSION);
    String decryptedMessage =
        converter.reverse().convert(decryptReport(encryptedReport, sharedInfo));

    // Checks that once the bytes are decrypted, message is back.
    assertThat(decryptedMessage).isEqualTo(message);
  }

  private ByteSource decryptReport(ByteSource message, String sharedInfo)
      throws PayloadDecryptionException, GeneralSecurityException {
    return HybridDecryptionCipher.of(keysetHandle.getPrimitive(HybridDecrypt.class))
        .decrypt(message, sharedInfo, LATEST_VERSION);
  }
}
