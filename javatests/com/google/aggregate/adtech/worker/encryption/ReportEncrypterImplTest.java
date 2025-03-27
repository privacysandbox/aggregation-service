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

package com.google.aggregate.adtech.worker.encryption;

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipher.PayloadDecryptionException;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher;
import com.google.aggregate.adtech.worker.encryption.ReportEncrypter.EncryptionException;
import com.google.aggregate.adtech.worker.encryption.hybrid.HybridCipherModule;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.testing.FakeEncryptionKeyService;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.time.Instant;
import org.junit.Rule;
import org.junit.Test;

public class ReportEncrypterImplTest {

  static final String ENCRYPTION_KEY_ID = "00000000-0000-0000-0000-000000000000";
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject private ReportEncrypter reportEncrypter;
  @Inject private KeysetHandle keysetHandle;
  @Inject private PayloadSerdes payloadSerdes;
  @Inject private SharedInfoSerdes sharedInfoSerdes;

  @Test
  public void encryptSingleReport()
      throws EncryptionException, PayloadDecryptionException, GeneralSecurityException {
    Payload payload =
        Payload.builder()
            .setOperation("foo")
            .addFact(Fact.builder().setBucket(BigInteger.TEN).setValue(300).build())
            .build();
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setScheduledReportTime(Instant.now())
            .setReportingOrigin("foo.com")
            .setVersion(LATEST_VERSION)
            .build();
    Report report = Report.builder().setPayload(payload).setSharedInfo(sharedInfo).build();

    EncryptedReport encryptedReport = reportEncrypter.encryptSingleReport(report, LATEST_VERSION);

    assertThat(getDecryptedPayload(encryptedReport)).isEqualTo(payload);
    assertThat(encryptedReport.keyId()).isEqualTo(ENCRYPTION_KEY_ID);
    assertThat(sharedInfoSerdes.convert(encryptedReport.sharedInfo()).get()).isEqualTo(sharedInfo);
  }

  @Test
  public void encryptSingleReport_withEmptyPayload()
      throws EncryptionException, PayloadDecryptionException, GeneralSecurityException {
    Payload payload = Payload.builder().build();
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setScheduledReportTime(Instant.now())
            .setReportingOrigin("foo.com")
            .setVersion(LATEST_VERSION)
            .build();
    Report report = Report.builder().setPayload(payload).setSharedInfo(sharedInfo).build();

    EncryptedReport encryptedReport = reportEncrypter.encryptSingleReport(report, LATEST_VERSION);

    assertThat(getDecryptedPayload(encryptedReport)).isEqualTo(payload);
    assertThat(encryptedReport.keyId()).isEqualTo(ENCRYPTION_KEY_ID);
    assertThat(sharedInfoSerdes.convert(encryptedReport.sharedInfo()).get()).isEqualTo(sharedInfo);
  }

  private Payload getDecryptedPayload(EncryptedReport encryptedReport)
      throws PayloadDecryptionException, GeneralSecurityException {
    return payloadSerdes
        .convert(
            HybridDecryptionCipher.of(keysetHandle.getPrimitive(HybridDecrypt.class))
                .decrypt(encryptedReport.payload(), encryptedReport.sharedInfo(), LATEST_VERSION))
        .get();
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new HybridCipherModule());
      bind(EncryptionKeyService.class).to(FakeEncryptionKeyService.class);
      bind(ReportEncrypter.class).to(ReportEncrypterImpl.class);
      bind(PayloadSerdes.class).to(CborPayloadSerdes.class);
    }

    @Provides
    @Singleton
    public KeysetHandle keysetHandleProvider() throws GeneralSecurityException {
      HybridConfig.register();
      return KeysetHandle.generateNew(
          EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());
    }
  }
}
