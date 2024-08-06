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

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.AWS_S3_BUCKET_REGION;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.createJob;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getJobResult;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.waitForJobCompletions;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Aggregation runs to perform Performance Regression Test. Runs 50 Aggregation jobs with 500k
 * report size and fixed 500k domain keys.
 */
@RunWith(JUnit4.class)
public class AwsWorkerPerformanceRegressionTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public TestName name = new TestName();

  private static final Duration COMPLETION_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);

  private static final String PERFORMANCE_REGRESSION_DATA_BUCKET =
      "aggregate-service-performance-regression-bucket";

  private static final int NUM_WARMUP_RUNS = 5;

  private static final int NUM_TRANSIENT_RUNS = 50;

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;
  @Inject private AvroDebugResultsReaderFactory readerFactory;

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  private static String getTestDataBucket() {
    if (System.getenv("TEST_DATA_BUCKET") != null) {
      return System.getenv("TEST_DATA_BUCKET");
    }
    return PERFORMANCE_REGRESSION_DATA_BUCKET;
  }

  /** Run Aggregation for 500k Attribution Reporting API (ARA) reports with 500k Domain keys. */
  @Test
  public void aggregateARA500kTransient() throws Exception {

    for (int i = 1; i <= NUM_TRANSIENT_RUNS; i++) {
      var inputKey = String.format("test-data/%s/test-inputs/500k_report.avro", KOKORO_BUILD_ID);
      var domainKey = String.format("test-data/%s/test-inputs/500k_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_output.avro",
              KOKORO_BUILD_ID, i);
      CreateJobRequest createJobRequest =
          AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
              getTestDataBucket(),
              inputKey,
              getTestDataBucket(),
              outputKey,
              /* debugRun= */ false,
              /* jobId= */ getClass().getSimpleName()
                  + "::"
                  + name.getMethodName()
                  + "_transient-"
                  + i,
              /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

      assertThat(result.get("result_info").get("return_code").asText())
          .isEqualTo(DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();
    }
  }

  /**
   * Run Aggregation for 500k Attribution Reporting API (ARA) reports with 500k Domain keys.
   *
   * <p>These tests are for warming up the instances.
   *
   * @throws Exception
   */
  @Test
  public void aggregateARA500kReports500kDomainWarmup() throws Exception {

    ArrayList<CreateJobRequest> warmUpJobRequests = new ArrayList<>(NUM_WARMUP_RUNS);
    ArrayList<CreateJobRequest> warmUpJobRequestsDeepCopy = new ArrayList<>(NUM_WARMUP_RUNS);
    // Run 40 jobs to make sure at least each ec2 instance runs a job
    for (int i = 1; i <= NUM_WARMUP_RUNS; i++) {
      var inputKey = String.format("test-data/%s/test-inputs/500k_report.avro", KOKORO_BUILD_ID);
      var domainKey = String.format("test-data/%s/test-inputs/500k_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format(
              "test-data/%s/test-outputs/500k_report_%s_500k_domain_warmup_output.avro",
              KOKORO_BUILD_ID, i);
      CreateJobRequest createJobRequest =
          AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
              getTestDataBucket(),
              inputKey,
              getTestDataBucket(),
              outputKey,
              /* debugRun= */ false,
              /* jobId= */ getClass().getSimpleName()
                  + "::"
                  + name.getMethodName()
                  + "_warmup-"
                  + i,
              /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      createJob(createJobRequest);

      warmUpJobRequests.add(createJobRequest);
      warmUpJobRequestsDeepCopy.add(createJobRequest);
    }

    waitForJobCompletions(warmUpJobRequestsDeepCopy, Duration.of(1000, ChronoUnit.MINUTES));

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
   *
   * @throws Exception
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
          AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
              getTestDataBucket(),
              inputKey,
              getTestDataBucket(),
              outputKey,
              /* debugRun= */ false,
              /* jobId= */ getClass().getSimpleName()
                  + "::"
                  + name.getMethodName()
                  + "_transient-"
                  + i,
              /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      createJob(createJobRequest);
      transientJobRequests.add(createJobRequest);
      transientJobRequestsDeepCopy.add(createJobRequest);
    }

    waitForJobCompletions(
        transientJobRequestsDeepCopy, Duration.of(1000, ChronoUnit.MINUTES), false);

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
      bind(S3Client.class)
          .toInstance(
              S3Client.builder()
                  .region(AWS_S3_BUCKET_REGION)
                  .httpClient(UrlConnectionHttpClient.builder().build())
                  .build());
      bind(S3AsyncClient.class)
          .toInstance(S3AsyncClient.builder().region(AWS_S3_BUCKET_REGION).build());
      bind(Boolean.class).annotatedWith(S3UsePartialRequests.class).toInstance(false);
      bind(Integer.class).annotatedWith(PartialRequestBufferSize.class).toInstance(20);
    }
  }
}
