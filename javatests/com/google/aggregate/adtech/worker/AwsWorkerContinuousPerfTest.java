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
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.getAndWriteStopwatchesFromS3;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJobAndWaitForResult;
import static com.google.common.truth.Truth.assertThat;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.nio.file.Path;
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

@RunWith(JUnit4.class)
public class AwsWorkerContinuousPerfTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();

  private static final Duration completionTimeout = Duration.of(60, ChronoUnit.MINUTES);
  private static final String TESTING_BUCKET = "aggregation-service-testing";
  private static final Path stopwatchWriteLocation = Path.of(System.getenv("STOPWATCH_TEMP_FILE"));

  // Input data generated with the following command:
  // bazel run java/com/google/aggregate/simulation:SimulationRunner -- \
  //   --aggregatable_report_file_path $PWD/1m_staging_2022_05_21.avro \
  //   --num_reports 1000000 \
  //   --num_encryption_keys 3 \
  //   --encryption_key_service CLOUD \
  //   --public_key_vending_uri \
  // https://jykzugjj3g.execute-api.us-west-2.amazonaws.com/stage/v1alpha/publicKeys \
  //   --distribution FILE \
  //   --distribution_file_path $PWD/1m_staging_1_integer_buckets.txt \
  //   --generate_output_domain \
  //   --output_domain_path $PWD/1m_staging_2022_08_08.avro
  // Where the distribution file used is
  //  s3://aggregation-service-testing/testdata/1m_staging_1_integer_buckets.txt
  // Data then sharded with:
  // Reports:
  // bazel run //java/com/google/aggregate/tools/shard:AvroShard -- \
  //   --input $PWD/1m_staging_2022_05_21.avro \
  //   --output_dir $PWD/1m_staging_2022_05_21_sharded \
  //   --num_shards 20
  // Domain:
  // bazel run //java/com/google/aggregate/tools/shard:AvroShard -- \
  //   --input $PWD/1m_staging_2022_08_08.avro \
  //   --output_dir $PWD/1m_staging_2022_08_08_sharded \
  //   --num_shards 20 \
  //   --domain

  private static final String INPUT_REPORTS_PREFIX = "testdata/1m_staging_2022_05_21_sharded/shard";
  private static final String OUTPUT_DOMAIN_PREFIX =
      "testdata/1m_staging_2022_08_08_sharded_domain/shard";

  @Inject S3BlobStorageClient s3BlobStorageClient;

  @Test
  public void e2ePerfTest() throws Exception {
    // End to end perf testing:
    //    1. Use createJob request to API gateway with the test inputs pre-uploaded to s3 bucket.
    //    2. Call getJob API to retrieve result information.
    //    3. Assert on result status (SUCCESS)
    //    4. Stopwatch files are retrieved from S3 and written locally to be picked up by perfgate.
    String outputDataPath =
        // This output isn't checked.
        String.format(
            "e2e_test_outputs/%s/%s", KOKORO_BUILD_ID, "createJobE2EperfTest-reports1m.avro");

    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            TESTING_BUCKET,
            INPUT_REPORTS_PREFIX,
            TESTING_BUCKET,
            outputDataPath,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName(),
            /* outputDomainBucketName= */ Optional.of(TESTING_BUCKET),
            /* outputDomainPrefix= */ Optional.of(OUTPUT_DOMAIN_PREFIX));

    JsonNode result = submitJobAndWaitForResult(createJobRequest, completionTimeout);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());

    // TODO(b/241940276) Retrieve domain stopwatch files from S3 and written locally to be picked
    // up by perfgate.
    // Read Stopwatches from S3 and write to file.
    getAndWriteStopwatchesFromS3(
        s3BlobStorageClient,
        /* bucket= */ TESTING_BUCKET,
        /* key= */ "stopwatches",
        /* exportLocation= */ stopwatchWriteLocation);
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
