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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * End-to-end test for Private Aggregation API. Following report types are used - 1.
 * protected-audience 2. shared-storage Report size - 10k Domain size - 20k
 */
@RunWith(JUnit4.class)
public class AwsWorkerPrivateAggregationAPITest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";

  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  private static String getTestDataBucket() {
    if (System.getenv("TEST_DATA_BUCKET") != null) {
      return System.getenv("TEST_DATA_BUCKET");
    }
    return DEFAULT_TEST_DATA_BUCKET;
  }

  /**
   * End-to-end test for Private Aggregation API - Protected Audience reports. See <a
   * href="https://github.com/patcg-individual-drafts/private-aggregation-api#turtledovefledge-reporting">
   * Protected Audience.</a> 10k Protected Audience API type reports are provided for aggregation.
   */
  @Test
  public void createJobE2EProtectedAudienceTest() throws Exception {
    // TODO(b/278573071) : Enable private aggregation tests in release tests
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_protected_audience.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/20k_test_domain_protected_audience.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_protected_audience.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    var inputKeyDebug =
        String.format(
            "%s/%s/test-inputs/10k_test_input_protected_audience_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKeyDebug =
        String.format(
            "%s/%s/test-inputs/20k_test_domain_protected_audience_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKeyDebug =
        String.format(
            "%s/%s/test-outputs/10k_test_output_protected_audience_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    /* Debug job */
    CreateJobRequest createDebugJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKeyDebug,
            getTestDataBucket(),
            outputKeyDebug,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_debug",
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKeyDebug));
    JsonNode resultDebug = submitJobAndWaitForResult(createDebugJobRequest, COMPLETION_TIMEOUT);

    // Verify non debug job result
    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    // assert that aggregated facts count is at least equal to number of domain keys
    assertThat(aggregatedFacts.size()).isAtLeast(20000);

    // Verify debug job result
    assertThat(resultDebug.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(resultDebug.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read debug output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFactsDebug =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    // assert that debug job aggregated facts count is at least equal to number of domain keys
    assertThat(aggregatedFactsDebug.size()).isAtLeast(20000);
  }

  /**
   * End-to-end test for Private Aggregation API - shared-storage reports. See <a
   * href="https://github.com/WICG/shared-storage>Shared Storage API.</a> 10k shared-storage API
   * type reports are provided for aggregation.
   */
  @Test
  public void createJobE2ESharedStorageTest() throws Exception {
    // TODO(b/278573071) : Enable private aggregation tests in release tests
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_shared_storage.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/20k_test_domain_shared_storage.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_shared_storage.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    var inputKeyDebug =
        String.format(
            "%s/%s/test-inputs/10k_test_input_shared_storage_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKeyDebug =
        String.format(
            "%s/%s/test-inputs/20k_test_domain_shared_storage_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKeyDebug =
        String.format(
            "%s/%s/test-outputs/10k_test_output_shared_storage_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    /* Debug job */
    CreateJobRequest createDebugJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKeyDebug,
            getTestDataBucket(),
            outputKeyDebug,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_debug",
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKeyDebug));
    JsonNode resultDebug = submitJobAndWaitForResult(createDebugJobRequest, COMPLETION_TIMEOUT);

    // Verify non debug job result
    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    // assert that aggregated facts count is at least equal to number of domain keys
    assertThat(aggregatedFacts.size()).isAtLeast(20000);

    // Verify debug job result
    assertThat(resultDebug.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(resultDebug.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read debug output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFactsDebug =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    // assert that debug job aggregated facts count is at least equal to number of domain keys
    assertThat(aggregatedFactsDebug.size()).isAtLeast(20000);
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
