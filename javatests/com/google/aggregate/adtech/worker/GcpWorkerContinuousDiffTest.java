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

import static com.google.aggregate.adtech.worker.SmokeTestBase.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;
import static com.google.aggregate.adtech.worker.SmokeTestBase.readResultsFromCloud;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.scp.operator.protos.frontend.api.v1.ReturnCodeProto.ReturnCode.SUCCESS;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.tools.diff.ResultDiffer;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.gcp.GcsBlobStorageClient;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GcpWorkerContinuousDiffTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);

  @Inject GcsBlobStorageClient gcsBlobStorageClient;
  @Inject AvroResultsFileReader avroResultsFileReader;

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  @Test
  public void e2eDiffTest() throws Exception {

    String goldenLocation = "testdata/golden/2022_10_18/10k_diff_test.avro.golden";

    String inputKey =
        String.format("%s/test-inputs/10k_diff_test_input_sharded/", SmokeTestBase.KOKORO_BUILD_ID);
    String domainKey =
        String.format("%s/test-inputs/diff_test_domain_sharded/", SmokeTestBase.KOKORO_BUILD_ID);
    String outputDataPrefix =
        String.format(
            "%s/test-outputs/10k_diff_test_output.avro.result", SmokeTestBase.KOKORO_BUILD_ID);

    CreateJobRequest createJobRequest =
        SmokeTestBase.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            inputKey,
            getTestDataBucket(),
            outputDataPrefix,
            Optional.of(getTestDataBucket()),
            Optional.of(domainKey));

    JsonNode result = SmokeTestBase.submitJobAndWaitForResult(createJobRequest, COMPLETION_TIMEOUT);

    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(SUCCESS.name());
    assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
        .isTrue();

    // Read output avro from storage.
    ImmutableList<AggregatedFact> aggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient,
            avroResultsFileReader,
            getTestDataBucket(),
            outputDataPrefix + "-1-of-1");
    ImmutableList<AggregatedFact> goldenAggregatedFacts =
        readResultsFromCloud(
            gcsBlobStorageClient, avroResultsFileReader, getTestDataBucket(), goldenLocation);

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
