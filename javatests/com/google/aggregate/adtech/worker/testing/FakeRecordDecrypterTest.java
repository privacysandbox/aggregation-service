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

import static com.google.aggregate.adtech.worker.testing.FakeReportGenerator.generateWithFixedReportId;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.decryption.RecordDecrypter.DecryptionException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.common.io.ByteSource;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeRecordDecrypterTest {

  private Job ctx;

  // Under test
  FakeRecordDecrypter decrypter;

  @Before
  public void setUp() {
    decrypter = new FakeRecordDecrypter();
    ctx = FakeJobGenerator.generate("foo");
  }

  @Test
  public void throwsWhenInstructed() {
    decrypter.setShouldThrow(true);

    assertThrows(DecryptionException.class, () -> decrypter.decryptSingleReport(null));
  }

  @Test
  public void fakeDecryption() throws Exception {
    decrypter.setIdToGenerate(2);
    ByteSource reportBytes = ByteSource.wrap(new byte[] {0x02, 0x03});
    EncryptedReport encryptedReport =
        EncryptedReport.builder().setPayload(reportBytes).setKeyId("").setSharedInfo("").build();

    Report report = decrypter.decryptSingleReport(encryptedReport);

    assertThat(report)
        .isEqualTo(
            generateWithFixedReportId(
                2, report.sharedInfo().reportId().get(), /* reportVersion */ ""));
  }
}
