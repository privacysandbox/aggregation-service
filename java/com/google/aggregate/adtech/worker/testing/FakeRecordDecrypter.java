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

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipherFactory.CipherCreationException;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;

/**
 * Fake record decrypter that either returns the decrypted key as the same bytes as the encrypted
 * payload or throws an exception if requested
 */
public final class FakeRecordDecrypter implements RecordDecrypter {

  private boolean shouldThrow;
  private ErrorReason throwReason;
  private int idToGenerate;

  public FakeRecordDecrypter() {
    shouldThrow = false;
    idToGenerate = 1;
  }

  /**
   * Provided report is completely ignored, instead returns the result of
   * FakeReportGenerator.generate(1) or throws an exception if requested.
   */
  @Override
  public Report decryptSingleReport(EncryptedReport unused) throws DecryptionException {
    if (shouldThrow) {
      shouldThrow = false;
      if (throwReason != null) {
        throw new DecryptionException(
            new CipherCreationException(
                new Exception("FakeRecordDecrypter test throw"), throwReason));
      } else {
        throw new DecryptionException(new IllegalStateException("The decrypter was set to throw."));
      }
    }

    return FakeReportGenerator.generateWithParam(idToGenerate, LATEST_VERSION, "https://foo.com");
  }

  public void setShouldThrow(boolean shouldThrow, ErrorReason reason) {
    this.shouldThrow = shouldThrow;
    this.throwReason = reason;
  }

  public void setIdToGenerate(int idToGenerate) {
    this.idToGenerate = idToGenerate;
  }
}
