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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.common.util.concurrent.Service.State.TERMINATED;

import com.google.aggregate.adtech.worker.Annotations.BenchmarkMode;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableStackTraceInResponse;
import com.google.aggregate.adtech.worker.Annotations.InstanceId;
import com.google.aggregate.adtech.worker.Annotations.MaxDepthOfStackTrace;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.OutputShardFileSizeBytes;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.testing.FakeJobResultGenerator;
import com.google.aggregate.adtech.worker.testing.NoopJobProcessor;
import com.google.aggregate.adtech.worker.testing.NoopJobProcessor.ExceptionToThrow;
import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.privacysandbox.otel.Annotations.EnableOTelLogs;
import com.google.privacysandbox.otel.OTelConfiguration;
import com.google.privacysandbox.otel.OtlpJsonLoggingOTelConfigurationModule;
import com.google.scp.operator.cpio.jobclient.JobClient;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.model.JobResult;
import com.google.scp.operator.cpio.jobclient.testing.ConstantJobClient;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.cpio.jobclient.testing.OneTimePullBackoff;
import com.google.scp.operator.cpio.metricclient.MetricClient;
import com.google.scp.operator.cpio.metricclient.local.LocalMetricClient;
import java.util.function.Supplier;
import javax.inject.Singleton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AggregationWorkerTest {

  private AggregationWorker worker;
  private ServiceManager serviceManager;

  private ConstantJobClient jobClient;
  private NoopJobProcessor processor;

  private BenchmarkFlagSupplier benchmarkFlagSupplier;

  @Before
  public void setUp() {
    OTelConfiguration.resetForTest();
    worker = AggregationWorker.fromModule(new TestEnv());
    serviceManager = worker.createServiceManager();

    // The components below are instantiated as singletons in this test, so the injector will return
    // the same objects.
    Injector injector = worker.getInjector();
    jobClient = injector.getInstance(ConstantJobClient.class);
    processor = injector.getInstance(NoopJobProcessor.class);
    benchmarkFlagSupplier = injector.getInstance(BenchmarkFlagSupplier.class);
  }

  @Test
  public void generic() {
    Job item = FakeJobGenerator.generate("foo");
    JobResult jobResult = FakeJobResultGenerator.fromJob(item);

    jobClient.setReturnConstant(item);
    processor.setJobResultToReturn(jobResult);

    runWorker();

    // Check that the item was called to be processed and completed
    assertThat(processor.getLastProcessed()).isPresent();
    assertThat(processor.getLastProcessed().get()).isEqualTo(item);
    assertThat(jobClient.getLastJobResultCompleted()).isEqualTo(jobResult);
    // Ensure that the one and only service has completed normally (TERMINATED).
    assertThat(serviceManager.servicesByState().keySet()).containsExactly(TERMINATED);
  }

  @Test
  public void generic_withBenchmarkMode() {
    Job item = FakeJobGenerator.generate("foo");
    JobResult jobResult = FakeJobResultGenerator.fromJob(item);
    // Set benchmarkMode=true
    benchmarkFlagSupplier.setIsBenchmark();
    jobClient.setReturnConstant(item);
    processor.setJobResultToReturn(jobResult);

    runWorker();

    // Check that the item was called to be processed and completed
    assertThat(processor.getLastProcessed()).isPresent();
    assertThat(processor.getLastProcessed().get()).isEqualTo(item);
    assertThat(jobClient.getLastJobResultCompleted()).isEqualTo(jobResult);
    // Ensure that the one and only service has completed normally (TERMINATED).
    assertThat(serviceManager.servicesByState().keySet()).containsExactly(TERMINATED);
  }

  @Test
  public void jobClientGetJobThrowing() {
    // Set the JobClient to throw on first call and return empty on second call
    jobClient.setShouldThrowOnGetJob(true);
    jobClient.setReturnEmpty();

    runWorker();

    // Ensure that no item was processed
    assertThat(processor.getLastProcessed()).isEmpty();
    assertThat(jobClient.getLastJobResultCompleted()).isNull();
    // Ensure that the has completed normally (TERMINATED) despite the exception
    assertThat(serviceManager.servicesByState().keySet()).containsExactly(TERMINATED);
  }

  @Test
  public void jobClientMarkJobCompletedThrowing() {
    // Set the JobClient to return an item on the first call but to throw an exception when
    // markJobCompleted is called
    Job Job = FakeJobGenerator.generate("foo");
    jobClient.setReturnConstant(Job);
    jobClient.setShouldThrowOnMarkJobCompleted(true);

    runWorker();

    // Check that the item was passed to the processor
    assertThat(processor.getLastProcessed()).isPresent();
    assertThat(processor.getLastProcessed()).hasValue(Job);
    // Ensure that the has completed normally (TERMINATED) despite the exception
    assertThat(serviceManager.servicesByState().keySet()).containsExactly(TERMINATED);
  }

  @Test
  public void processorThrowing() {
    Job item = FakeJobGenerator.generate("foo");
    jobClient.setReturnConstant(item);
    processor.setShouldThrowException(ExceptionToThrow.IllegalState);

    runWorker();

    // Check that the jobClient didn't have the job reported as complete
    assertThat(jobClient.getLastJobResultCompleted()).isNull();
    // Ensure that the has completed normally (TERMINATED) despite the exception
    assertThat(serviceManager.servicesByState().keySet()).containsExactly(TERMINATED);
  }

  private void runWorker() {
    serviceManager.startAsync();
    serviceManager.awaitStopped();
  }

  static final class BenchmarkFlagSupplier implements Supplier<Boolean> {

    private boolean isBenchmark;

    BenchmarkFlagSupplier() {
      isBenchmark = false;
    }

    void setIsBenchmark() {
      isBenchmark = true;
    }

    @Override
    public Boolean get() {
      return isBenchmark;
    }
  }

  private static class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new WorkerModule());
      install(new OtlpJsonLoggingOTelConfigurationModule());
      bind(boolean.class).annotatedWith(EnableOTelLogs.class).toInstance(false);
      bind(String.class).annotatedWith(InstanceId.class).toInstance("");

      bind(ConstantJobClient.class).in(Singleton.class);
      bind(OneTimePullBackoff.class).in(Singleton.class);
      bind(NoopJobProcessor.class).in(Singleton.class);
      bind(JobClient.class).to(ConstantJobClient.class);
      bind(JobProcessor.class).to(NoopJobProcessor.class);
      bind(StopwatchExporter.class).to(NoOpStopwatchExporter.class);
      bind(BenchmarkFlagSupplier.class).in(Singleton.class);
      bind(MetricClient.class).to(LocalMetricClient.class);

      // Privacy parameters
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);

      // Response related flags
      bind(boolean.class).annotatedWith(EnableStackTraceInResponse.class).toInstance(true);
      bind(int.class).annotatedWith(MaxDepthOfStackTrace.class).toInstance(32);
      bind(boolean.class).annotatedWith(DomainOptional.class).toInstance(true);
      bind(long.class).annotatedWith(OutputShardFileSizeBytes.class).toInstance(360L);
    }

    @Provides
    @BenchmarkMode
    boolean isBenchmark(BenchmarkFlagSupplier flagSupplier) {
      return flagSupplier.get();
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
