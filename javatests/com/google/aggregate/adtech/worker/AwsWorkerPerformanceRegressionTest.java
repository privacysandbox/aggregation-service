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
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.common.truth.Truth.assertThat;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Aggregation runs to perform Performance Regression Test. Runs 100 Aggregation jobs with 10k
 * report size and fixed 20k domain keys.
 */
@RunWith(JUnit4.class)
public class AwsWorkerPerformanceRegressionTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  private static final String PERFORMANCE_REGRESSION_DATA_BUCKET =
      "aggregate-service-performance-regression-bucket";

  private static final int NUM_JOBS = 25;

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

  /** Run Aggregation for 10k Attribution Reporting API (ARA) reports with 1M Domain keys. */
  @Test
  public void aggregateARA10kReports1MDomain() throws Exception {

    for (int i = 1; i <= NUM_JOBS; i++) {
      var inputKey = String.format("%s/test-inputs/10k_report_%s.avro", KOKORO_BUILD_ID, i);
      var domainKey = String.format("%s/test-inputs/20k_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format("%s/test-outputs/10k_report_%s_20k_domain_output.avro", KOKORO_BUILD_ID, i);

      CreateJobRequest createJobRequest =
          AwsWorkerContinuousTestHelper.createJobRequest(
              getTestDataBucket(),
              inputKey,
              getTestDataBucket(),
              outputKey,
              /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

      assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());
      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();

      // Read output avro from s3.
      ImmutableList<AggregatedFact> aggregatedFacts =
          readResultsFromS3(
              s3BlobStorageClient, avroResultsFileReader, getTestDataBucket(), outputKey);

      // assert that aggregated facts count is at least equal to number of domain keys
      assertThat(aggregatedFacts.size()).isAtLeast(10000);
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
      bind(Boolean.class).annotatedWith(S3UsePartialRequests.class).toInstance(false);
      bind(Integer.class).annotatedWith(PartialRequestBufferSize.class).toInstance(20);
    }
  }
}
