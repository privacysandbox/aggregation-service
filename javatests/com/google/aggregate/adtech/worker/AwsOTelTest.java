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

import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.AWS_S3_BUCKET_REGION;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getOutputFileName;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.common.truth.Truth.assertThat;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.model.GetTraceSummariesRequest;
import com.amazonaws.services.xray.model.GetTraceSummariesResult;
import com.amazonaws.services.xray.model.TraceSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * In AwsOTeltest, one job would be sent first to trigger metric/trace generation. And the following
 * tests are testing if OTel metrics and traces exist in Cloudwatch and AWS Xray. In continuous
 * environment, prod binary is used for OTel which would only export prod metrics. Use
 * FixMethodOrder for this class to ensure the job will be running first to generate metrics and
 * traces.
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AwsOTelTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";
  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";
  private static final String ENVIRONMENT_NAME = "continuous-mp";
  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  private static final AmazonCloudWatch amazonCloudWatch =
      AmazonCloudWatchClientBuilder.standard().withRegion("us-east-1").build();

  private String jobId = getClass().getSimpleName();
  private String testDataBucket = SmokeTestBase.getTestDataBucket(DEFAULT_TEST_DATA_BUCKET);

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  /** This test will run first to send a job and trigger metrics and traces generation. */
  @Test
  public void createJobE2ETest() throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_otel_test_input.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_otel_test_domain.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_otel_test_output.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            testDataBucket,
            inputKey,
            testDataBucket,
            outputKey,
            /* jobId= */ jobId,
            /* outputDomainBucketName= */ Optional.of(testDataBucket),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            testDataBucket,
            getOutputFileName(outputKey));

    assertThat(aggregatedFacts.size()).isAtLeast(10000);
  }

  @Test
  public void e2eCPUMetricTest() {
    GetMetricDataRequest request =
        generateGetMetricDataRequest("process.runtime.jvm.CPU.utilization");

    List<MetricDataResult> metrics = amazonCloudWatch.getMetricData(request).getMetricDataResults();
    MetricDataResult metricResult = metrics.get(0);

    assertThat(metricResult.getValues().size()).isGreaterThan(0);
    for (int i = 0; i < metricResult.getValues().size(); i++) {
      assertThat(metricResult.getValues().get(i) % 1).isEqualTo(0);
    }
  }

  @Test
  public void e2eMemoryMetricTest() {
    GetMetricDataRequest request =
        generateGetMetricDataRequest("process.runtime.jvm.memory.utilization_ratio");

    List<MetricDataResult> metrics = amazonCloudWatch.getMetricData(request).getMetricDataResults();
    MetricDataResult metricResult = metrics.get(0);

    assertThat(metricResult.getValues().size()).isGreaterThan(0);
    for (int i = 0; i < metricResult.getValues().size(); i++) {
      assertThat(metricResult.getValues().get(i) % 10).isEqualTo(0);
    }
  }

  @Test
  public void e2eTracesTest() throws InterruptedException {
    // Adding 3 mins sleep here to make sure all traces are uploaded.
    Thread.sleep(180000);
    AtomicInteger prodTraceCount = new AtomicInteger(0);
    AtomicInteger debugTraceCount = new AtomicInteger(0);
    AWSXRay awsxRay = AWSXRayClientBuilder.standard().withRegion("us-east-1").build();
    Date currentTime = new Date(System.currentTimeMillis());
    Date searchStartTime =
        new Date(System.currentTimeMillis() - 600 * 1000); // Check last 10 minutes

    GetTraceSummariesResult traceSummaryResult =
        awsxRay.getTraceSummaries(
            new GetTraceSummariesRequest().withStartTime(searchStartTime).withEndTime(currentTime));
    List<TraceSummary> traceSummaries = traceSummaryResult.getTraceSummaries();
    traceSummaries.forEach(
        v -> {
          if (v.getAnnotations()
              .get("job_id")
              .get(0)
              .getAnnotationValue()
              .getStringValue()
              .equals(jobId)) {
            if (v.getServiceIds().get(0).getName().equals("total_execution_time")) {
              prodTraceCount.getAndIncrement();
            } else {
              debugTraceCount.getAndIncrement();
            }
          }
        });

    assertThat(prodTraceCount.get()).isEqualTo(1);
    assertThat(debugTraceCount.get()).isEqualTo(0);
  }

  private GetMetricDataRequest generateGetMetricDataRequest(String metricName) {
    Date endTime = new Date(System.currentTimeMillis());
    Dimension dim =
        new Dimension()
            .withName("OTelLib")
            .withValue("com.google.privacysandbox.otel.OTelConfigurationImpl");
    Date startTime = new Date(endTime.getTime() - 600000); // query for the last 10 mins.
    Metric metric =
        new Metric().withMetricName(metricName).withNamespace(ENVIRONMENT_NAME).withDimensions(dim);
    MetricStat metricStat = new MetricStat().withMetric(metric).withStat("Sum").withPeriod(1);
    MetricDataQuery query = new MetricDataQuery().withId("test").withMetricStat(metricStat);
    return new GetMetricDataRequest()
        .withMetricDataQueries(query)
        .withStartTime(startTime)
        .withEndTime(endTime);
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
