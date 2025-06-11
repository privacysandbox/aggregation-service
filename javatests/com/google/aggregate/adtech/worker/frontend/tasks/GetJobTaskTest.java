/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.tasks;

import static com.google.common.truth.Truth.assertThat;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.JOB_NOT_FOUND_MESSAGE;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;
import static com.google.scp.shared.api.exception.testing.ServiceExceptionAssertions.assertThatServiceExceptionMatches;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.injection.modules.testing.FakeFrontendModule;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.FakeMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.JobGenerator;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GetJobTaskTest {

  static final String jobRequestId = "123";
  static final String attributionReportTo = "bar.com";

  @Rule public Acai acai = new Acai(TestEnv.class);
  // Under test
  @Inject GetJobTask getJobTask;
  @Inject FakeMetadataDb fakeMetadataDb;
  @Inject Clock clock;

  JobMetadata jobMetadata;
  JobKey jobKey;
  Instant clockTime;

  @Before
  public void setUp() {
    clockTime = clock.instant();
    jobKey = JobKey.newBuilder().setJobRequestId(jobRequestId).build();
    jobMetadata =
        JobGenerator.createFakeJobMetadata(jobRequestId).toBuilder()
            .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(clockTime))
            .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(clockTime))
            .setRequestProcessingStartedAt(ProtoUtil.toProtoTimestamp(clockTime))
            .setJobStatus(RECEIVED)
            .clearRecordVersion()
            .clearResultInfo()
            .build();
    fakeMetadataDb.reset();
  }

  /** Test for scenario to get a job that exists with no failures */
  @Test
  public void getJob_returnsJob() throws Exception {
    fakeMetadataDb.setJobMetadataToReturn(Optional.of(this.jobMetadata));

    JobMetadata jobMetadataResult = getJobTask.getJob(jobRequestId);

    assertThat(jobMetadataResult).isEqualTo(jobMetadata);
    assertThat(fakeMetadataDb.getLastJobKeyStringLookedUp()).isEqualTo(jobKey.getJobRequestId());
  }

  /** Test for scenario to get a job that does not exist with no failures */
  @Test
  public void getJob_throwsException_whenJobDoesNotExist() {
    fakeMetadataDb.setJobMetadataToReturn(Optional.empty());

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> getJobTask.getJob(jobRequestId));

    ServiceException expectedServiceException =
        new ServiceException(
            Code.NOT_FOUND,
            ErrorReasons.JOB_NOT_FOUND.toString(),
            String.format(JOB_NOT_FOUND_MESSAGE, jobRequestId));
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);
    assertThat(fakeMetadataDb.getLastJobKeyStringLookedUp()).isEqualTo(jobKey.getJobRequestId());
  }

  /** Test for scenario to get a job with some internal error happening */
  @Test
  public void getJob_throwsException_whenDbThrowsException() {
    fakeMetadataDb.setShouldThrowJobMetadataDbException(true);

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> getJobTask.getJob(jobRequestId));

    ServiceException expectedServiceException =
        new ServiceException(Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE);
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);
  }

  static class TestEnv extends AbstractModule {

    @Override
    public void configure() {
      install(new FakeFrontendModule());
    }
  }
}
