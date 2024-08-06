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

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.PRIVACY_BUDGET_EXHAUSTED;
import static com.google.aggregate.adtech.worker.SmokeTestBase.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.SmokeTestBase.checkFileExists;
import static com.google.aggregate.adtech.worker.SmokeTestBase.checkJobExecutionResult;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;
import static com.google.aggregate.adtech.worker.SmokeTestBase.readDebugResultsFromCloud;
import static com.google.aggregate.adtech.worker.SmokeTestBase.readResultsFromCloud;
import static com.google.aggregate.adtech.worker.SmokeTestBase.submitJobAndWaitForResult;
import static com.google.aggregate.adtech.worker.util.DebugSupportHelper.getDebugFilePrefix;
import static com.google.common.truth.Truth.assertThat;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.gcp.GcsBlobStorageClient;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** GCP integration tests. */
@RunWith(JUnit4.class)
public final class GcpWorkerContinuousSmokeTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject GcsBlobStorageClient gcsBlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;
  @Inject private AvroDebugResultsReaderFactory readerFactory;
  public static final String OUTPUT_DATA_PREFIX_NAME = "-1-of-1";
  private static final Integer DEBUG_DOMAIN_KEY_SIZE = 10000;
  private static final Duration COMPLETION_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);

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
    String inputDataPrefix = String.format("%s/test-inputs/10k_test_input_1.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_1.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format("%s/test-outputs/10k_test_domain_1.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
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
    assertThat(aggregatedFacts.size()).isGreaterThan(10);
  }

  /*
   This test includes sending a non-debug job and aggregatable reports with debug mode enabled.
  */
  @Test
  public void createNotDebugJobE2EReportDebugEnabledTest() throws Exception {
    String inputDataPrefix =
        String.format("%s/test-inputs/10k_test_input_debug.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_debug.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format("%s/test-outputs/10k_test_input_non_debug.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            false,
            Optional.of(getTestDataBucket()),
            Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);
    checkJobExecutionResult(result, SUCCESS.name(), 0);

    // Read output avro from GCS.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    // If the domainOptional is true, the aggregatedFact keys would be more than domain keys
    // Otherwise, aggregatedFact keys would be equal to domain keys
    // The "isAtLeast" assert is set here to accommodate both conditions.
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
    // The debug file shouldn't exist because it's not debug run
    assertThat(
            checkFileExists(
                gcsBlobStorageClient,
                getTestDataBucket(),
                getDebugFilePrefix(outputDataPrefix + OUTPUT_DATA_PREFIX_NAME)))
        .isFalse();
  }

  /*
  This test includes sending a debug job and aggregatable reports with debug mode enabled.
   */
  @Test
  public void createDebugJobE2EReportDebugModeEnabledTest() throws Exception {
    String inputDataPrefix =
        String.format(
            "%s/test-inputs/10k_test_input_debug_for_debug_disabled.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format(
            "%s/test-inputs/10k_test_domain_debug_for_debug_disabled.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_test_input_debug_for_debug_disabled.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            true,
            Optional.of(getTestDataBucket()),
            Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);
    checkJobExecutionResult(result, SUCCESS.name(), 0);

    // Read output avro from GCS.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    // The "isAtLeast" assert is set here to accommodate domainOptional(True/False) conditions.
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);

    // Read debug results avro from GCS.
    ImmutableList<AggregatedFact> aggregatedDebugFacts =
        readDebugResultsFromCloud(
            gcsBlobStorageClient,
            readerFactory,
            getTestDataBucket(),
            getDebugFilePrefix(outputDataPrefix + OUTPUT_DATA_PREFIX_NAME));
    // Debug facts count should be greater than or equal to the summary facts count because some
    // keys are filtered out due to thresholding or not in domain.
    assertThat(aggregatedDebugFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
  }

  /*
   This test includes sending a debug job and aggregatable reports with debug mode disabled
   and uses the same data as the normal e2e test.
  */
  @Test
  public void createDebugJobE2EReportDebugModeDisabledTest() throws Exception {
    String inputDataPrefix = String.format("%s/test-inputs/10k_test_input_2.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_2.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format("%s/test-outputs/10k_test_input_2.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            true,
            Optional.of(getTestDataBucket()),
            Optional.of(domainDataPrefix),
            /* totalReportsCount= */ 10000,
            /* reportErrorThreshold= */ 10);
    JsonNode result = SmokeTestBase.submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

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
  }

  /**
   * End-to-end test for the Aggregate Reporting Debug API. </a> 10k attribution-reporting-debug
   * type reports are provided for aggregation. Verifies job status and the size of summary report
   * facts.
   */
  @Test
  public void createJobE2EAggregateReportingDebugTest() throws Exception {
    String inputDataPrefix =
        String.format("%s/test-inputs/10k_test_input_attribution_debug.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_attribution_debug.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_test_output_attribution_debug.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
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
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
  }

  /**
   * This test includes sending a job with reporting site only. Verifies that jobs with only
   * reporting site are successful.
   */
  @Test
  public void createJobE2ETestWithReportingSite() throws Exception {
    var inputDataPrefix =
        String.format("%s/test-inputs/10k_test_input_reporting_site.avro", KOKORO_BUILD_ID);
    var domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_reporting_site.avro", KOKORO_BUILD_ID);
    var outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_test_output_reporting_site.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithReportingSite(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    checkJobExecutionResult(result, SUCCESS.name(), 0);

    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
  }

  /**
   * This test includes sending a job with reports from multiple reporting origins belonging to the
   * same reporting site. Verifies that all the reports are processed successfully.
   */
  @Test
  public void createJobE2ETestWithMultipleReportingOrigins() throws Exception {
    var inputDataPrefix = String.format("%s/test-inputs/same-site/", KOKORO_BUILD_ID);
    var domainDataPrefix =
        String.format(
            "%s/test-inputs/10k_test_domain_multiple_origins_same_site.avro", KOKORO_BUILD_ID);
    var outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_test_output_multiple_origins_same_site.avro.result",
            KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithReportingSite(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    checkJobExecutionResult(result, SUCCESS.name(), 0);

    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    assertThat(aggregatedFacts.size()).isAtLeast(DEBUG_DOMAIN_KEY_SIZE);
  }

  /**
   * This test includes sending a job with reports from multiple reporting origins belonging to
   * different reporting sites. It is expected that the 5k reports with a different reporting site
   * will fail and come up in the error counts.
   */
  @Test
  public void createJobE2ETestWithSomeReportsHavingDifferentReportingOrigins() throws Exception {
    var inputDataPrefix = String.format("%s/test-inputs/different-site/", KOKORO_BUILD_ID);
    var domainDataPrefix =
        String.format(
            "%s/test-inputs/10k_test_domain_multiple_origins_different_site.avro", KOKORO_BUILD_ID);
    var outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_test_output_multiple_origins_different_site.avro.result",
            KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithReportingSite(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            /* outputDomainBucketName= */ Optional.of(getTestDataBucket()),
            /* outputDomainPrefix= */ Optional.of(domainDataPrefix));
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
        .isEqualTo(5000);
    assertThat(
            result
                .get("result_info")
                .get("error_summary")
                .get("error_counts")
                .get(0)
                .get("category")
                .asText())
        .isEqualTo(ErrorCounter.REPORTING_SITE_MISMATCH.name());

    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + OUTPUT_DATA_PREFIX_NAME);
    assertThat(aggregatedFacts.size()).isAtLeast(5000);
  }

  /*
   Creates a job and waits for successful completion. Then creates another job for the same data
   and verifies the privacy budget is exhausted.
  */
  @Test
  public void createJobE2ETestPrivacyBudgetExhausted() throws Exception {
    String inputDataPrefix = String.format("%s/test-inputs/10k_test_input_3.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/10k_test_domain_3.avro", KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format("%s/test-outputs/10k_test_input_3.avro.result", KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest1 =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputDataPrefix,
            getTestDataBucket(),
            outputDataPrefix,
            Optional.of(getTestDataBucket()),
            Optional.of(domainDataPrefix));
    JsonNode result = submitJobAndWaitForResult(createJobRequest1, COMPLETION_TIMEOUT);
    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());
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
