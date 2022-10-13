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

import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.Report;

/**
 * Interface for decrypting a stream of encrypted reports,
 *
 * <p>Implementing classes can either exist for a single decryption operation or remain in place for
 * a series of decryption operations, based on the decryption scheme in use.
 */
public interface RecordDecrypter {

  /**
   * Decrypts and deserializes a single report. This is intended as a function that can be mapped
   * onto a series of EncryptedReports.
   */
  Report decryptSingleReport(EncryptedReport encryptedReport) throws DecryptionException;

  class DecryptionException extends Exception {

    public DecryptionException(Throwable cause) {
      super(cause);
    }
  }
}
