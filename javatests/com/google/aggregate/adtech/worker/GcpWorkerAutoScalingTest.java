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
import static com.google.aggregate.adtech.worker.SmokeTestBase.getEnvironmentName;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestDataBucket;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestProjectId;
import static com.google.aggregate.adtech.worker.SmokeTestBase.getTestServiceAccount;

import com.google.acai.Acai;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesClient.AggregatedListPagedResponse;
import com.google.cloud.compute.v1.InstancesScopedList;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GcpWorkerAutoScalingTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  private static final Duration SUBMIT_JOB_TIMEOUT = Duration.of(1, ChronoUnit.SECONDS);
  private static final Duration SCALE_ACTION_COMPLETION_TIMEOUT =
      Duration.of(20, ChronoUnit.MINUTES);
  private static final Duration COMPLETION_TIMEOUT = Duration.of(15, ChronoUnit.MINUTES);
  private static final Integer MIN_INSTANCES = 1;
  public static final int CONCURRENT_JOBS = 5;

  @Inject InstancesClient gcpInstancesClient;

  @Test
  public void autoscalingE2ETest() throws Exception {
    String inputDataPrefix =
        String.format("%s/test-inputs/100k_auto_scale_test_input.avro", KOKORO_BUILD_ID);
    String domainDataPrefix =
        String.format("%s/test-inputs/100k_auto_scale_test_domain.avro", KOKORO_BUILD_ID);

    List<CreateJobRequest> jobRequests = new ArrayList<>();
    for (int jobNum = 0; jobNum < CONCURRENT_JOBS; jobNum++) {
      String outputFile = String.format("100k_auto_scale_job_%d.avro.test", jobNum);
      String outputDataPrefix = String.format("%s/test-outputs/%s", KOKORO_BUILD_ID, outputFile);

      CreateJobRequest jobRequest =
          SmokeTestBase.createJobRequestWithAttributionReportTo(
              getTestDataBucket(),
              inputDataPrefix,
              getTestDataBucket(),
              outputDataPrefix,
              Optional.of(getTestDataBucket()),
              Optional.of(domainDataPrefix));

      SmokeTestBase.submitJob(jobRequest, SUBMIT_JOB_TIMEOUT, false);

      jobRequests.add(jobRequest);
    }

    // Wait for instances expansion. "scale out".
    waitForInstanceScaleAction(true);

    SmokeTestBase.waitForJobCompletions(jobRequests, COMPLETION_TIMEOUT);

    // Wait for instance reduction. "scale in".
    waitForInstanceScaleAction(false);
  }

  private void waitForInstanceScaleAction(boolean isScaleOut) throws InterruptedException {
    Instant waitMax = Instant.now().plus(SCALE_ACTION_COMPLETION_TIMEOUT);
    boolean scaleSuccessful = false;
    int instanceCount = 0;

    while (!scaleSuccessful && Instant.now().isBefore(waitMax)) {
      instanceCount = getInstanceCount();
      System.out.println(
          "Verifying instance count. Is scale out: "
              + isScaleOut
              + ". Current instance count: "
              + instanceCount);
      if ((!isScaleOut && instanceCount == MIN_INSTANCES)
          || (isScaleOut && instanceCount > MIN_INSTANCES)) {
        scaleSuccessful = true;
      } else {
        System.out.println(
            "Waiting for autoscaling action ... Remaining time: "
                + Duration.between(Instant.now(), waitMax));
        Thread.sleep(10_000);
      }
    }

    if (!scaleSuccessful) {
      throw new IllegalStateException(
          "Scale action did not complete in time. Timeout was "
              + SCALE_ACTION_COMPLETION_TIMEOUT
              + ". Is scale out:"
              + isScaleOut
              + " Current instance count:"
              + instanceCount);
    }
  }

  private int getInstanceCount() {
    AggregatedListPagedResponse pagedResponse =
        gcpInstancesClient.aggregatedList(getTestProjectId());

    int instancesCount = 0;
    for (Entry<String, InstancesScopedList> entry : pagedResponse.iterateAll()) {
      instancesCount +=
          entry.getValue().getInstancesList().stream()
              .filter(i -> i.getName().contains(getEnvironmentName()))
              .count();
    }
    return instancesCount;
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
                .setScopes(List.of("https://www.googleapis.com/auth/devstorage.read_write"))
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

      try {
        bind(InstancesClient.class).toInstance(InstancesClient.create());
      } catch (IOException e) {
        throw new RuntimeException("Unable to instantiate GCP Instances client: ", e);
      }
    }
  }
}
