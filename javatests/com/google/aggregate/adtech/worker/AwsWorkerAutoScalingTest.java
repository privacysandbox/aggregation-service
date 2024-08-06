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

import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.KOKORO_BUILD_ID;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.submitJob;
import static com.google.aggregate.adtech.worker.AwsWorkerContinuousTestHelper.waitForJobCompletions;

import com.google.acai.Acai;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.PartialRequestBufferSize;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClientModule.S3UsePartialRequests;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;

@RunWith(JUnit4.class)
public class AwsWorkerAutoScalingTest {
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Rule public final TestName name = new TestName();

  private static final Duration SUBMIT_JOB_TIMEOUT = Duration.of(1, ChronoUnit.SECONDS);
  private static final Duration COMPLETION_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);
  private static final Duration SCALE_ACTION_COMPLETION_TIMEOUT =
      Duration.of(5, ChronoUnit.MINUTES);

  private static final String DEFAULT_TEST_DATA_BUCKET = "aggregation-service-testing";
  private static final String TEST_DATA_S3_KEY_PREFIX = "generated-test-data";

  // Input data and domain were generated in continuous_auto_scaling_test in shared_e2e.sh.
  // Generate only one set of input reports and domain for 5 job requests
  // because it takes long time to generate 100k reports and auto-scale testing
  // doesn't check request success. If PBS is enabled, only first request would pass without error,
  // and the rest 4 jobs would have PRIVACY_BUDGET_EXHAUSTED error due to using same data.
  // Use 100k reports because if the report size is too small, the job completes very fast
  // and almost no job waits in queue, which would not trigger auto-scaling.
  private static final String INPUT_DATA_PATH =
      String.format(
          "%s/%s/test-inputs/100k_auto_scale_test_input.avro",
          TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);
  private static final String INPUT_DOMAIN_PATH =
      String.format(
          "%s/%s/test-inputs/100k_auto_scale_test_domain.avro",
          TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID);

  public static final String AUTO_SCALING_GROUP = System.getenv("AUTO_SCALING_GROUP");
  private static final Integer CONCURRENT_JOBS = 5;
  private static final Integer MIN_INSTANCES = 1;

  private static String getTestDataBucket() {
    if (System.getenv("TEST_DATA_BUCKET") != null) {
      return System.getenv("TEST_DATA_BUCKET");
    }
    return DEFAULT_TEST_DATA_BUCKET;
  }

  @Inject AutoScalingClient autoScalingClient;

  @Test
  public void autoscalingE2ETest() throws Exception {
    ArrayList<CreateJobRequest> jobRequests = new ArrayList<>();
    for (int jobCount = 0; jobCount < CONCURRENT_JOBS; jobCount++) {
      jobRequests.add(createE2EJob(jobCount));
    }

    // Check for scale-out to match the concurrentJobs
    // TODO(b/251508750) Make job queue time deterministic.
    waitForScaleAction(true);

    // Wait for all jobs to complete
    waitForJobCompletions(jobRequests, COMPLETION_TIMEOUT);

    // Check for scale-in to minimum instances
    waitForScaleAction(false);
  }

  private CreateJobRequest createE2EJob(Integer jobCount) throws Exception {
    String outputFile = String.format("100k_auto_scale_job_%d.avro.test", jobCount);
    String outputDataPath =
        String.format(
            "%s/test-outputs/%s/%s", TEST_DATA_S3_KEY_PREFIX, KOKORO_BUILD_ID, outputFile);
    CreateJobRequest createJobRequest =
        AwsWorkerContinuousTestHelper.createJobRequestWithAttributionReportTo(
            getTestDataBucket(),
            INPUT_DATA_PATH,
            getTestDataBucket(),
            outputDataPath,
            /* jobId= */ getClass().getSimpleName() + "::" + name.getMethodName() + "_" + jobCount,
            Optional.of(getTestDataBucket()),
            Optional.of(INPUT_DOMAIN_PATH));
    submitJob(createJobRequest, SUBMIT_JOB_TIMEOUT, false);
    return createJobRequest;
  }

  private void waitForScaleAction(Boolean isScaleOut) throws InterruptedException {
    Instant waitMax = Instant.now().plus(SCALE_ACTION_COMPLETION_TIMEOUT);
    Boolean scaleSuccessful = false;
    Integer currentInstanceCount = 0;

    while (!scaleSuccessful && Instant.now().isBefore(waitMax)) {
      currentInstanceCount = getInstanceCount();
      System.out.println(
          "Verifying instance count. Is scale out:"
              + isScaleOut
              + " Current instance count:"
              + currentInstanceCount);
      if ((!isScaleOut && currentInstanceCount == MIN_INSTANCES)
          || (isScaleOut && currentInstanceCount > MIN_INSTANCES)) {
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
              + currentInstanceCount);
    }
  }

  private Integer getInstanceCount() {
    DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest =
        DescribeAutoScalingGroupsRequest.builder()
            .autoScalingGroupNames(AUTO_SCALING_GROUP)
            .build();
    DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse =
        autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
    List<Instance> instanceDetails =
        describeAutoScalingGroupsResponse.autoScalingGroups().get(0).instances();
    return instanceDetails.size();
  }

  private static class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      bind(AutoScalingClient.class)
          .toInstance(
              AutoScalingClient.builder()
                  .httpClient(UrlConnectionHttpClient.builder().build())
                  .build());
      bind(Boolean.class).annotatedWith(S3UsePartialRequests.class).toInstance(false);
      bind(Integer.class).annotatedWith(PartialRequestBufferSize.class).toInstance(20);
    }
  }
}
