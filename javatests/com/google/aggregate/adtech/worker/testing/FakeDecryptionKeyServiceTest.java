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

import static com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher.ASSOCIATED_DATA_PREFIX;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.io.ByteSource;
import com.google.crypto.tink.KeysetHandle;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeDecryptionKeyServiceTest {

  // Under test
  FakeDecryptionKeyService fakeDecryptionKeyService;

  @Before
  public void setUp() {
    fakeDecryptionKeyService = new FakeDecryptionKeyService();
  }

  @Test
  public void testThrowsWhenSet() {
    fakeDecryptionKeyService.setShouldThrow(true);

    assertThrows(
        KeyFetchException.class, () -> fakeDecryptionKeyService.getDecrypter("random string"));
  }

  @Test
  public void testSavesLastKeyIdUsed() throws Exception {
    String keyId = UUID.randomUUID().toString();

    fakeDecryptionKeyService.getDecrypter(keyId);

    assertThat(fakeDecryptionKeyService.getLastKeyIdUsed()).isEqualTo(keyId);
  }

  // tests getKeysethandle() rather than get() because the HybridDecrypt interface returned by get()
  // doesn't have a proper equals method implmented.
  @Test
  public void testGetKeysetHandleCreatesPersistentKey() throws Exception {
    var keyId = UUID.randomUUID().toString();

    // implicitly created key.
    KeysetHandle firstCall = fakeDecryptionKeyService.getKeysetHandle(keyId);
    KeysetHandle secondCall = fakeDecryptionKeyService.getKeysetHandle(keyId);

    assertThat(firstCall).isEqualTo(secondCall);
  }

  @Test
  public void testCreateCiphertextWithContextInfo() throws Exception {
    var keyId = UUID.randomUUID().toString();
    var hybridDecrypt = fakeDecryptionKeyService.getDecrypter(keyId);
    var plaintext = "foo".getBytes();
    var contextInfo = "context info";

    var cipherText =
        fakeDecryptionKeyService.generateCiphertext(keyId, ByteSource.wrap(plaintext), contextInfo);

    var decryptedText =
        hybridDecrypt.decrypt(
            cipherText.read(), (ASSOCIATED_DATA_PREFIX + contextInfo).getBytes(UTF_8));
    assertThat(decryptedText).isEqualTo(plaintext);
  }
}
