/*
 * Copyright 2023 Google LLC
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
import static com.google.aggregate.adtech.worker.SmokeTestBase.createJob;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getJobResult;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;
import static com.google.aggregate.adtech.worker.SmokeTestBase.waitForJobCompletions;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.gcp.GcsBlobStorageClient;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** GCP performance regression test implementation */
@RunWith(JUnit4.class)
public final class GcpWorkerPerformanceRegressionTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public TestName name = new TestName();

  @Inject GcsBlobStorageClient gcsBlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;
  @Inject private AvroDebugResultsReaderFactory readerFactory;
  private static final String PERFORMANCE_REGRESSION_DATA_BUCKET =
      "gcp_performance_regression_test_data";
  private static final int NUM_WARMUP_RUNS = 5;
  private static final int NUM_TRANSIENT_RUNS = 50;

  private static final Duration COMPLETION_TIMEOUT = Duration.of(45, ChronoUnit.MINUTES);

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  /**
   * Run Aggregation for 500k Attribution Reporting API (ARA) reports with 500k Domain keys.
   *
   * <p>These tests are for warming up the instances.
   */
  @Test
  public void aggregateARA500kReports500kDomainWarmup() throws Exception {
    ArrayList<CreateJobRequest> warmUpJobRequests = new ArrayList<>(NUM_WARMUP_RUNS);
    ArrayList<CreateJobRequest> warmUpJobRequestsDeepCopy = new ArrayList<>(NUM_WARMUP_RUNS);

    for (int i = 1; i <= NUM_WARMUP_RUNS; i++) {
      var inputKey = String.format("test-data/%s/test-inputs/500k_report.avro", KOKORO_BUILD_ID);
      var domainKey = String.format("test-data/%s/test-inputs/500k_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_warmup_output.avro",
              KOKORO_BUILD_ID, i);
      CreateJobRequest createJobRequest =
          SmokeTestBase.createJobRequestWithAttributionReportTo(
              getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET),
              inputKey,
              getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET),
              outputKey,
              /* debugRun= */ true,
              /* jobId= */ getClass().getSimpleName()
                  + "::"
                  + name.getMethodName()
                  + "_warmup-"
                  + i,
              /* outputDomainBucketName= */ Optional.of(
                  getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET)),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      createJob(createJobRequest);

      warmUpJobRequests.add(createJobRequest);
      warmUpJobRequestsDeepCopy.add(createJobRequest);
    }

    waitForJobCompletions(warmUpJobRequestsDeepCopy, COMPLETION_TIMEOUT, true);

    for (int i = 1; i <= NUM_WARMUP_RUNS; i++) {
      String outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_warmup_output.avro",
              KOKORO_BUILD_ID, i);
      JsonNode result = getJobResult(warmUpJobRequests.get(i - 1));

      assertThat(result.get("result_info").get("return_code").asText())
          .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());

      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();
    }
  }

  /**
   * Run Aggregation for 500k Attribution Reporting API (ARA) reports with 500k Domain keys.
   *
   * <p>These tests are the transient runs after the warm-up runs.
   */
  @Test
  public void aggregateARA500kReports500kDomainTransient() throws Exception {
    ArrayList<CreateJobRequest> transientJobRequests = new ArrayList<>(NUM_TRANSIENT_RUNS);
    ArrayList<CreateJobRequest> transientJobRequestsDeepCopy = new ArrayList<>(NUM_TRANSIENT_RUNS);
    for (int i = 1; i <= NUM_TRANSIENT_RUNS; i++) {
      var inputKey = String.format("test-data/%s/test-inputs/500k_report.avro", KOKORO_BUILD_ID);
      var domainKey = String.format("test-data/%s/test-inputs/500k_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_transient_output.avro",
              KOKORO_BUILD_ID, i);
      CreateJobRequest createJobRequest =
          SmokeTestBase.createJobRequestWithAttributionReportTo(
              getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET),
              inputKey,
              getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET),
              outputKey,
              /* debugRun= */ true,
              /* jobId= */ getClass().getSimpleName()
                  + "::"
                  + name.getMethodName()
                  + "_transient-"
                  + i,
              /* outputDomainBucketName= */ Optional.of(
                  getTestDataBucket(PERFORMANCE_REGRESSION_DATA_BUCKET)),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      createJob(createJobRequest);
      transientJobRequests.add(createJobRequest);
      transientJobRequestsDeepCopy.add(createJobRequest);
    }

    waitForJobCompletions(
        transientJobRequestsDeepCopy, COMPLETION_TIMEOUT, true);

    for (int i = 1; i <= NUM_TRANSIENT_RUNS; i++) {
      var outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_transient_output.avro",
              KOKORO_BUILD_ID, i);
      JsonNode result = getJobResult(transientJobRequests.get(i - 1));
      assertThat(result.get("result_info").get("return_code").asText())
          .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();
    }
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
