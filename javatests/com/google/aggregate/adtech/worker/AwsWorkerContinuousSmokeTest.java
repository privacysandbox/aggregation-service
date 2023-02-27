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

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.AWS_S3_BUCKET_REGION;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readDebugResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.aggregate.adtech.worker.util.DebugSupportHelper.getDebugFilePrefix;
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
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Integration test which runs against an AWS deployment and verifies that a job accessing an
 * existing encrypted payload can be processed by the system and produce an output avro file.
 *
 * <p>The expected input files are of 3 types as detailed below and are located at the S3 prefix
 * "s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-inputs". Currently, there are multiple
 * tests that rely on reports with version 0.1 and hence we generate multiple input report files for
 * version 0.1. This is necessary because everytime a report file is used, the privacy budget
 * associated with it is consumed and it cannot be reused. Similar technique can be followed to
 * generate multiple report files in other versions as per need. Please see shared_e2e.sh for
 * details:
 *
 * <ul>
 *   <li>Multiple report input files with version 0.1 of reports. Each file follows the naming
 *       format 10k_test_input_${number}.avro. Each test below that relies on version 0.1 reports is
 *       supposed to use one of the above input files each
 *   <li>One report input file with report version 0.0 which is the report version before 0.1 and
 *       can be generated with --generated_report_version ""` in SimulationRunner. The file follows
 *       the naming format 10k_test_input_v0_0.avro
 *   <li>One report input file with version 0.1 of reports and debug mode enabled on generated
 *       reports. The file follows the naming format 10k_test_input_debug.avro
 * </ul>
 *
 * The expected domain files are of 3 types as detailed below and are located at the S3 prefix
 * "s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-inputs":
 *
 * <ul>
 *   <li>Multiple domain files for version 0.1 of reports. Each file follows the naming format
 *       10k_test_domain_${number}.avro. Each test below that relies on version 0.1 reports is
 *       supposed to use one of the above domain files each that matches the report input.
 *   <li>One domain file for reports with version 0.0 which is the report version before 0.1. The
 *       file follows the naming format 10k_test_domain_v0_0.avro
 *   <li>One domain file for reports in version 0.1 with debug mode enabled on the reports. The file
 *       follows the naming format 10k_test_domain_debug.avro
 * </ul>
 *
 * The expected distribution file used can be found at
 * "s3://aggregation-service-testing/testdata/1m_staging_1_integer_buckets.txt"
 *
 * <p>The resulting output files are:
 *
 * <ul>
 *   <li>"s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-outputs/10k_test_output_${number}.avro"
 *   <li>"s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-outputs/10k_test_output_v0_0.avro"
 *   <li>"s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-outputs/10k_test_output_debug_nodebug.avro"
 *   <li>"s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-outputs/10k_test_output_nodebug_nodebug.avro"
 *   <li>"s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-outputs/10k_test_output_debug_debug.avro"
 * </ul>
 *
 * See the definition of continuous_smoke_test() in //kokoro/gcp_ubuntu/shared_e2e.sh for how these
 * are generated.
 */
@RunWith(JUnit4.class)
public class AwsWorkerContinuousSmokeTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  private static final Integer DEBUG_DOMAIN_KEY_SIZE = 10000;

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";

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
    return DEFAULT_TEST_DATA_BUCKET;
  }

  @Test
  public void createJobE2ETest() throws Exception {
    // End to end testing:
    //   Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
    //   bucket.
    //   Ends by calling getJob API to retrieve result information.
    //   Assertions are made on result status (SUCCESS) and result avro (not empty) which sits in
    //   the testing bucket.

    var inputKey = String.format("%s/test-inputs/10k_test_input_1.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_1.avro", KOKORO_BUILD_ID);
    var outputKey = String.format("%s/test-outputs/10k_test_output.avro", KOKORO_BUILD_ID);

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

    // TODO(b/228874552) assert that the output contains more values than just the output domain
    // values
    assertThat(aggregatedFacts.size()).isGreaterThan(10);
  }

  @Test
  public void createJobE2EVersionZeroDotZeroTest() throws Exception {
    // End to end testing:
    // Follows same idea as createJobE2ETest but uses report with version 0.0 (the report version
    // before v0.1) and old format shared info.

    var inputKey = String.format("%s/test-inputs/10k_test_input_v0_0.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_v0_0.avro", KOKORO_BUILD_ID);
    var outputKey = String.format("%s/test-outputs/10k_test_output_v0_0.avro", KOKORO_BUILD_ID);

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

    // TODO(b/228874552) assert that the output contains more values than just the output domain
    // values
    assertThat(aggregatedFacts.size()).isGreaterThan(10);
  }

  /**
   * This test includes sending a non-debug job and aggregatable reports with debug mode enabled.
   */
  @Test
  public void createNotDebugJobE2EReportDebugEnabledTest() throws Exception {
    var inputKey = String.format("%s/test-inputs/10k_test_input_debug.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_debug.avro", KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/test-outputs/10k_test_output_notDebugJob_debugEnabled.avro", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ false,
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

    // If the domainOptional is true, the aggregatedFact keys would be more than domain keys
    // Otherwise, aggregatedFact keys would be equal to domain keys
    // The "isAtLeast" assert is set here to accommodate both conditions.
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
    // The debug file shouldn't exist because it's not debug run
    assertThat(
            AwsWorkerContinuousTestHelper.checkS3FileExists(
                s3BlobStorageClient, getTestDataBucket(), getDebugFilePrefix(outputKey)))
        .isFalse();
  }

  /** This test includes sending a debug job and aggregatable reports with debug mode enabled. */
  @Test
  public void createDebugJobE2EReportDebugModeEnabledTest() throws Exception {
    var inputKey = String.format("%s/test-inputs/10k_test_input_debug.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_debug.avro", KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/test-outputs/10k_test_output_DebugJob_debugEnabled.avro", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
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

    // The "isAtLeast" assert is set here to accommodate domainOptional(True/False) conditions.
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);

    // Read debug results avro from s3.
    ImmutableList<AggregatedFact> aggregatedDebugFacts =
        readDebugResultsFromS3(
            s3BlobStorageClient, readerFactory, getTestDataBucket(), getDebugFilePrefix(outputKey));

    // Debug facts count should be greater than or equal to the summary facts count because some
    // keys are filtered out due to thresholding or not in domain.
    assertThat(aggregatedDebugFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
  }

  /**
   * This test includes sending a debug job and aggregatable reports with debug mode disabled. Uses
   * the same data as the normal e2e test.
   */
  @Test
  public void createDebugJobE2EReportDebugModeDisabledTest() throws Exception {
    var inputKey = String.format("%s/test-inputs/10k_test_input_2.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_2.avro", KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/test-outputs/10k_test_output_DebugJob_debugDisabled.avro", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("count")
                .asInt())
        .isEqualTo(1000);

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient, avroResultsFileReader, getTestDataBucket(), outputKey);

    assertThat(aggregatedFacts.size()).isEqualTo(DEBUG_DOMAIN_KEY_SIZE);

    // Read debug result from s3.
    ImmutableList<AggregatedFact> aggregatedDebugFacts =
        readDebugResultsFromS3(
            s3BlobStorageClient, readerFactory, getTestDataBucket(), getDebugFilePrefix(outputKey));

    // Only contains keys in domain because all reports are filtered out.
    assertThat(aggregatedDebugFacts.size()).isEqualTo(DEBUG_DOMAIN_KEY_SIZE);
    // The unnoisedMetric of aggregatedDebugFacts should be 0 for all keys because
    // all reports are filtered out.
    // Noised metric in both debug reports and summary reports should be noise value instead of 0.
    aggregatedDebugFacts.forEach(fact -> assertThat(fact.unnoisedMetric().get()).isEqualTo(0));
  }

  @Test
  public void createJobE2ETestPrivacyBudgetExhausted() throws Exception {
    // End to end testing:
    //   Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
    //   bucket.
    //   Ends by calling getJob API to retrieve result information.
    //   Assertions are made on result status (SUCCESS) and result avro (not empty) which sits in
    //   the testing bucket.

    var inputKey = String.format("%s/test-inputs/10k_test_input_3.avro", KOKORO_BUILD_ID);
    var domainKey = String.format("%s/test-inputs/10k_test_domain_3.avro", KOKORO_BUILD_ID);
    var outputKey = String.format("%s/test-outputs/10k_test_output.avro", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest1 =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest1, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    CreateJobRequest createJobRequest2 =
        createJobRequest1.toBuilder().setJobRequestId(UUID.randomUUID().toString()).build();

    result = submitJobAndWaitForResult(createJobRequest2, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();
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
    }
  }
}
