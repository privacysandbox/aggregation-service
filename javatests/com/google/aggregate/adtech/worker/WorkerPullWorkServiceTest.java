/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.OutputShardFileSizeBytes;
import com.google.aggregate.adtech.worker.selector.MetricClientSelector;
import com.google.aggregate.adtech.worker.testing.NoopJobProcessor;
import com.google.aggregate.adtech.worker.testing.NoopJobProcessor.ExceptionToThrow;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.privacysandbox.otel.OtlpJsonLoggingOTelConfigurationModule;
import com.google.scp.operator.cpio.jobclient.JobClient;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.jobclient.testing.ConstantJobClient;
import com.google.scp.operator.protos.shared.backend.JobKeyProto.JobKey;
import com.google.scp.operator.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.scp.operator.protos.shared.backend.ResultInfoProto.ResultInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class WorkerPullWorkServiceTest {

  private static final String RETURN_CODE_SUCCESS = AggregationWorkerReturnCode.SUCCESS.name();
  private static final String RETURN_CODE_INVALID_JOB =
      AggregationWorkerReturnCode.INVALID_JOB.name();
  @Inject private ConstantJobClient jobClient;
  @Inject private NoopJobProcessor jobProcessor;
  @Rule public final Acai acai = new Acai(TestEnv.class);
  private ArgumentCaptor<JobResult> jobResultCaptor;
  private ArgumentCaptor<JobKey> jobKeyCaptor;

  // Under Test
  @Inject private WorkerPullWorkService service;

  @Before
  public void beforeEach() {
    reset(jobClient);
    jobProcessor.setShouldThrowException(ExceptionToThrow.None);
    jobResultCaptor = ArgumentCaptor.forClass(JobResult.class);
    jobKeyCaptor = ArgumentCaptor.forClass(JobKey.class);
  }

  @Test
  public void pullJob() throws Exception {
    Job job = createJob("test job");
    jobClient.setReturnConstant(job);
    jobProcessor.setJobResultToReturn(createJobResult(job, RETURN_CODE_SUCCESS));

    service.run();

    verify(jobClient).markJobCompleted(jobResultCaptor.capture());
    assertThat(jobResultCaptor.getAllValues()).hasSize(1);
    assertThat(jobResultCaptor.getValue().resultInfo().getReturnCode())
        .isEqualTo(RETURN_CODE_SUCCESS);
  }

  @Test
  public void withInvalidFilteringIds_returnsInvalidJobCode() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.getDefaultInstance().toBuilder()
            .putJobParameters("attribution_report_to", "https://foo.com")
            .putJobParameters(JobUtils.JOB_PARAM_FILTERING_IDS, "5,6,null")
            .build();
    Job job = createJob("test job").toBuilder().setRequestInfo(requestInfo).build();
    jobClient.setReturnConstant(job);

    service.run();

    verify(jobClient).markJobCompleted(jobResultCaptor.capture());
    assertThat(jobResultCaptor.getAllValues()).hasSize(1);
    assertThat(jobResultCaptor.getValue().resultInfo().getReturnCode())
        .isEqualTo(AggregationWorkerReturnCode.INVALID_JOB.name());
  }

  @Test
  public void withValidFilteringIds_processingSucceeds() throws Exception {
    RequestInfo requestInfo =
        RequestInfo.getDefaultInstance().toBuilder()
            .putJobParameters("attribution_report_to", "https://foo.com")
            .putJobParameters(JobUtils.JOB_PARAM_FILTERING_IDS, " ,5,6, ,, 67, ")
            .build();
    Job job = createJob("test job").toBuilder().setRequestInfo(requestInfo).build();
    jobClient.setReturnConstant(job);
    jobProcessor.setJobResultToReturn(createJobResult(job, RETURN_CODE_SUCCESS));

    service.run();

    verify(jobClient).markJobCompleted(jobResultCaptor.capture());
    assertThat(jobResultCaptor.getAllValues()).hasSize(1);
    assertThat(jobResultCaptor.getValue().resultInfo().getReturnCode())
        .isEqualTo(RETURN_CODE_SUCCESS);
  }

  @Test
  public void pullJob_invalid() throws Exception {
    Job invalidJob = createInvalidJob("test job");
    jobClient.setReturnConstant(invalidJob);
    jobProcessor.setJobResultToReturn(createJobResult(invalidJob, RETURN_CODE_INVALID_JOB));

    service.run();

    verify(jobClient).markJobCompleted(jobResultCaptor.capture());
    assertThat(jobResultCaptor.getAllValues()).hasSize(1);
    assertThat(jobResultCaptor.getValue().resultInfo().getReturnCode())
        .isEqualTo(RETURN_CODE_INVALID_JOB);
  }

  @Test
  public void pullJob_throwsAggregateJobProcessException() throws Exception {
    jobClient.setReturnConstant(createJob("test job"));
    jobProcessor.setShouldThrowException(ExceptionToThrow.AggregationJobProcess);

    service.run();

    verify(jobClient).markJobCompleted(jobResultCaptor.capture());
    assertThat(jobResultCaptor.getAllValues()).hasSize(1);
    assertThat(jobResultCaptor.getValue().resultInfo().getReturnCode())
        .isEqualTo(RETURN_CODE_INVALID_JOB);
  }

  @Test
  public void pullJob_throwsException() throws Exception {
    Job job = createJob("test job");
    jobClient.setReturnConstant(job);
    jobProcessor.setShouldThrowException(ExceptionToThrow.Interrupted);

    service.run();

    verify(jobClient, never()).markJobCompleted(any());
    verify(jobClient).appendJobErrorMessage(jobKeyCaptor.capture(), anyString());
    assertThat(jobKeyCaptor.getValue()).isEqualTo(job.jobKey());
  }

  @Test
  public void pullJob_throwsAggregateJobProcessException_thenThrowsException() throws Exception {
    Job invalidJob = createJob("test job");
    jobClient.setReturnConstant(invalidJob);
    jobProcessor.setShouldThrowException(ExceptionToThrow.AggregationJobProcess);
    jobClient.setShouldThrowOnMarkJobCompleted(true);

    service.run();

    verify(jobClient).appendJobErrorMessage(jobKeyCaptor.capture(), anyString());
    assertThat(jobKeyCaptor.getValue()).isEqualTo(invalidJob.jobKey());
  }

  @Test
  public void pullJob_throwsValidationException_thenThrowsException() throws Exception {
    Job invalidJob = createInvalidJob("test job");
    jobClient.setReturnConstant(invalidJob);
    jobClient.setShouldThrowOnMarkJobCompleted(true);

    service.run();

    verify(jobClient).appendJobErrorMessage(jobKeyCaptor.capture(), anyString());
    assertThat(jobKeyCaptor.getValue()).isEqualTo(invalidJob.jobKey());
  }

  @Test
  public void pullJob_throwsException_thenThrowsException() throws Exception {
    jobClient.setReturnConstant(createJob("test job"));
    jobProcessor.setShouldThrowException(ExceptionToThrow.Interrupted);
    jobClient.setShouldThrowOnAppendJobErrorMessage(true);

    service.run();

    verify(jobClient).appendJobErrorMessage(any(), anyString());
    verify(jobClient, never()).markJobCompleted(any());
  }

  private Job createJob(String id) {
    return Job.builder()
        .setJobKey(JobKey.newBuilder().setJobRequestId(id).build())
        .setRequestInfo(
            RequestInfo.getDefaultInstance().toBuilder()
                .putJobParameters("attribution_report_to", "https://foo.com")
                .build())
        .setJobProcessingTimeout(Duration.ofSeconds(3600))
        .setCreateTime(Instant.now())
        .setUpdateTime(Instant.now().plusSeconds(1))
        .setProcessingStartTime(Optional.of(Instant.now().plusSeconds(2)))
        .setJobStatus(JobStatus.IN_PROGRESS)
        .setNumAttempts(0)
        .build();
  }

  private Job createInvalidJob(String id) {
    // RequestInfo.defaultInstance() made invalid by not containing attribution_report_to parameter
    return Job.builder()
        .setJobKey(JobKey.newBuilder().setJobRequestId(id).build())
        .setRequestInfo(RequestInfo.getDefaultInstance())
        .setJobProcessingTimeout(Duration.ofSeconds(3600))
        .setCreateTime(Instant.now())
        .setUpdateTime(Instant.now().plusSeconds(1))
        .setProcessingStartTime(Optional.of(Instant.now().plusSeconds(2)))
        .setJobStatus(JobStatus.IN_PROGRESS)
        .setNumAttempts(0)
        .build();
  }

  private JobResult createJobResult(Job job, String returnCode) {
    return JobResult.builder()
        .setJobKey(job.jobKey())
        .setResultInfo(
            ResultInfo.getDefaultInstance().toBuilder().setReturnCode(returnCode).build())
        .build();
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      // JobClient
      ConstantJobClient myJobClient = spy(new ConstantJobClient());
      bind(ConstantJobClient.class).toInstance(myJobClient);
      bind(JobClient.class).to(ConstantJobClient.class);

      // Job Processor
      bind(NoopJobProcessor.class).in(TestScoped.class);
      bind(JobProcessor.class).to(NoopJobProcessor.class);

      // JobResultHelper
      bind(Boolean.class).annotatedWith(EnableStackTraceInResponse.class).toInstance(true);
      bind(Integer.class).annotatedWith(MaxDepthOfStackTrace.class).toInstance(3);

      // MetricClient
      install(MetricClientSelector.LOCAL.getMetricModule());

      // Stopwatch Exporter
      bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);

      // OutputShardFileSizeBytes
      bind(Long.class).annotatedWith(OutputShardFileSizeBytes.class).toInstance(4096l);

      // Domain Optional
      bind(Boolean.class).annotatedWith(DomainOptional.class).toInstance(true);
      bind(Boolean.class).annotatedWith(BenchmarkMode.class).toInstance(true);
      bind(Boolean.class).annotatedWith(EnableThresholding.class).toInstance(true);

      // Otel collector
      install(new OtlpJsonLoggingOTelConfigurationModule());
      bind(Boolean.class).annotatedWith(EnableStackTraceInResponse.class).toInstance(true);
      bind(Integer.class).annotatedWith(MaxDepthOfStackTrace.class).toInstance(3);
    }

    // Used by JobResultHelper in WorkerPullWorkService
    @Provides
    Clock provideClock() {
      return Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    // Used by StopwatchRegistry in WorkerPullWorkService
    @Provides
    Ticker provideTimingTicker() {
      return Ticker.systemTicker();
    }

    @Provides
    @NonBlockingThreadPool
    ListeningExecutorService provideNonBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return newDirectExecutorService();
    }
  }
}
