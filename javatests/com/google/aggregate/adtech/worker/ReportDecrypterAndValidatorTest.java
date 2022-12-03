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

package com.google.aggregate.adtech.worker;

import static com.google.common.truth.Truth8.assertThat;
import static com.google.scp.operator.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory.DECRYPTION_ERROR;
import static com.google.scp.operator.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory.GENERAL_ERROR;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.testing.FakeRecordDecrypter;
import com.google.aggregate.adtech.worker.testing.FakeReportGenerator;
import com.google.aggregate.adtech.worker.testing.FakeValidator;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReportDecrypterAndValidatorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject FakeValidator fakeValidator = new FakeValidator();

  @Inject FakeRecordDecrypter fakeRecordDecrypter;

  // Under test
  @Inject private ReportDecrypterAndValidator reportDecrypterAndValidator;

  private Job ctx;
  private EncryptedReport encryptedReport;

  @Before
  public void setUp() {
    ctx = FakeJobGenerator.generate("foo");
    encryptedReport =
        EncryptedReport.builder()
            .setPayload(ByteSource.wrap(new byte[] {0x00, 0x01}))
            .setKeyId(UUID.randomUUID().toString())
            .setSharedInfo("")
            .build();
    fakeValidator.setNextShouldReturnError(ImmutableList.of(false).iterator());
  }

  @Test
  public void testBasic() {
    DecryptionValidationResult decryptionValidationResult =
        reportDecrypterAndValidator.decryptAndValidate(encryptedReport, ctx);

    assertThat(decryptionValidationResult.report()).isPresent();
    assertThat(decryptionValidationResult.report())
        .hasValue(
            FakeReportGenerator.generateWithFixedReportId(
                1,
                decryptionValidationResult
                    .report()
                    .get()
                    .sharedInfo()
                    .reportId()
                    .get(), /* reportVersion */
                ""));
  }

  @Test
  public void testDecryptionError() {
    fakeRecordDecrypter.setShouldThrow(true);

    DecryptionValidationResult decryptionValidationResult =
        reportDecrypterAndValidator.decryptAndValidate(encryptedReport, ctx);

    // Check that the report isn't present and that there is an ErrorMessage corresponding to a
    // decryption error
    assertThat(decryptionValidationResult.report()).isEmpty();
    assertThat(decryptionValidationResult.errorMessages().stream().map(ErrorMessage::category))
        .containsExactly(DECRYPTION_ERROR.name());
  }

  @Test
  public void testValidationError() {
    fakeValidator.setNextShouldReturnError(ImmutableList.of(true).iterator());

    DecryptionValidationResult decryptionValidationResult =
        reportDecrypterAndValidator.decryptAndValidate(encryptedReport, ctx);

    // Check that the report isn't present and that there is an ErrorMessage from the FakeValidator
    assertThat(decryptionValidationResult.report()).isEmpty();
    assertThat(decryptionValidationResult.errorMessages().stream().map(ErrorMessage::category))
        .containsExactly(GENERAL_ERROR.name());
  }

  public static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeRecordDecrypter.class).in(TestScoped.class);
      bind(RecordDecrypter.class).to(FakeRecordDecrypter.class);

      bind(FakeValidator.class).in(TestScoped.class);
      bind(ReportValidator.class).to(FakeValidator.class);

      Multibinder<ReportValidator> reportValidatorMultibinder =
          Multibinder.newSetBinder(binder(), ReportValidator.class);
      reportValidatorMultibinder.addBinding().to(FakeValidator.class);
    }
  }
}
