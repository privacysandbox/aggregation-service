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

package com.google.aggregate.adtech.worker.frontend.tasks.gcp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DUPLICATE_JOB_MESSAGE;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;
import static com.google.scp.shared.api.exception.testing.ServiceExceptionAssertions.assertThatServiceExceptionMatches;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTask;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.adtech.worker.frontend.testing.FakeRequestInfoValidator;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.testing.FakeJobQueue;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.FakeMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.JobGenerator;
import com.google.aggregate.adtech.worker.shared.testing.FakeClock;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GcpCreateJobTaskTest {

  static final String jobRequestId = "123";
  @Rule public Acai acai = new Acai(TestEnv.class);

  CreateJobTask createJobTask;
  @Inject JobQueue jobQueue;
  @Inject JobMetadataDb jobMetadataDb;
  @Inject FakeRequestInfoValidator fakeRequestInfoValidator;
  @Inject Clock clock;

  RequestInfo requestInfo;
  JobMetadata jobMetadata;
  FakeMetadataDb fakeMetadataDb;
  FakeJobQueue fakeJobQueue;
  Instant clockTime;

  @Before
  public void setup() {
    Integer ttlDays = 365;
    createJobTask =
        new GcpCreateJobTask(
            jobMetadataDb, jobQueue, clock, Set.of(fakeRequestInfoValidator), ttlDays);
    clockTime = clock.instant();
    Long ttlSec = clockTime.plus(ttlDays, ChronoUnit.DAYS).getEpochSecond();
    requestInfo = JobGenerator.createFakeRequestInfo(jobRequestId);
    jobMetadata =
        JobGenerator.createFakeJobMetadata(jobRequestId).toBuilder()
            .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(clockTime))
            .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(clockTime))
            .setJobStatus(RECEIVED)
            .clearRecordVersion()
            .clearResultInfo()
            .clearCreateJobRequest()
            .setTtl(ttlSec)
            .build();
    fakeMetadataDb = (FakeMetadataDb) jobMetadataDb;
    fakeJobQueue = (FakeJobQueue) jobQueue;
    fakeMetadataDb.reset();
  }

  /** Test for scenario to insert a job with no failures */
  @Test
  public void createJob_createsJobInDb() throws Exception {
    createJobTask.createJob(requestInfo);
    JobMetadata created = fakeMetadataDb.getLastJobMetadataInserted();
    // No assertion on the CreateJobResponse that is returned since it has no fields to check
    assertThat(created)
        .isEqualTo(jobMetadata.toBuilder().setServerJobId(created.getServerJobId()).build());
    assertThat(fakeJobQueue.getLastJobKeySent().getJobRequestId()).isEqualTo(jobRequestId);
  }

  /** Test for scenario to insert a job with a validation failure */
  @Test
  public void createJob_throwsValidationFailure_whenInvalid() {
    String validationErrorMessage = "Oh no a validation failure";
    fakeRequestInfoValidator.setValidateReturnValue(Optional.of(validationErrorMessage));

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> createJobTask.createJob(requestInfo));

    ServiceException expectedServiceException =
        new ServiceException(
            Code.INVALID_ARGUMENT,
            ErrorReasons.VALIDATION_FAILED.toString(),
            validationErrorMessage);
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);

    // Verify that queue and db never received a job
    assertThat(fakeMetadataDb.getLastJobMetadataInserted()).isNull();
    assertThat(fakeJobQueue.getLastJobKeySent()).isNull();
  }

  /** Test for scenario to insert a job where the JobKey is taken */
  @Test
  public void createJob_throwsException_whenKeyIsDuplicateInDb() {
    fakeMetadataDb.setShouldThrowJobKeyExistsException(true);

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> createJobTask.createJob(requestInfo));

    ServiceException expectedServiceException =
        new ServiceException(
            Code.ALREADY_EXISTS,
            ErrorReasons.DUPLICATE_JOB_KEY.toString(),
            String.format(DUPLICATE_JOB_MESSAGE, requestInfo.getJobRequestId()));
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);

    // Verify that only the queue has a job entry
    assertThat(fakeMetadataDb.getLastJobMetadataInserted()).isNull();
    assertThat(fakeJobQueue.getLastJobKeySent().getJobRequestId()).isEqualTo(jobRequestId);
  }

  /** Test for scenario to insert a job with some internal error in db */
  @Test
  public void createJob_throwsException_whenDbThrowsException() {
    fakeMetadataDb.setShouldThrowJobMetadataDbException(true);

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> createJobTask.createJob(requestInfo));

    ServiceException expectedServiceException =
        new ServiceException(Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE);
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);

    // The initial check for job metadata will fail so nothing is queued or inserted
    assertThat(fakeMetadataDb.getLastJobMetadataInserted()).isNull();
    assertThat(fakeJobQueue.getLastJobKeySent()).isNull();
  }

  /** Test for scenario to insert a job with some internal error in queue */
  @Test
  public void createJob_throwsException_whenQueueThrowsException() {
    fakeJobQueue.setShouldThrowException(true);

    ServiceException serviceException =
        assertThrows(ServiceException.class, () -> createJobTask.createJob(requestInfo));

    ServiceException expectedServiceException =
        new ServiceException(Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE);
    assertThatServiceExceptionMatches(serviceException, expectedServiceException);

    // Verify that queue and db never received a job
    assertThat(fakeMetadataDb.getLastJobMetadataInserted()).isNull();
    assertThat(fakeJobQueue.getLastJobKeySent()).isNull();
  }

  static class TestEnv extends AbstractModule {
    @Override
    public void configure() {
      bind(Clock.class).to(FakeClock.class).in(Singleton.class);
      bind(JobQueue.class).to(FakeJobQueue.class).in(TestScoped.class);
      bind(JobMetadataDb.class).to(FakeMetadataDb.class).in(TestScoped.class);
      bind(RequestInfoValidator.class).to(FakeRequestInfoValidator.class).in(TestScoped.class);
    }
  }
}
