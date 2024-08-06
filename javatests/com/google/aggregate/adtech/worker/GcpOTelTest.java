/*
 * Copyright 2024 Google LLC
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

import static com.google.aggregate.adtech.worker.SmokeTestBase.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.SmokeTestBase.checkJobExecutionResult;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;
import static com.google.aggregate.adtech.worker.SmokeTestBase.readResultsFromCloud;
import static com.google.aggregate.adtech.worker.SmokeTestBase.submitJobAndWaitForResult;
import static com.google.common.truth.Truth.assertThat;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceClient.ListTimeSeriesPagedResponse;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.common.collect.ImmutableList;
import com.google.devtools.cloudtrace.v1.ListTracesRequest;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ListTimeSeriesRequest.TimeSeriesView;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.util.Timestamps;
import com.google.scp.operator.cpio.blobstorageclient.gcp.GcsBlobStorageClient;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

/**
 * In GcpOTelTest, one job would be sent first to trigger metric/trace generation. And the following
 * tests are testing if OTel metrics and traces exist in Monitoring and traces. In continuous
 * environment, prod binary is used for OTel which would only export prod metrics. Use
 * FixMethodOrder for this class to ensure the job will be running first to generate metrics and
 * traces.
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class GcpOTelTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject GcsBlobStorageClient gcsBlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;
  public static final String OUTPUT_DATA_PREFIX_NAME = "-1-of-1";
  private static final Integer DEBUG_DOMAIN_KEY_SIZE = 1000;
  private final String jobId = getClass().getSimpleName();

  private static final ProjectName projectName = ProjectName.of("ps-msmt-aggserv-test");
  private static final String ENVIRONMENT_NAME = "continuous-mp";
  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  /*
  End-to-end test. Creates a job request for data already created in GCS. Asserts the job status and
  the size of summary report facts.
   */
  @Test
  public void createJobE2ETest() throws Exception {
    String inputDataPrefix = String.format("%s/test-inputs/otel_test_input.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/otel_test_domain.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format("%s/test-outputs/otel_test.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            jobId,
            Optional.of(getTestDataBucket()),
            Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);
    checkJobExecutionResult(result, SUCCESS.name(), 0);

    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    assertThat(aggregatedFacts.size()).isEqualTo(DEBUG_DOMAIN_KEY_SIZE);
  }

  @Test
  public void e2eCPUMetricTest() throws IOException {
    String metricName = "workload.googleapis.com/process.runtime.jvm.CPU.utilization";

    ListTimeSeriesPagedResponse response = listAllMetrics(metricName);
    int count = 0;

    for (TimeSeries ts : response.iterateAll()) {
      for (int i = 0; i < ts.getPointsCount(); i++) {
        assertThat(ts.getPoints(i).getValue().getDoubleValue() % 1).isEqualTo(0);
      }
      count += 1;
    }
    assertThat(count).isGreaterThan(0);
  }

  @Test
  public void e2eMemoryMetricTest() throws IOException {
    String metricName = "workload.googleapis.com/process.runtime.jvm.memory.utilization_ratio";

    ListTimeSeriesPagedResponse response = listAllMetrics(metricName);
    int count = 0;

    for (TimeSeries ts : response.iterateAll()) {
      for (int i = 0; i < ts.getPointsCount(); i++) {
        assertThat(ts.getPoints(i).getValue().getDoubleValue() % 10).isEqualTo(0);
      }
      count += 1;
    }
    assertThat(count).isGreaterThan(0);
  }

  @Test
  public void e2eTracesTest() throws InterruptedException, IOException {
    // Wait for 3 mins for uploading traces
    Thread.sleep(180000);
    int prodTraceCount = 0;
    int debugTraceCount = 0;
    // Restrict time to last 15 minutes
    long startMillis = System.currentTimeMillis() - ((60 * 15) * 1000);

    try (TraceServiceClient traceServiceClient = TraceServiceClient.create()) {
      ListTracesRequest request =
          ListTracesRequest.newBuilder()
              .setProjectId(projectName.getProject())
              .setStartTime(Timestamps.fromMillis(startMillis))
              .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
              .setFilter("+root:total_execution_time" + " " + "job-id:" + jobId)
              .build();

      for (Trace element : traceServiceClient.listTraces(request).iterateAll()) {
        prodTraceCount += 1;
      }
    }

    // decryption_time_per_report is a debug trace which won't be generated when using prod binary.
    try (TraceServiceClient traceServiceClient = TraceServiceClient.create()) {
      ListTracesRequest request =
          ListTracesRequest.newBuilder()
              .setProjectId(projectName.getProject())
              .setStartTime(Timestamps.fromMillis(startMillis))
              .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
              .setFilter("+root:decryption_time_per_report" + " " + "job-id:" + jobId)
              .build();

      for (Trace element : traceServiceClient.listTraces(request).iterateAll()) {
        debugTraceCount += 1;
      }
    }

    assertThat(prodTraceCount).isEqualTo(1);
    assertThat(debugTraceCount).isEqualTo(0);
  }

  private ListTimeSeriesPagedResponse listAllMetrics(String metricType) throws IOException {
    // Restrict time to last 10 minutes
    long startMillis = System.currentTimeMillis() - ((60 * 10) * 1000);
    TimeInterval interval =
        TimeInterval.newBuilder()
            .setStartTime(Timestamps.fromMillis(startMillis))
            .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
            .build();
    ListTimeSeriesRequest.Builder requestBuilder =
        ListTimeSeriesRequest.newBuilder()
            .setName(projectName.toString())
            .setFilter(
                "metric.type=\""
                    + metricType
                    + "\""
                    + " AND "
                    + "metric.label.custom_namespace=\""
                    + ENVIRONMENT_NAME
                    + "\""
                    + " AND "
                    + "resource.type=\"generic_node\"")
            .setInterval(interval)
            .setView(TimeSeriesView.FULL);
    ListTimeSeriesRequest request = requestBuilder.build();
    ListTimeSeriesPagedResponse response;
    try (final MetricServiceClient client = MetricServiceClient.create(); ) {
      response = client.listTimeSeries(request);
    }
    return response;
  }

  private static class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      ImpersonatedCredentials credentials;
      try {
        credentials =
            ImpersonatedCredentials.newBuilder()
                .setSourceCredentials(GoogleCredentials.getApplicationDefault())
                .setTargetPrincipal(getTestServiceAccount())
                .setScopes(Arrays.asList("https://www.googleapis.com/auth/devstorage.read_write"))
                .build();
      } catch (IOException e) {
        throw new RuntimeException("Invalid credentials", e);
      }
      bind(Storage.class)
          .toInstance(
              StorageOptions.newBuilder()
                  .setProjectId(getTestProjectId())
                  .setCredentials(credentials)
                  .build()
                  .getService());
    }
  }
}
