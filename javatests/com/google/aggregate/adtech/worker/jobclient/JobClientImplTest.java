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

package com.google.aggregate.adtech.worker.jobclient;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.protobuf.util.Durations;
import com.google.aggregate.adtech.worker.jobclient.JobClient.JobClientException;
import com.google.aggregate.adtech.worker.jobclient.JobHandlerModule.JobClientJobMaxNumAttemptsBinding;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.model.JobRetryRequest;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobResultGenerator;
import com.google.aggregate.adtech.worker.jobclient.testing.OneTimePullBackoff;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;
import com.google.aggregate.adtech.worker.lifecycleclient.local.LocalLifecycleClient;
import com.google.aggregate.adtech.worker.lifecycleclient.local.LocalLifecycleModule;
import com.google.scp.operator.cpio.metricclient.MetricClient;
import com.google.scp.operator.cpio.metricclient.local.LocalMetricClient;
import com.google.scp.operator.cpio.notificationclient.NotificationClient;
import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueException;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.testing.FakeJobQueue;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.FakeMetadataDb;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.local.Annotations.ParameterValues;
import com.google.scp.shared.clients.configclient.local.LocalParameterClient;
import com.google.scp.shared.proto.ProtoUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class JobClientImplTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  // Under test
  @Inject Clock clock;
  @Inject
  JobClientImpl jobClient;
  @Inject FakeJobQueue jobQueue;
  @Inject FakeMetadataDb jobMetadataDb;
  @Inject Optional<NotificationClient> notificationClient;
  @Inject ParameterClient parameterClient;

  private JobQueueItem baseJobQueueItem;
  private JobMetadata baseJobMetadata;
  private JobRetryRequest baseJobRetryRequest;
  private Job baseJob;
  private Job expectedBaseJob;
  private RequestInfo requestInfo;

  private static final Instant requestReceivedAt = Instant.ofEpochSecond(100);
  private static final Instant requestUpdatedAt = Instant.ofEpochSecond(200);

  @Before
  public void setUp() {
    baseJobQueueItem =
        JobQueueItem.newBuilder()
            .setJobKeyString("request|abc.com")
            .setServerJobId("123")
            .setReceiptInfo("receipt")
            .setJobProcessingTimeout(Durations.fromSeconds(3600))
            .setJobProcessingStartTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .build();
    requestInfo =
        RequestInfo.newBuilder()
            .setJobRequestId("request")
            .setInputDataBlobPrefix("bar")
            .setInputDataBucketName("foo")
            .setOutputDataBlobPrefix("bar")
            .setOutputDataBucketName("foo")
            .setPostbackUrl("http://myUrl.com")
            .putAllJobParameters(
                ImmutableMap.of(
                    "attribution_report_to",
                    "abc.com",
                    "output_domain_blob_prefix",
                    "bar",
                    "output_domain_bucket_name",
                    "foo",
                    "debug_privacy_budget_limit",
                    "5"))
            .build();
    baseJobMetadata =
        JobMetadata.newBuilder()
            .setJobKey(JobKey.newBuilder().setJobRequestId("request").build())
            .setServerJobId("123")
            .setJobStatus(JobStatus.RECEIVED)
            .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(requestReceivedAt))
            .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(requestUpdatedAt))
            .setNumAttempts(0)
            .setCreateJobRequest(
                CreateJobRequest.newBuilder()
                    .setJobRequestId("request")
                    .setAttributionReportTo("abc.com")
                    .setInputDataBlobPrefix("bar")
                    .setInputDataBucketName("foo")
                    .setOutputDataBlobPrefix("bar")
                    .setOutputDataBucketName("foo")
                    .setOutputDomainBucketName("fizz")
                    .setOutputDomainBlobPrefix("buzz")
                    .setPostbackUrl("http://myUrl.com")
                    .setDebugPrivacyBudgetLimit(5)
                    .putAllJobParameters(
                        ImmutableMap.of(
                            "attribution_report_to",
                            "abc.com",
                            "output_domain_blob_prefix",
                            "bar",
                            "output_domain_bucket_name",
                            "foo",
                            "debug_privacy_budget_limit",
                            "5"))
                    .build())
            .setRequestInfo(requestInfo)
            .build();

    baseJob = FakeJobGenerator.generate("foo");
    expectedBaseJob =
        Job.builder()
            .setJobKey(baseJobMetadata.getJobKey())
            .setJobProcessingTimeout(
                ProtoUtil.toJavaDuration(baseJobQueueItem.getJobProcessingTimeout()))
            .setRequestInfo(requestInfo)
            .setCreateTime(requestReceivedAt)
            .setUpdateTime(requestUpdatedAt)
            .setNumAttempts(0)
            .setJobStatus(JobStatus.RECEIVED)
            .build();
  }

  @Test
  public void getJob_getsFakeJob() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));

    Optional<Job> actual = jobClient.getJob();

    assertThat(actual).isPresent();
    assertThat(actual.get()).isEqualTo(expectedBaseJob);
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getJobStatus())
        .isEqualTo(JobStatus.IN_PROGRESS);
    // Check the worker start process time also updates
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getRequestProcessingStartedAt())
        .isEqualTo(ProtoUtil.toProtoTimestamp(Instant.now(clock)));
  }

  @Test
  public void getJob_exhaustsRetry() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.empty());
    jobMetadataDb.setJobMetadataToReturn(Optional.empty());

    Optional<Job> actual = jobClient.getJob();

    assertThat(actual).isEmpty();
  }

  @Test
  public void getJob_ignoresJobWhenJobMetadataNotFound() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.empty());

    Optional<Job> actual = jobClient.getJob();

    // ignores the queue item because no metadata entry is found,
    // then exhausts puller backoff and returns empty.
    assertThat(actual).isEmpty();
    // make sure {@code acknowledgeJobCompletion} is called
    assertThat(jobQueue.getLastJobQueueItemSent()).isEqualTo(baseJobQueueItem);
  }

  @Test
  public void getJob_deleteMessageForServerJobIdMismatch()
      throws JobClientException, JobQueueException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(
        Optional.of(baseJobMetadata.toBuilder().setServerJobId("456").build()));

    Optional<Job> actual = jobClient.getJob();

    assertThat(actual).isEmpty();
    // make sure {@code acknowledgeJobCompletion} is called
    assertThat(jobQueue.getLastJobQueueItemSent()).isEqualTo(baseJobQueueItem);
    assertTrue(jobQueue.receiveJob().isEmpty());
  }

  @Test
  public void getJob_retriesWhenJobMetadataNotFound() throws JobClientException, JobQueueException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));
    jobMetadataDb.setInitialLookupFailureCount(5);

    Optional<Job> actual = jobClient.getJob();

    assertThat(actual).isPresent();
    assertThat(actual.get()).isEqualTo(expectedBaseJob);
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getJobStatus())
        .isEqualTo(JobStatus.IN_PROGRESS);
    // Check the worker start process time also updates when retry
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getRequestProcessingStartedAt())
        .isEqualTo(ProtoUtil.toProtoTimestamp(Instant.now(clock)));
  }

  @Test
  public void getJob_ignoresJobWhenStatusFinished() throws JobClientException, JobQueueException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(
        Optional.of(baseJobMetadata.toBuilder().setJobStatus(JobStatus.FINISHED).build()));

    Optional<Job> actual = jobClient.getJob();

    // ignores the queue item because the job is already finished,
    // then exhausts puller backoff and returns empty.
    assertThat(actual).isEmpty();
    // make sure {@code acknowledgeJobCompletion} is called
    assertThat(jobQueue.getLastJobQueueItemSent()).isEqualTo(baseJobQueueItem);
  }

  @Test
  public void getJob_marksJobWithExhaustedAttemptsAsFailed() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(
        Optional.of(
            baseJobMetadata.toBuilder()
                .setJobStatus(JobStatus.IN_PROGRESS)
                .setNumAttempts(2)
                .build()));

    Optional<Job> actual = jobClient.getJob();

    // ignores the queue item because the job has exhausted attempts,
    // then exhausts puller backoff and returns empty.
    assertThat(actual).isEmpty();
    // make sure job metadata updated
    ResultInfo resultInfo = jobMetadataDb.getLastJobMetadataUpdated().getResultInfo();
    assertThat(resultInfo.getFinishedAt())
        .isEqualTo(ProtoUtil.toProtoTimestamp(Instant.now(clock)));
    assertThat(resultInfo.getReturnMessage()).contains("Number of retry attempts exhausted");
    assertThat(resultInfo.getReturnCode()).isEqualTo(ReturnCode.RETRIES_EXHAUSTED.name());
    // make sure {@code acknowledgeJobCompletion} is called
    assertThat(jobQueue.getLastJobQueueItemSent()).isEqualTo(baseJobQueueItem);
  }

  @Test
  public void markJobCompleted_marksJobCompletion() throws Exception {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));

    Job job = jobClient.getJob().get();
    ResultInfo resultInfo =
        ResultInfo.newBuilder()
            .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.ofEpochSecond(1234)))
            .setReturnCode(ReturnCode.SUCCESS.name())
            .setReturnMessage("success")
            .setErrorSummary(ErrorSummary.getDefaultInstance())
            .build();
    JobResult result =
        JobResult.builder().setJobKey(job.jobKey()).setResultInfo(resultInfo).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(jobMetadataDb.getLastJobMetadataUpdated()));
    jobClient.markJobCompleted(result);

    JobMetadata expectedMetadata =
        baseJobMetadata.toBuilder()
            .setJobStatus(JobStatus.FINISHED)
            .setResultInfo(resultInfo)
            .setNumAttempts(1)
            .setRequestProcessingStartedAt(
                jobMetadataDb.getLastJobMetadataUpdated().getRequestProcessingStartedAt())
            .build();
    // make sure job metadata updated
    assertThat(jobMetadataDb.getLastJobMetadataUpdated()).isEqualTo(expectedMetadata);
    // make sure job is removed from the job queue
    assertThat(jobQueue.getLastJobQueueItemSent()).isEqualTo(baseJobQueueItem);
  }

  @Test
  public void markJobCompleted_throwsMetadataNotFound() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));

    Job job = jobClient.getJob().get();
    JobResult result = FakeJobResultGenerator.fromJob(job);
    jobMetadataDb.setJobMetadataToReturn(Optional.empty());

    assertThrows(JobClientException.class, () -> jobClient.markJobCompleted(result));
  }

  @Test
  public void markJobCompleted_throwsJobStatusNotInProgress() throws JobClientException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));

    Job job = jobClient.getJob().get();
    JobResult result =
        JobResult.builder()
            .setJobKey(job.jobKey())
            .setResultInfo(
                ResultInfo.newBuilder()
                    .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.ofEpochSecond(1234)))
                    .setReturnCode(ReturnCode.SUCCESS.name())
                    .setReturnMessage("success")
                    .setErrorSummary(
                        ErrorSummary.newBuilder()
                            .setNumReportsWithErrors(0)
                            .addAllErrorCounts(ImmutableList.of())
                            .build())
                    .build())
            .build();
    jobMetadataDb.setJobMetadataToReturn(
        Optional.of(baseJobMetadata.toBuilder().setJobStatus(JobStatus.FINISHED).build()));

    assertThrows(JobClientException.class, () -> jobClient.markJobCompleted(result));
  }

  @Test
  public void markJobCompleted_throwsJobNotInCache() {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));
    Job job = FakeJobGenerator.generate("request");
    JobResult result = FakeJobResultGenerator.fromJob(job);

    jobMetadataDb.setJobMetadataToReturn(
        Optional.of(baseJobMetadata.toBuilder().setJobStatus(JobStatus.FINISHED).build()));

    assertThrows(JobClientException.class, () -> jobClient.markJobCompleted(result));
  }

  @Test
  public void buildJob_buildsJob() throws JobClientException {
    // no setup

    Job actual = jobClient.buildJob(baseJobQueueItem, baseJobMetadata);

    assertThat(actual).isEqualTo(expectedBaseJob);
  }

  @Test
  public void isDuplicateJob_overTimeoutReturnsFalse() {
    Job job =
        baseJob.toBuilder()
            .setJobStatus(JobStatus.IN_PROGRESS)
            .setCreateTime(Instant.parse("2021-01-01T12:24:00Z"))
            .setUpdateTime(Instant.parse("2021-01-01T12:24:59Z"))
            .setJobProcessingTimeout(Duration.ofMinutes(5))
            .build();

    assertThat(jobClient.isDuplicateJob(Optional.of(job))).isEqualTo(false);
  }

  @Test
  public void isDuplicateJob_receivedStatusReturnsFalse() {
    Job job =
        baseJob.toBuilder()
            .setJobStatus(JobStatus.RECEIVED)
            .setCreateTime(Instant.parse("2021-01-01T12:28:00Z"))
            .setUpdateTime(Instant.parse("2021-01-01T12:29:00Z"))
            .setJobProcessingTimeout(Duration.ofMinutes(5))
            .build();

    assertThat(jobClient.isDuplicateJob(Optional.of(job))).isEqualTo(false);
  }

  @Test
  public void isDuplicateJob_duplicateReturnsTrue() {
    Job job =
        baseJob.toBuilder()
            .setJobStatus(JobStatus.IN_PROGRESS)
            .setCreateTime(Instant.parse("2021-01-01T12:28:00Z"))
            .setProcessingStartTime(Optional.of(Instant.parse("2021-01-01T12:28:00Z")))
            .setUpdateTime(Instant.parse("2021-01-01T12:29:00Z"))
            .setJobProcessingTimeout(Duration.ofMinutes(5))
            .build();

    assertThat(jobClient.isDuplicateJob(Optional.of(job))).isEqualTo(true);
  }

  @Test
  public void updateJobResultErrorSummary_throwsMetadataNotFound() throws JobClientException {
    jobMetadataDb.setJobMetadataToReturn(Optional.empty());

    ThrowingRunnable methodToTest =
        () -> jobClient.appendJobErrorMessage(JobKey.newBuilder().build(), "");

    assertThrows(JobClientException.class, methodToTest);
  }

  @Test
  public void updateJobResultErrorSummary_throwsNotInProgress() throws JobClientException {
    JobMetadata metadata = baseJobMetadata.toBuilder().setJobStatus(JobStatus.FINISHED).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(metadata));

    ThrowingRunnable methodToTest = () -> jobClient.appendJobErrorMessage(metadata.getJobKey(), "");

    assertThrows(JobClientException.class, methodToTest);
  }

  @Test
  public void updateJobResultErrorSummary_success()
      throws JobClientException, JobMetadataDbException {
    String sampleErrorMessage = "fake.error.message to put within the error summary";
    JobMetadata metadata = baseJobMetadata.toBuilder().setJobStatus(JobStatus.IN_PROGRESS).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(metadata));

    jobClient.appendJobErrorMessage(metadata.getJobKey(), sampleErrorMessage);

    assertThat(
            jobMetadataDb
                .getLastJobMetadataUpdated()
                .getResultInfo()
                .getErrorSummary()
                .getErrorMessages(0))
        .isEqualTo(sampleErrorMessage);
    assertThat(
            jobMetadataDb.getLastJobMetadataUpdated().getResultInfo().getFinishedAt().getSeconds())
        .isGreaterThan(0);
  }

  @Test
  public void updateJobResultErrorSummary_errorMessagesCorrectLength() throws JobClientException {
    int expectedErrorMessageListLength = 5;
    String sampleErrorMessage = "fake.error.message to put within the error summary";
    JobMetadata metadata = baseJobMetadata.toBuilder().setJobStatus(JobStatus.IN_PROGRESS).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(metadata));

    for (int i = 0; i < expectedErrorMessageListLength; i++) {
      jobClient.appendJobErrorMessage(metadata.getJobKey(), sampleErrorMessage + i);
      // Fake JobMetadataDB stores updated metadata in separate variable from what getJobMetadata()
      // returns, so it must be manually assigned back to it here
      jobMetadataDb.setJobMetadataToReturn(Optional.of(jobMetadataDb.getLastJobMetadataUpdated()));
    }

    assertThat(
            jobMetadataDb
                .getLastJobMetadataUpdated()
                .getResultInfo()
                .getErrorSummary()
                .getErrorMessagesList()
                .size())
        .isEqualTo(expectedErrorMessageListLength);
  }

  @Test
  public void updateJobResultErrorSummary_errorMessagesCorrectlySet() throws JobClientException {
    String[] sampleErrorMessages =
        new String[] {
          "fake.error.message 1",
          "fake.error.message 2",
          "my.fake.message 3",
          "fake message 4",
          "my fake message 5"
        };
    JobMetadata metadata = baseJobMetadata.toBuilder().setJobStatus(JobStatus.IN_PROGRESS).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(metadata));

    for (int i = 0; i < sampleErrorMessages.length; i++) {
      jobClient.appendJobErrorMessage(metadata.getJobKey(), sampleErrorMessages[i]);
      // Fake JobMetadataDB stores updated metadata in separate variable from what getJobMetadata()
      // returns, so it must be manually assigned back to it here
      jobMetadataDb.setJobMetadataToReturn(Optional.of(jobMetadataDb.getLastJobMetadataUpdated()));
    }
    List<String> actualErrorMessages =
        jobMetadataDb
            .getLastJobMetadataUpdated()
            .getResultInfo()
            .getErrorSummary()
            .getErrorMessagesList();

    for (int i = 0; i < sampleErrorMessages.length; i++) {
      assertThat(actualErrorMessages.get(i)).isEqualTo(sampleErrorMessages[i]);
    }
  }

  @Test
  public void returnJobForRetry_throwsNotInProgress() throws JobClientException {
    JobRetryRequest jobRetryRequest = JobRetryRequest.builder().setJobKey(baseJob.jobKey()).build();
    JobMetadata metadata = baseJobMetadata.toBuilder().setJobStatus(JobStatus.FINISHED).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.of(metadata));

    ThrowingRunnable methodToTest = () -> jobClient.returnJobForRetry(jobRetryRequest);
    assertThrows(JobClientException.class, methodToTest);
  }

  @Test
  public void returnJobForRetry_throwsMetadataNotFound() throws JobClientException {
    JobRetryRequest jobRetryRequest = JobRetryRequest.builder().setJobKey(baseJob.jobKey()).build();
    jobMetadataDb.setJobMetadataToReturn(Optional.empty());

    ThrowingRunnable methodToTest = () -> jobClient.returnJobForRetry(jobRetryRequest);
    assertThrows(JobClientException.class, methodToTest);
  }

  @Test
  public void returnJobForRetry_success() throws JobClientException, JobQueueException {
    jobQueue.setJobQueueItemToBeReceived(Optional.of(baseJobQueueItem));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(baseJobMetadata));
    Optional<Job> actual = jobClient.getJob();
    assertThat(actual).isPresent();
    assertThat(actual.get()).isEqualTo(expectedBaseJob);
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getJobStatus())
        .isEqualTo(JobStatus.IN_PROGRESS);
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getRequestProcessingStartedAt())
        .isEqualTo(ProtoUtil.toProtoTimestamp(Instant.now(clock)));
    jobMetadataDb.setJobMetadataToReturn(Optional.of(jobMetadataDb.getLastJobMetadataUpdated()));

    String sampleErrorMessage = "fake.error.message to put within the error summary";
    ErrorSummary updatedErrorSummary =
        ErrorSummary.newBuilder().addErrorMessages(sampleErrorMessage).build();
    Clock clock = Clock.systemUTC();
    ResultInfo newResultInfo =
        ResultInfo.newBuilder()
            .setErrorSummary(updatedErrorSummary)
            .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
            .setReturnCode(ReturnCode.RETURN_CODE_UNKNOWN.name())
            .setReturnMessage("retrying.")
            .build();
    JobRetryRequest jobRetryRequest =
        JobRetryRequest.builder()
            .setJobKey(actual.get().jobKey())
            .setDelay(Duration.ofSeconds(0))
            .setResultInfo(newResultInfo)
            .build();

    jobClient.returnJobForRetry(jobRetryRequest);
    assertThat(jobMetadataDb.getLastJobMetadataUpdated().getJobStatus())
        .isEqualTo(JobStatus.RECEIVED);
    assertThat(
            jobMetadataDb
                .getLastJobMetadataUpdated()
                .getResultInfo()
                .getErrorSummary()
                .getErrorMessages(0))
        .isEqualTo(sampleErrorMessage);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(FakeMetadataDb.class).in(TestScoped.class);
      bind(FakeJobQueue.class).in(TestScoped.class);
      bind(JobQueue.class).to(FakeJobQueue.class);
      bind(JobMetadataDb.class).to(FakeMetadataDb.class);
      bind(JobPullBackoff.class).to(OneTimePullBackoff.class);
      bind(Integer.class).annotatedWith(JobClientJobMaxNumAttemptsBinding.class).toInstance(1);
      install(new JobValidatorModule());
      install(new LocalLifecycleModule());
      bind(LifecycleClient.class).to(LocalLifecycleClient.class);
      bind(MetricClient.class).to(LocalMetricClient.class);
      bind(new TypeLiteral<ImmutableMap<String, String>>() {})
          .annotatedWith(ParameterValues.class)
          .toInstance(ImmutableMap.of());
      bind(ParameterClient.class).to(LocalParameterClient.class);
      OptionalBinder.newOptionalBinder(binder(), Key.get(NotificationClient.class));
    }

    @Provides
    @Singleton
    Clock provideClock() {
      return Clock.fixed(Instant.parse("2021-01-01T12:30:00Z"), ZoneId.systemDefault());
    }
  }
}
