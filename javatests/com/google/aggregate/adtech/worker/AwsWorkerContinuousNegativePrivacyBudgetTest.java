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
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getS3Bucket;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getS3Key;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
 * <p>The default file read is is
 * "s3://aggregation-service-testing/testdata/10k_staging_2022_03_30.avro" but this path can be
 * overriden by setting the TEST_INPUT_PATH environment variable using that same s3 syntax.
 *
 * <p>The default output file location is
 * "s3://aggregation-service-testing/e2e_test_outputs/$KOKORO_BUILD_ID/10k_staging.avro.test" but
 * this path can be overriden by setting the TEST_OUTPUT_PATH environment variable using that same
 * s3 syntax.
 */
@RunWith(JUnit4.class)
public class AwsWorkerContinuousNegativePrivacyBudgetTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  // Input data generated with the following command:
  // bazel run java/com/google/aggregate/simulation:SimluationRunner -- \
  //   --aggregatable_report_file_path $PWD/10k_staging_2022_05_20.avro \
  //   --num_reports 10000 \
  //   --num_encryption_keys 3 \
  //   --encryption_key_service CLOUD \
  //   --public_key_vending_uri \
  // https://jykzugjj3g.execute-api.us-west-2.amazonaws.com/stage/v1alpha/publicKeys \
  //   --distribution FILE \
  //   --distribution_file_path $PWD/1m_staging_1_integer_buckets.txt \
  //   --generate_output_domain \
  //   --output_domain_path "$PWD/10k_staging_domain_2022_05_20.avro" \
  //   --output_domain_size 1000
  // Where the distribution file used is
  // s3://aggregation-service-testing/testdata/1m_staging_1_integer_buckets.txt
  private static String DEFAULT_INPUT_DATA_URI =
      "s3://aggregation-service-testing/testdata/10k_staging_2022_05_20.avro";
  private static String DEFAULT_DOMAIN_DATA_URI =
      "s3://aggregation-service-testing/testdata/10k_staging_domain_2022_05_20.avro";

  /** Returns the S3 URI to use as the input for the aggregate job. */
  private static String getInputDataUri() {
    if (System.getenv("TEST_INPUT_PATH") != null) {
      return System.getenv("TEST_INPUT_PATH");
    }
    return DEFAULT_INPUT_DATA_URI;
  }

  /** Returns the S3 URI to use as the input for the aggregate job. */
  private static String getDomainDataUri() {
    if (System.getenv("TEST_DOMAIN_PATH") != null) {
      return System.getenv("TEST_DOMAIN_PATH");
    }
    return DEFAULT_DOMAIN_DATA_URI;
  }

  /** Returns the S3 URI to use as the output destination for the aggregate job. */
  private static String getOutputDataUri() {
    if (System.getenv("TEST_OUTPUT_PATH") != null) {
      return System.getenv("TEST_OUTPUT_PATH");
    }
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException(
          "if TEST_OUTPUT_PATH is not set, KOKORO_BUILD_ID env var must be set.");
    }
    return String.format(
        "s3://aggregation-service-testing/e2e_test_outputs/%s/%s",
        KOKORO_BUILD_ID, "10k_staging.avro.test");
  }

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  @Test
  public void createJobE2ETest() throws Exception {
    // End to end testing:
    //   Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
    //   bucket.
    //   Ends by calling getJob API to retrieve result information.
    //   Assertions are made on result status (SUCCESS) and result avro (not empty) which sits in
    //   the testing bucket.

    var inputBucket = getS3Bucket(getInputDataUri());
    var inputKey = getS3Key(getInputDataUri());
    var outputBucket = getS3Bucket(getOutputDataUri());
    var outputKey = getS3Key(getOutputDataUri());
    var domainBucket = getS3Bucket(getDomainDataUri());
    var domainKey = getS3Key(getDomainDataUri());

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequest(
            inputBucket,
            inputKey,
            outputBucket,
            outputKey,
            /* outputDomainBucketName= */ Optional.of(domainBucket),
            /* outputDomainPrefix= */ Optional.of(domainKey),
            /* debugPrivacyBudgetLimit= */ Optional.of("1"));
    JsonNode result = submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText())
        .isEqualTo(PRIVACY_BUDGET_EXHAUSTED.name());
    assertThat(result.get("result_info").get("return_message").asText())
        .contains("Insufficient privacy budget");
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
