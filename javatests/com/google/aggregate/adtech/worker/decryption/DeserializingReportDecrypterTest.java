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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter.DecryptionException;
import com.google.aggregate.adtech.worker.decryption.hybrid.HybridDecryptionCipherFactory;
import com.google.aggregate.adtech.worker.encryption.EncryptionCipher;
import com.google.aggregate.adtech.worker.encryption.hybrid.HybridEncryptionCipher;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.aggregate.adtech.worker.model.serdes.SharedInfoSerdes;
import com.google.aggregate.adtech.worker.model.serdes.cbor.CborPayloadSerdes;
import com.google.aggregate.adtech.worker.testing.FakeDecryptionKeyService;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.io.ByteSource;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.security.GeneralSecurityException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeserializingReportDecrypterTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject private DeserializingReportDecrypter deserializingReportDecrypter;

  @Inject EncryptionCipher encryptionCipher;

  @Inject PayloadSerdes payloadSerdes;
  @Inject SharedInfoSerdes sharedInfoSerdes;

  // Report used in testing
  private Report report;

  // Should decrypt and deserialize without exceptions
  private EncryptedReport encryptedReport;

  // Should decrypt correctly but fail to deserialize
  private EncryptedReport garbageReportEncryptedWithCorrectKey;

  private static final String DECRYPTION_KEY_ID = "8dab7951-e459-4a19-bd6c-d81c0a600f86";

  private String sharedInfo;

  @Before
  public void setUp() throws Exception {
    report =
        FakeReportGenerator.generateWithParam(
            1, /* reportVersion */ LATEST_VERSION, "https/foo.com");
    sharedInfo = sharedInfoSerdes.reverse().convert(Optional.of(report.sharedInfo()));
    encryptReport();
  }

  /** Test for decrypting with shared info a single report with no errors */
  @Test
  public void testSimpleDecryption() throws Exception {
    // No setup

    Report decryptedReport = deserializingReportDecrypter.decryptSingleReport(encryptedReport);

    assertThat(decryptedReport).isEqualTo(report);
  }

  /** Test error handling for failed sharedInfo deserialization */
  @Test
  public void testExceptionInSharedInfoDeserialization() throws Exception {
    sharedInfo = "{ \"bad_field\": \"foo\" }";
    encryptReport();

    DecryptionException decryptionException =
        assertThrows(
            DecryptionException.class,
            () -> deserializingReportDecrypter.decryptSingleReport(encryptedReport));
    assertThat(decryptionException)
        .hasCauseThat()
        .hasMessageThat()
        .contains("Couldn't deserialize shared_info");
  }

  /** Test error handling for modified sharedInfo after report encryption */
  @Test
  public void testExceptionInDeserializationWithModifiedSharedInfo() throws Exception {
    encryptReport();

    // Modify the shared info after the report has been encrypted.
    SharedInfo originalSharedInfo = sharedInfoSerdes.convert(encryptedReport.sharedInfo()).get();
    SharedInfo modifiedSharedInfo =
        originalSharedInfo.toBuilder().setReportingOrigin("newReportingOrigin.com").build();

    encryptedReport =
        EncryptedReport.builder()
            .setPayload(encryptedReport.payload())
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfoSerdes.reverse().convert(Optional.of(modifiedSharedInfo)))
            .build();

    DecryptionException decryptionException =
        assertThrows(
            DecryptionException.class,
            () -> deserializingReportDecrypter.decryptSingleReport(encryptedReport));
    assertThat(decryptionException).hasCauseThat().hasMessageThat().contains("decryption failed");
  }

  /** Test error handling for failed payload deserialization */
  @Test
  public void testExceptionInPayloadDeserialization() {
    // No setup

    DecryptionException decryptionException =
        assertThrows(
            DecryptionException.class,
            () ->
                deserializingReportDecrypter.decryptSingleReport(
                    garbageReportEncryptedWithCorrectKey));
    assertThat(decryptionException)
        .hasCauseThat()
        .hasMessageThat()
        .contains("Decrypted payload could not be deserialized");
  }

  @Test
  public void decryptSingleReport_withSourceRegistrationTimeZero() throws Exception {
    Report report =
        FakeReportGenerator.generateWithFixedReportId(
            /* dummyValue= */ 1, /* reportId= */ "report_id", /* reportVersion */ LATEST_VERSION);
    String sharedInfo =
        "{\"source_registration_time\":0,\"version\":\"0.1\",\"reporting_origin\":\"1\",\"attribution_destination\":\"1\",\"report_id\":\"report_id\",\"scheduled_report_time\":1.000000000,"
            + " \"api\":\"attribution-reporting\"}";
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(
                encryptionCipher.encryptReport(
                    payloadSerdes.reverse().convert(Optional.of(report.payload())),
                    sharedInfo,
                    LATEST_VERSION))
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfo)
            .build();

    Report decryptedReport = deserializingReportDecrypter.decryptSingleReport(encryptedReport);

    SharedInfo deserializedSharedInfo = sharedInfoSerdes.convert(sharedInfo).get();
    assertThat(decryptedReport.sharedInfo()).isEqualTo(deserializedSharedInfo);
  }

  @Test
  public void decryptSingleReport_withSourceRegistrationTimeNegative() throws Exception {
    Report report =
        FakeReportGenerator.generateWithFixedReportId(
            /* dummyValue= */ 1, /* reportId= */ "report_id", /* reportVersion */ LATEST_VERSION);
    String sharedInfo =
        "{\"source_registration_time\":0,\"version\":\"0.1\",\"reporting_origin\":\"1\",\"attribution_destination\":\"1\",\"report_id\":\"report_id\",\"scheduled_report_time\":1.000000000,"
            + " \"api\":\"attribution-reporting\"}";
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(
                encryptionCipher.encryptReport(
                    payloadSerdes.reverse().convert(Optional.of(report.payload())),
                    sharedInfo,
                    LATEST_VERSION))
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfo)
            .build();

    Report decryptedReport = deserializingReportDecrypter.decryptSingleReport(encryptedReport);

    SharedInfo deserializedSharedInfo = sharedInfoSerdes.convert(sharedInfo).get();
    assertThat(decryptedReport.sharedInfo()).isEqualTo(deserializedSharedInfo);
  }

  @Test
  public void decryptSingleReport_withNoSourceRegistrationTime() throws Exception {
    Report report =
        FakeReportGenerator.generateWithFixedReportId(
            /* dummyValue= */ 1, /* reportId= */ "report_id", /* reportVersion */ LATEST_VERSION);
    String sharedInfo =
        "{\"version\":\"0.1\",\"reporting_origin\":\"1\",\"attribution_destination\":\"1\",\"report_id\":\"report_id\",\"scheduled_report_time\":1.000000000,"
            + " \"api\":\"attribution-reporting\"}";
    EncryptedReport encryptedReport =
        EncryptedReport.builder()
            .setPayload(
                encryptionCipher.encryptReport(
                    payloadSerdes.reverse().convert(Optional.of(report.payload())),
                    sharedInfo,
                    LATEST_VERSION))
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfo)
            .build();

    Report decryptedReport = deserializingReportDecrypter.decryptSingleReport(encryptedReport);

    assertTrue(decryptedReport.sharedInfo().sourceRegistrationTime().isEmpty());
  }

  private void encryptReport() throws Exception {
    ByteSource serializedPayload = payloadSerdes.reverse().convert(Optional.of(report.payload()));
    encryptedReport =
        EncryptedReport.builder()
            .setPayload(
                encryptionCipher.encryptReport(serializedPayload, sharedInfo, LATEST_VERSION))
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfo)
            .build();
    ByteSource garbageBytesEncryptedWithCorrectKey =
        encryptionCipher.encryptReport(
            ByteSource.wrap(new byte[] {0x00, 0x01}), sharedInfo, LATEST_VERSION);
    garbageReportEncryptedWithCorrectKey =
        EncryptedReport.builder()
            .setPayload(garbageBytesEncryptedWithCorrectKey)
            .setKeyId(DECRYPTION_KEY_ID)
            .setSharedInfo(sharedInfo)
            .build();
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(ObjectMapper.class).to(TimeObjectMapper.class);
      bind(PayloadSerdes.class).to(CborPayloadSerdes.class);
      bind(DecryptionCipherFactory.class).to(HybridDecryptionCipherFactory.class);
      bind(FakeDecryptionKeyService.class).in(TestScoped.class);
      bind(DecryptionKeyService.class).to(FakeDecryptionKeyService.class);
    }

    @Provides
    EncryptionCipher provideEncryptionCipher(FakeDecryptionKeyService decryptionService)
        throws GeneralSecurityException {
      return HybridEncryptionCipher.of(
          decryptionService.getKeysetHandle(DECRYPTION_KEY_ID).getPublicKeysetHandle());
    }
  }
}
