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

import static com.google.aggregate.adtech.worker.SmokeTestBase.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.SmokeTestBase.createJob;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getJobResult;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;
import static com.google.aggregate.adtech.worker.SmokeTestBase.waitForJobCompletions;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.acai.Acai;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.AbstractModule;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** GCP KHS loadtest implementation */
@RunWith(JUnit4.class)
public final class GcpWorkerKhsLoadtest {

  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public TestName name = new TestName();

  private static final String KHS_LOADTEST_DATA_BUCKET = "loadtest_data";
  private static final int NUM_RUNS = 5;

  private static final Duration COMPLETION_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);

  @Before
  public void checkBuildEnv() {
    if (KOKORO_BUILD_ID == null) {
      throw new IllegalStateException("KOKORO_BUILD_ID env var must be set.");
    }
  }

  /** Run Aggregation job for KHS loadtest. */
  @Test
  public void aggregateKhsLoadTest() throws Exception {
    ArrayList<CreateJobRequest> jobRequests = new ArrayList<>(NUM_RUNS);
    ArrayList<CreateJobRequest> jobRequestsDeepCopy = new ArrayList<>(NUM_RUNS);

    for (int i = 1; i <= NUM_RUNS; i++) {
      var inputKey =
          String.format("test-data/%s/test-inputs/loadtest_report.avro", KOKORO_BUILD_ID);
      var domainKey =
          String.format("test-data/%s/test-inputs/loadtest_domain.avro", KOKORO_BUILD_ID);
      var outputKey =
          String.format("test-data/%s/test-outputs/loadtest_%s_output.avro", KOKORO_BUILD_ID, i);
      CreateJobRequest createJobRequest =
          SmokeTestBase.createJobRequestWithAttributionReportTo(
              getTestDataBucket(KHS_LOADTEST_DATA_BUCKET),
              inputKey,
              getTestDataBucket(KHS_LOADTEST_DATA_BUCKET),
              outputKey,
              /* debugRun= */ true,
              /* jobId= */ UUID.randomUUID().toString(),
              /* outputDomainBucketName= */ Optional.of(
                  getTestDataBucket(KHS_LOADTEST_DATA_BUCKET)),
              /* outputDomainPrefix= */ Optional.of(domainKey));
      createJob(createJobRequest);

      jobRequests.add(createJobRequest);
      jobRequestsDeepCopy.add(createJobRequest);
    }

    waitForJobCompletions(jobRequestsDeepCopy, COMPLETION_TIMEOUT);

    for (int i = 1; i <= NUM_RUNS; i++) {
      JsonNode result = getJobResult(jobRequests.get(i - 1));

      assertThat(result.get("result_info").get("return_code").asText())
          .isEqualTo(AggregationWorkerReturnCode.SUCCESS.name());

      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();
    }
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
