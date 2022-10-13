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

import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import java.util.stream.Stream;

/** Reader interface for reading the encrypted records based on the job context. */
public interface RecordReader extends AutoCloseable {

  Stream<EncryptedReport> readEncryptedReports(DataLocation dataLocation)
      throws RecordReadException;

  @Override
  void close() throws RecordReadException;

  final class RecordReadException extends Exception {

    public RecordReadException(Throwable cause) {
      super(cause);
    }

    public static class UncheckedRecordReadException extends RuntimeException {

      public UncheckedRecordReadException(RecordReadException cause) {
        super(cause);
      }
    }
  }
}
