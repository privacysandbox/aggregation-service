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
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.tools.diff.ResultDiffer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.math.BigInteger;
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

@RunWith(JUnit4.class)
public class AwsWorkerContinuousDiffTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();

  private static final Duration completionTimeout = Duration.of(60, ChronoUnit.MINUTES);

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";

  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";

  // Input data is generated in shared_e2e.sh
  private static final String inputKey =
      String.format(
          "%s/%s/test-inputs/10k_diff_test_input_sharded/",
          TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
  private static final String domainKey =
      String.format(
          "%s/%s/test-inputs/diff_test_domain_sharded/", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
  private static final String outputKey =
      String.format(
          "%s/%s/test-outputs/10k_diff_test_output.avro", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  /**
   * TEST_DATA_BUCKET is used for storing the input data and the output results. If TEST_DATA_BUCKET
   * is not set in the environment variable, DEFAULT_TEST_DATA_BUCKET is used.
   */
  private static String getTestDataBucket() {
    if (System.getenv("TEST_DATA_BUCKET") != null) {
      return System.getenv("TEST_DATA_BUCKET");
    }
    return DEFAULT_TEST_DATA_BUCKET;
  }

  @Test
  public void e2eDiffTest() throws Exception {
    // To update golden:
    //    1. Run this test
    //    2. Find the latest test output under
    // s3://aggregation-service-testing/<BUILD-ID>/test-outputs/10k_diff_test_output.avro
    //    3. Copy/Move above file to
    // s3://aggregation-service-testing/testdata/golden/<TODAY'S-DATE>/10k_diff_test.avro.golden
    //    4. Update the value of 'goldenLocation' below to the new path

    String goldenLocation = "testdata/golden/2022_10_18/10k_diff_test.avro.golden";

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputKey,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            Optional.of(getTestDataBucket()),
            Optional.of(domainKey));

    JsonNode result = submitJobAndWaitForResult(createJobRequest, completionTimeout);

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
    ImmutableList<AggregatedFact> goldenAggregatedFacts =
        readResultsFromS3(
            s3BlobStorageClient, avroResultsFileReader, getTestDataBucket(), goldenLocation);

    MapDifference<BigInteger, AggregatedFact> diffs =
        ResultDiffer.diffResults(aggregatedFacts.stream(), goldenAggregatedFacts.stream());
    assertWithMessage(
            String.format(
                "Found (%s) diffs between left(test) and right(golden). Found (%s) entries only on"
                    + " left(test) and (%s) entries only on right(golden).",
                diffs.entriesDiffering().size(),
                diffs.entriesOnlyOnLeft().size(),
                diffs.entriesOnlyOnRight().size()))
        .that(diffs.areEqual())
        .isTrue();

    // Dump 100 lines of diffs for debugging/verification.
    System.out.println("Preview at most 100 diffs:");
    diffs.entriesDiffering().entrySet().stream().limit(100).forEach(System.out::println);
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
