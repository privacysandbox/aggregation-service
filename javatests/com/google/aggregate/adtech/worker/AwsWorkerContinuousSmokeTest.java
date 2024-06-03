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
import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.AWS_S3_BUCKET_REGION;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getOutputFileName;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readDebugResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.readResultsFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.aggregate.adtech.worker.util.DebugSupportHelper.getDebugFilePrefix;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
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
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
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
 *   <li>One report input file with version 0.1 of reports and debug mode enabled on generated
 *       reports. The file follows the naming format 10k_attribution_report_test_input_debug.avro
 * </ul>
 *
 * The expected domain files are of 3 types as detailed below and are located at the S3 prefix
 * "s3://aggregation-service-testing/$KOKORO_BUILD_ID/test-inputs":
 *
 * <ul>
 *   <li>Multiple domain files for version 0.1 of reports. Each file follows the naming format
 *       10k_test_domain_${number}.avro. Each test below that relies on version 0.1 reports is
 *       supposed to use one of the above domain files each that matches the report input.
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
  @Rule public final TestName name = new TestName();

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  private static final Integer DEBUG_DOMAIN_KEY_SIZE = 10000;

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";

  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";

  private static final Logger logger = LoggerFactory.getLogger(AwsWorkerContinuousSmokeTest.class);

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
    String testDataBucket = System.getenv("TEST_DATA_BUCKET");
    return testDataBucket != null ? testDataBucket : DEFAULT_TEST_DATA_BUCKET;
  }

  private static boolean runPrivateAggregationTests() {
    return System.getenv("RUN_PRIVATE_AGGREGATION_TESTS") != null
        && System.getenv("RUN_PRIVATE_AGGREGATION_TESTS").equalsIgnoreCase("true");
  }

  private static boolean runMultiOutputShardTest() {
    return System.getenv("RUN_MULTI_OUTPUT_SHARD_TEST") != null
        && System.getenv("RUN_MULTI_OUTPUT_SHARD_TEST").equalsIgnoreCase("true");
  }

  /*
     Starts with a createJob request to API gateway with the test inputs pre-uploaded in s3
     bucket. Ends by calling getJob API to retrieve result information. Assertions are made on
     result status (SUCCESS) and result avro (not empty) which sits in the testing bucket.
  */
  @Test
  public void createJobE2ETest() throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_1.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_1.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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

    // TODO(b/228874552) assert that the output contains more values than just the output domain
    // values
    assertThat(aggregatedFacts.size()).isGreaterThan(10);
  }

  /**
   * This test includes sending a non-debug job and aggregatable reports with debug mode enabled.
   */
  @Test
  public void createNotDebugJobE2EReportDebugEnabledTest() throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_input_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_domain_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_attribution_report_test_output_notDebugJob_debugEnabled.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ false,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_input_debug_enabled_nondebug_run.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_domain_debug_enabled_nondebug_run.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_attribution_report_test_output_DebugJob_debugEnabled.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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

    // The "isAtLeast" assert is set here to accommodate domainOptional(True/False) conditions.
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);

    // Read debug results avro from s3.
    ImmutableList<AggregatedFact> aggregatedDebugFacts =
        readDebugResultsFromS3(
            s3BlobStorageClient,
            readerFactory,
            getTestDataBucket(),
            getOutputFileName(getDebugFilePrefix(outputKey)));

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
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_2.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_2.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_DebugJob_debugDisabled.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name());
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("count")
                .asInt())
        .isEqualTo(10000);
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("category")
                .asText())
        .isEqualTo(ErrorCounter.DEBUG_NOT_ENABLED.name());
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("description")
                .asText())
        .isEqualTo(ErrorCounter.DEBUG_NOT_ENABLED.getDescription());

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey));

    assertThat(aggregatedFacts.size()).isEqualTo(DEBUG_DOMAIN_KEY_SIZE);

    // Read debug result from s3.
    ImmutableList<AggregatedFact> aggregatedDebugFacts =
        readDebugResultsFromS3(
            s3BlobStorageClient,
            readerFactory,
            getTestDataBucket(),
            getOutputFileName(getDebugFilePrefix(outputKey)));

    // Only contains keys in domain because all reports are filtered out.
    assertThat(aggregatedDebugFacts.size()).isEqualTo(DEBUG_DOMAIN_KEY_SIZE);
    // The unnoisedMetric of aggregatedDebugFacts should be 0 for all keys because
    // all reports are filtered out.
    // Noised metric in both debug reports and summary reports should be noise value instead of 0.
    aggregatedDebugFacts.forEach(fact -> assertThat(fact.unnoisedMetric().get()).isEqualTo(0));
  }

  @Test
  public void aggregate_withDebugReportsInNonDebugMode_errorsExceedsThreshold_quitsEarly()
      throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_2.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_2.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_errorsExceedsThreshold.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* debugRun= */ true,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey),
            /* reportErrorThresholdPercentage= */ 10,
            /* inputReportCount= */ Optional.of(10000L));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.REPORTS_WITH_ERRORS_EXCEEDED_THRESHOLD.name());
    // Due to parallel aggregation, the processing may stop a little over the threshold.
    // So, asserting below that the processing stopped somewhere above the threshold but before all
    // the 10K reports are processed.
    int erroringReportCount =
        result
            .get("result_info")
            .get("error_summary")
            .get("error_counts")
            .get(0)
            .get("count")
            .asInt();
    assertThat(erroringReportCount).isAtLeast(1000);
    assertThat(erroringReportCount).isLessThan(10000);
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("category")
                .asText())
        .isEqualTo(ErrorCounter.DEBUG_NOT_ENABLED.name());
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("description")
                .asText())
        .isEqualTo(ErrorCounter.DEBUG_NOT_ENABLED.getDescription());
    assertThat(
            AwsWorkerContinuousTestHelper.checkS3FileExists(
                s3BlobStorageClient, getTestDataBucket(), outputKey))
        .isFalse();
  }

  /**
   * End to end test for the Aggregate Reporting Debug API. </a> 10k attribution-reporting-debug
   * type reports are provided for aggregation.
   */
  @Test
  public void createJobE2EAggregateReportingDebugTest() throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_attribution_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_attribution_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_attribution_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    // TODO: b/322832198 - Update assertions once Debug Reporting API is launched.
    // The threshold is 100%, so we get SUCCESS_WITH_ERRORS.
    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name());
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("count")
                .asInt())
        .isEqualTo(10000);
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("category")
                .asText())
        .isEqualTo(ErrorCounter.UNSUPPORTED_REPORT_API_TYPE.name());
  }

  @Test
  public void createJobE2ETestPrivacyBudgetExhausted() throws Exception {
    // End to end testing:
    //   Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
    //   bucket.
    //   Ends by calling getJob API to retrieve result information.
    //   Assertions are made on result status (SUCCESS) and result avro (not empty) which sits in
    //   the testing bucket.

    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_3.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_3.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest1 =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_request_1",
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest1, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    CreateJobRequest createJobRequest2 =
        createJobRequest1.toBuilder()
            .setJobRequestId(
                getClass().getSimpleName() + "::" + name.getMethodName() + "_request_2")
            .build();

    result = submitJobAndWaitForResult(createJobRequest2, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();
  }

  /**
   * End to end test for Private Aggregation API - fledge reports. See <a
   * href="https://github.com/patcg-individual-drafts/private-aggregation-api#turtledovefledge-reporting">Fledge
   * API.</a> 10k fledge type reports are provided for aggregation.
   */
  @Test
  public void createJobE2EFledgeTest() throws Exception {
    // Do not run private aggregation test if env variable is set.
    if (!runPrivateAggregationTests()) {
      logger.info("Skipping Private Aggregation API type Fledge test");
      return;
    }

    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_fledge.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/20k_test_domain_fledge.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_fledge.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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
  }

  /**
   * End to end test for Private Aggregation API - shared-storage reports. See <a
   * href="https://github.com/WICG/shared-storage>Shared Storage API.</a> 10k shared-storage type
   * reports are provided for aggregation.
   */
  @Test
  public void createJobE2ESharedStorageTest() throws Exception {
    // Do not run private aggregation test if env variable is set.
    if (!runPrivateAggregationTests()) {
      logger.info("Skipping Private Aggregation API type Shared Storage test");
      return;
    }

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

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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
  }

  @Test
  public void createDebugJobE2ETestPrivacyBudgetExhausted() throws Exception {
    String inputKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_input_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    String domainKey =
        String.format(
            "%s/%s/test-inputs/10k_attribution_report_test_domain_debug.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    String outputKey =
        String.format(
            "%s/%s/test-outputs/10k_attribution_report_test_output_DebugJob_debugEnabled_PBSExhausted.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest1 =
        AwsWorkerContinuousTestHelper.createJobRequest(
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
        .isEqualTo(DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();
    CreateJobRequest createJobRequest2 =
        createJobRequest1.toBuilder()
            .setJobRequestId(
                getClass().getSimpleName() + "::" + name.getMethodName() + "_request_2")
            .build();

    result = submitJobAndWaitForResult(createJobRequest2, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(DEBUG_SUCCESS_WITH_PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();
  }

  /*
   Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
   bucket. Ends by calling getJob API to retrieve result information. Assertions are made on
   result status (SUCCESS) and result avro (not empty) which sits in the testing bucket.
   The output files must be sharded into 3 separate files.
  */
  @Test
  public void createJobE2ETestWithMultiOutputShard() throws Exception {
    // Skip this test for the case where build parameter cannot be set (ex> release image)
    if (!runMultiOutputShardTest()) {
      logger.info("Skipping multi output shard test");
      return;
    }

    var inputKey =
        String.format(
            "%s/%s/test-inputs/30k_test_input.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/30k_test_domain.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/30k_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFactsInShard1 =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey, 1, 2));
    ImmutableList<AggregatedFact> aggregatedFactsInShard2 =
        readResultsFromS3(
            s3BlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            getOutputFileName(outputKey, 2, 2));

    assertThat(aggregatedFactsInShard1.size()).isGreaterThan(14000);
    assertThat(aggregatedFactsInShard2.size()).isGreaterThan(14000);
  }

  /**
   * End to end test for aggregating 10k reports with invalid key. Tests if the exception caching
   * works. This test would fail without exception caching due to timeout.
   */
  @Test
  public void createJobE2ETestWithInvalidReports() throws Exception {
    var inputKey =
        String.format(
            "%s/%s/test-inputs/10k_test_input_invalid_key.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var domainKey =
        String.format(
            "%s/%s/test-inputs/10k_test_domain_invalid_key.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
    var outputKey =
        String.format(
            "%s/%s/test-outputs/10k_test_output_invalid_key.avro",
            TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainKey));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    // The job should be completed before the completion timeout.
    assertThat(result.get("job_status").asText()).isEqualTo("FINISHED");
    // The threshold is 100%, so we get SUCCESS_WITH_ERRORS.
    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS_WITH_ERRORS.name());
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
