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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipher;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipherFactory.CipherCreationException;
import com.google.aggregate.adtech.worker.exceptions.InternalServerException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.common.io.ByteSource;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;
import java.security.AccessControlException;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HybridDecryptionCipherFactoryTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject HybridDecryptionCipherFactory hybridDecryptionCipherFactory;

  @Inject FakeDecryptionKeyService fakeDecryptionKeyService;

  @Test
  public void testThrowsOnFailedFetch() {
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(ByteSource.empty())
            .setKeyId(UUID.randomUUID().toString())
            .setSharedInfo("")
            .build();
    fakeDecryptionKeyService.setShouldThrow(true);

    CipherCreationException exception =
        assertThrows(
            CipherCreationException.class,
            () -> hybridDecryptionCipherFactory.decryptionCipherFor(encryptedReport));
    assertThat(exception).hasCauseThat().isInstanceOf(KeyFetchException.class);
    assertThat(exception.reason).isEqualTo(ErrorReason.UNKNOWN_ERROR);
  }

  @Test
  public void testThrowsOnPermissionFailureFetch() {
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(ByteSource.empty())
            .setKeyId(UUID.randomUUID().toString())
            .setSharedInfo("")
            .build();
    fakeDecryptionKeyService.setShouldThrow(true, ErrorReason.PERMISSION_DENIED);

    assertThrows(
        AccessControlException.class,
        () -> hybridDecryptionCipherFactory.decryptionCipherFor(encryptedReport));
  }

  /** Test that DecryptionKeyService is requested with the right key */
  @Test
  public void testRequestsWithTheRightKey() throws CipherCreationException {
    String keyId = UUID.randomUUID().toString();
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(ByteSource.empty())
            .setKeyId(keyId)
            .setSharedInfo("")
            .build();

    DecryptionCipher decryptionCipher =
        hybridDecryptionCipherFactory.decryptionCipherFor(encryptedReport);

    assertThat(decryptionCipher).isInstanceOf(HybridDecryptionCipher.class);
    assertThat(fakeDecryptionKeyService.getLastKeyIdUsed()).isEqualTo(keyId);
  }

  @Test
  public void decryptionCipherFor_throwsServiceUnavailable() {
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(ByteSource.empty())
            .setKeyId(UUID.randomUUID().toString())
            .setSharedInfo("")
            .build();
    fakeDecryptionKeyService.setShouldThrow(true, ErrorReason.KEY_SERVICE_UNAVAILABLE);

    assertThrows(
        InternalServerException.class,
        () -> hybridDecryptionCipherFactory.decryptionCipherFor(encryptedReport));
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeDecryptionKeyService.class).in(TestScoped.class);
      bind(DecryptionKeyService.class).to(FakeDecryptionKeyService.class);
    }
  }
}
