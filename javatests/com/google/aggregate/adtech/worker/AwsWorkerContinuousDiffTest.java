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
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.createJobRequest;
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
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

@RunWith(JUnit4.class)
public class AwsWorkerContinuousDiffTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration completionTimeout = Duration.of(60, ChronoUnit.MINUTES);

  private static final String DATA_BUCKET = "aggregation-service-testing";

  // Input data generated with the following command:
  // bazel run java/com/google/aggregate/simulation:SimluationRunner -- \
  //   --aggregatable_report_file_path $PWD/1m_staging_2022_05_21.avro \
  //   --num_reports 1000000 \
  //   --num_encryption_keys 3 \
  //   --encryption_key_service CLOUD \
  //   --public_key_vending_uri \
  // https://jykzugjj3g.execute-api.us-west-2.amazonaws.com/stage/v1alpha/publicKeys \
  //   --distribution FILE \
  //   --distribution_file_path $PWD/1m_staging_1.txt
  // Where the distribution file used is
  //  s3://aggregation-service-testing/testdata/1m_staging_1.txt
  // Data then sharded with:
  // Report:
  // bazel run //java/com/google/aggregate/tools/shard:AvroShard -- \
  //   --input $PWD/1m_staging_2022_05_21.avro \
  //   --output_dir $PWD/1m_staging_2022_05_21_sharded \
  //   --num_shards 20
  // Domain:
  // bazel run //java/com/google/aggregate/tools/shard:AvroShard -- \
  //   --input $PWD/1m_staging_2022_08_08_domain.avro \
  //   --output_dir $PWD/1m_staging_2022_0808_sharded_domain \
  //   --num_shards 20 \
  //   --domain

  private static final String INPUT_DATA_PREFIX = "testdata/1m_staging_2022_05_21_sharded/shard";
  private static final String OUTPUT_DOMAIN_PREFIX =
      "testdata/1m_staging_2022_08_08_sharded_domain/shard";

  @Inject S3BlobStorageClient s3BlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  @Test
  public void e2eDiffTest() throws Exception {
    // End to end diff testing:
    //    Starts with a createJob request to API gateway with the test inputs pre-uploaded to s3
    //    bucket.
    //    Ends by calling getJob API to retrieve result information.
    //    Assertions are made on result status (SUCCESS) and result avro comparison with golden
    //    which sits in the testing bucket.
    // To update golden:
    //    1. Log onto aws console
    //    2. Find the latest test output under
    // s3://aggregation-service-testing/e2e_test_outputs/<BUILD-ID>/1m_staging_1_sharded.avro.test
    //    3. Copy/Move above file to
    // s3://aggregation-service-testing/testdata/golden/<TODAY'S-DATE>/1m_staging_1.avro.golden
    //    4. Update the value of 'goldenLocation' below to the new path

    // 20 shards test
    // Golden output is the output from e2e_test_outputs/3a73a3c1a29ea67ed0f89b98669cf399
    String goldenLocation = "testdata/golden/2022_08_25/1m_staging_1.avro.golden";
    String outputDataPath =
        String.format("e2e_test_outputs/%s/%s", KOKORO_BUILD_ID, "1m_staging_1_sharded.avro.test");

    CreateJobRequest createJobRequest =
        createJobRequest(
            DATA_BUCKET,
            INPUT_DATA_PREFIX,
            DATA_BUCKET,
            outputDataPath,
            Optional.of(DATA_BUCKET),
            Optional.of(OUTPUT_DOMAIN_PREFIX),
            /* debugPrivacyBudgetLimit= */ Optional.of(String.valueOf(Integer.MAX_VALUE)));

    JsonNode result = submitJobAndWaitForResult(createJobRequest, completionTimeout);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());

    // Read output avro from s3.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromS3(s3BlobStorageClient, avroResultsFileReader, DATA_BUCKET, outputDataPath);
    ImmutableList<AggregatedFact> goldenAggregatedFacts =
        readResultsFromS3(s3BlobStorageClient, avroResultsFileReader, DATA_BUCKET, goldenLocation);

    MapDifference<BigInteger, AggregatedFact> diffs =
        ResultDiffer.diffResults(aggregatedFacts.stream(), goldenAggregatedFacts.stream());
    assertWithMessage(
            String.format(
                "Found (%s) diffs between left(test) and right(golden).",
                diffs.entriesDiffering().size()))
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
    }
  }
}
