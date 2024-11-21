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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.decryption.DecryptionCipher.PayloadDecryptionException;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipher;
import com.google.aggregate.adtech.worker.encryption.RecordEncrypter.EncryptionException;
import com.google.aggregate.adtech.worker.encryption.hybrid.HybridCipherModule;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.testing.FakeEncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.testing.FakeReEncryptionKeyService;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.shared.testing.StringToByteSourceConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RecordEncrypterImplTest {

  static final String ENCRYPTION_KEY_ID = "00000000-0000-0000-0000-000000000000";
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject RecordEncrypter recordEncrypter;
  @Inject KeysetHandle keysetHandle;
  @Inject PayloadSerdes payloadSerdes;
  @Inject SharedInfoSerdes sharedInfoSerdes;
  private StringToByteSourceConverter converter;

  private static final String DESTINATION = "dest.com";
  private static final UUID FIXED_UUID = UUID.randomUUID();
  private static final String REPORTING_ORIGIN = "foo.com";

  @Before
  public void setUp() throws GeneralSecurityException {
    converter = new StringToByteSourceConverter();
  }

  @Test
  public void encryptSingleReport()
      throws EncryptionException, PayloadDecryptionException, GeneralSecurityException {
    ByteSource report = converter.convert("foo");
    String sharedInfo = "bar";

    EncryptedReport encryptedReport =
        recordEncrypter.encryptSingleReport(report, sharedInfo, LATEST_VERSION);

    assertThat(converter.reverse().convert(decryptReport(encryptedReport))).isEqualTo("foo");
    assertThat(encryptedReport.keyId()).isEqualTo(ENCRYPTION_KEY_ID);
    assertThat(encryptedReport.sharedInfo()).isEqualTo(sharedInfo);
  }

  @Test
  public void encryptSerializedReport_succeeds()
      throws EncryptionException,
          PayloadDecryptionException,
          GeneralSecurityException,
          IOException {
    ImmutableList<Fact> factList =
        ImmutableList.of(Fact.builder().setBucket(BigInteger.valueOf(123)).setValue(5).build());
    Payload payload = Payload.builder().addAllFact(factList).build();
    SharedInfo sharedInfo =
        SharedInfo.builder()
            .setSourceRegistrationTime(Instant.now())
            .setDestination(DESTINATION)
            .setScheduledReportTime(Instant.now())
            .setReportId(FIXED_UUID.toString())
            .setVersion(LATEST_VERSION)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setReportingOrigin(REPORTING_ORIGIN)
            .build();
    Report deserializedReport =
        Report.builder().setPayload(payload).setSharedInfo(sharedInfo).build();
    ByteSource serializedPayload = payloadSerdes.reverse().convert(Optional.of(payload));
    String serializedSharedInfo = sharedInfoSerdes.reverse().convert(Optional.of(sharedInfo));

    EncryptedReport generatedReport =
        recordEncrypter.encryptReport(deserializedReport, "fakeuri.com");

    assertThat(serializedPayload).isNotNull();
    assertTrue(decryptReport(generatedReport).contentEquals(serializedPayload));
    assertThat(generatedReport.keyId()).isEqualTo(ENCRYPTION_KEY_ID);
    assertTrue(generatedReport.sharedInfo().contentEquals(serializedSharedInfo));
  }

  private ByteSource decryptReport(EncryptedReport encryptedReport)
      throws PayloadDecryptionException, GeneralSecurityException {
    return HybridDecryptionCipher.of(keysetHandle.getPrimitive(HybridDecrypt.class))
        .decrypt(encryptedReport.payload(), encryptedReport.sharedInfo(), LATEST_VERSION);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new HybridCipherModule());
      bind(EncryptionKeyService.class).to(FakeEncryptionKeyService.class);
      bind(RecordEncrypter.class).to(RecordEncrypterImpl.class);
      bind(ReEncryptionKeyService.class).to(FakeReEncryptionKeyService.class);
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
