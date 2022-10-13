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

import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX;
import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR;
import static com.google.aggregate.adtech.worker.model.SharedInfo.DEFAULT_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.VERSION_0_1;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipher.PayloadDecryptionException;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HybridDecryptionCipherTest {

  private KeysetHandle keysetHandle;

  private HybridDecryptionCipher hybridDecryptionCipher;

  @Before
  public void setUp() throws Exception {
    HybridConfig.register(); // Config must be registered before calling encryption or decryption
    keysetHandle =
        KeysetHandle.generateNew(
            EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());
    hybridDecryptionCipher =
        HybridDecryptionCipher.of(keysetHandle.getPrimitive(HybridDecrypt.class));
  }

  @Test
  public void decryptionTestWithContextInfo() throws Exception {
    String message = Strings.repeat("This is a secret", 10000);
    String sharedInfo = "Context info";
    byte[] encryptedPayload =
        hybridEncryptData(
            message.getBytes(UTF_8),
            (ASSOCIATED_DATA_PREFIX_WITH_NULL_TERMINATOR + sharedInfo).getBytes(UTF_8));

    ByteSource decryptedPayload =
        hybridDecryptionCipher.decrypt(
            ByteSource.wrap(encryptedPayload), sharedInfo, DEFAULT_VERSION);

    String decryptedMessage = new String(decryptedPayload.read(), UTF_8);
    assertThat(decryptedMessage).isEqualTo(message);
  }

  @Test
  public void decryptionTestWithSharedInfoVersionZeroDotOne() throws Exception {
    String message = Strings.repeat("This is a secret", 10000);
    String sharedInfo =
        SharedInfo.Builder.builder()
            .setDestination("conversion.test")
            .setReportingOrigin("report.test")
            .setScheduledReportTime(Instant.EPOCH)
            .setSourceRegistrationTime(Instant.EPOCH)
            .setApi("attribution-reporting")
            .setVersion(VERSION_0_1)
            .build()
            .toString();
    byte[] encryptedPayload =
        hybridEncryptData(
            message.getBytes(UTF_8), (ASSOCIATED_DATA_PREFIX + sharedInfo).getBytes(UTF_8));

    ByteSource decryptedPayload =
        hybridDecryptionCipher.decrypt(ByteSource.wrap(encryptedPayload), sharedInfo, VERSION_0_1);

    String decryptedMessage = new String(decryptedPayload.read(), UTF_8);
    assertThat(decryptedMessage).isEqualTo(message);
  }

  @Test
  public void decryptionTestWithSharedInfoZeroDotOneWrongDecryptionVersion() throws Exception {
    String message = Strings.repeat("This is a secret", 10000);
    String sharedInfo =
        SharedInfo.Builder.builder()
            .setDestination("conversion.test")
            .setReportingOrigin("report.test")
            .setScheduledReportTime(Instant.EPOCH)
            .setSourceRegistrationTime(Instant.EPOCH)
            .setApi("attribution-reporting")
            .setVersion(VERSION_0_1)
            .build()
            .toString();
    byte[] encryptedPayload =
        hybridEncryptData(
            message.getBytes(UTF_8), (ASSOCIATED_DATA_PREFIX + sharedInfo).getBytes(UTF_8));

    assertThrows(
        PayloadDecryptionException.class,
        () ->
            hybridDecryptionCipher.decrypt(
                ByteSource.wrap(encryptedPayload), sharedInfo, DEFAULT_VERSION));
  }

  /** Test that an exception is thrown if the sharedInfo does not match */
  @Test
  public void decryptionTestWithWrongSharedInfo() throws Exception {
    String message = Strings.repeat("This is a secret", 10000);
    String encryptionSharedInfo = "Context info";
    String decryptionSharedInfo = "Different context info";
    byte[] encryptedPayload =
        hybridEncryptData(
            message.getBytes(UTF_8),
            (ASSOCIATED_DATA_PREFIX + encryptionSharedInfo).getBytes(UTF_8));

    assertThrows(
        PayloadDecryptionException.class,
        () ->
            hybridDecryptionCipher.decrypt(
                ByteSource.wrap(encryptedPayload), decryptionSharedInfo, DEFAULT_VERSION));
  }

  /** Test that an exception is thrown the wrong key is used */
  @Test
  public void decryptionTestWithWrongKey() throws Exception {
    String message = Strings.repeat("This is a secret", 10000);
    String sharedInfo = "Context info";
    KeysetHandle differentKeysetHandle =
        KeysetHandle.generateNew(
            EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());
    byte[] encryptedPayload =
        hybridEncryptData(
            message.getBytes(UTF_8), sharedInfo.getBytes(UTF_8), differentKeysetHandle);

    assertThrows(
        PayloadDecryptionException.class,
        () ->
            hybridDecryptionCipher.decrypt(
                ByteSource.wrap(encryptedPayload), sharedInfo, DEFAULT_VERSION));
  }

  private byte[] hybridEncryptData(byte[] plaintextPayload, byte[] contextInfo) throws Exception {
    return hybridEncryptData(plaintextPayload, contextInfo, keysetHandle);
  }

  private byte[] hybridEncryptData(
      byte[] plaintextPayload, byte[] contextInfo, KeysetHandle keysetHandle) throws Exception {
    HybridEncrypt hybridEncrypt =
        keysetHandle.getPublicKeysetHandle().getPrimitive(HybridEncrypt.class);
    return hybridEncrypt.encrypt(plaintextPayload, contextInfo);
  }
}
