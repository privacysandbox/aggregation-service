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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.getDataLocation;
import static org.junit.Assert.assertThrows;

import com.google.aggregate.adtech.worker.RecordReader.RecordReadException;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeRecordReaderFactoryTest {

  private Job ctx;
  private DataLocation inputLocation;

  // Under test
  private FakeRecordReaderFactory factory;

  @Before
  public void setUp() {
    factory = new FakeRecordReaderFactory();
    ctx = FakeJobGenerator.generate("foo");
    inputLocation =
        getDataLocation(
            ctx.requestInfo().getInputDataBucketName(), ctx.requestInfo().getInputDataBlobPrefix());
  }

  @Test
  public void throwsWhenInstructed() throws RecordReadException {
    factory.setShouldThrow(true);
    factory.of(inputLocation);

    assertThrows(
        RecordReadException.class,
        () -> factory.getRecordReader().readEncryptedReports(inputLocation));
  }

  @Test
  public void returnsHardcoded() throws Exception {
    ByteSource firstReportBytes = ByteSource.wrap(new byte[] {0x00, 0x01});
    ByteSource secondReportBytes = ByteSource.wrap(new byte[] {0x02, 0x03});
    EncryptedReport firstReport =
        EncryptedReport.builder()
            .setPayload(firstReportBytes)
            .setKeyId("")
            .setSharedInfo("")
            .build();
    EncryptedReport secondReport =
        EncryptedReport.builder()
            .setPayload(secondReportBytes)
            .setKeyId("")
            .setSharedInfo("")
            .build();
    ImmutableList<EncryptedReport> reports = ImmutableList.of(firstReport, secondReport);
    factory.setReportsToReturn(reports);
    factory.of(inputLocation);

    Stream<EncryptedReport> fakeRead =
        factory.getRecordReader().readEncryptedReports(inputLocation);

    assertThat(fakeRead).containsExactlyElementsIn(reports);
    assertThat(factory.getRecordReader().getLastNumberOfReportsRead()).isEqualTo(reports.size());
  }

  @Test
  public void throwsWhenClosed() throws RecordReadException {
    factory.of(inputLocation).close();

    assertThrows(
        RecordReadException.class,
        () -> factory.getRecordReader().readEncryptedReports(inputLocation));
  }
}
