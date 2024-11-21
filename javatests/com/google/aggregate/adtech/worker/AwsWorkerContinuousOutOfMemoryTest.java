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
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.RETRIES_EXHAUSTED;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

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
 * Integration test which runs against an AWS deployment and verifies that the first job (with 30m
 * domain keys) fails with out-of-memory which will restart the worker and enclave. After
 * restarting, the second job with 10k domain should succeed.
 */
@RunWith(JUnit4.class)
public class AwsWorkerContinuousOutOfMemoryTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();
  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);
  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";

  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

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
    return DEFAULT_TEST_DATA_BUCKET;
  }

  @Test
  public void createJobE2ETest() throws Exception {

    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_OOM_test_input.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/30m_OOM_test_domain.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/OOM_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest1 =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_request_1",
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest1, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(RETRIES_EXHAUSTED.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    domainKey =
        String.format(
            "%s/%s/test-inputs/1k_OOM_test_domain.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    outputKey =
        String.format(
            "%s/%s/test-outputs/OOM_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest2 =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_request_2",
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    result = submitJobAndWaitForResult(createJobRequest2, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    assertThat(aggregatedFacts.size()).isGreaterThan(10);
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
