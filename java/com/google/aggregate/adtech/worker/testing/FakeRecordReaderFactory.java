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

import com.google.aggregate.adtech.worker.RecordReader;
import com.google.aggregate.adtech.worker.RecordReader.RecordReadException;
import com.google.aggregate.adtech.worker.RecordReaderFactory;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import java.util.stream.Stream;

public final class FakeRecordReaderFactory implements RecordReaderFactory {

  private ImmutableList<EncryptedReport> reportsToReturn;
  private boolean shouldThrow;
  private FakeRecordReader recordReader;

  @Override
  public RecordReader of(DataLocation dataLocation) throws RecordReadException {
    recordReader = new FakeRecordReader(reportsToReturn, shouldThrow);
    return recordReader;
  }

  public void setShouldThrow(boolean shouldThrowOnRead) {
    shouldThrow = shouldThrowOnRead;
  }

  public void setReportsToReturn(ImmutableList<EncryptedReport> reportsToBeReturned) {
    reportsToReturn = reportsToBeReturned;
  }

  public FakeRecordReader getRecordReader() {
    return recordReader;
  }

  /** Fake record reader that either streams from a hardcoded list or throws an exception */
  public static final class FakeRecordReader implements RecordReader {

    private final ImmutableList<EncryptedReport> reportsToReturn;
    private boolean shouldThrow;

    private int lastNumberOfReportsRead;

    public FakeRecordReader(ImmutableList<EncryptedReport> reportsToReturn, boolean shouldThrow) {
      this.reportsToReturn = reportsToReturn;
      this.shouldThrow = shouldThrow;
      lastNumberOfReportsRead = 0;
    }

    @Override
    public Stream<EncryptedReport> readEncryptedReports(DataLocation dataLocation)
        throws RecordReadException {
      if (shouldThrow) {
        throw new RecordReadException(new IllegalStateException("The reader was set to throw."));
      }

      lastNumberOfReportsRead = reportsToReturn.size();
      return reportsToReturn.stream();
    }

    public int getLastNumberOfReportsRead() {
      return lastNumberOfReportsRead;
    }

    @Override
    public void close() {
      shouldThrow = true;
    }
  }
}
