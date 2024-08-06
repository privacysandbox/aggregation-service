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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.protocol.avro.AvroDebugResultsReader;
import com.google.aggregate.protocol.avro.AvroDebugResultsReaderFactory;
import com.google.aggregate.protocol.avro.AvroDebugResultsRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.protobuf.util.JsonFormat;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/** Smoke test base class */
public abstract class SmokeTestBase {

  public static final String ENV_ATTRIBUTION_REPORT_TO = System.getenv("ATTRIBUTION_REPORT_TO");
  public static final String ENV_REPORTING_SITE = System.getenv("REPORTING_SITE");
  public static final String DEFAULT_ATTRIBUTION_REPORT_TO = "https://subdomain.fakeurl.com";
  public static final String DEFAULT_REPORTING_SITE = "https://fakeurl.com";

  public static final String FRONTEND_CLOUDFUNCTION_URL =
      System.getenv("FRONTEND_CLOUDFUNCTION_URL");
  public static final String KOKORO_BUILD_ID = System.getenv("KOKORO_BUILD_ID");
  public static final String API_GATEWAY_VERSION = "v1alpha";
  public static final String GET_JOB_URI_PATTERN = "%s/%s/getJob?job_request_id=%s";

  public static final String CREATE_JOB_URI_PATTERN = "%s/%s/createJob";
  public static final String FRONTEND_API = System.getenv("FRONTEND_API");
  public static final String API_GATEWAY_STAGE = "stage";
  public static final String GCP_ACCESS_TOKEN = System.getenv("GCP_ACCESS_TOKEN");
  public static final String DEFAULT_DEPLOY_SA =
      "deploy-sa@ps-msmt-aggserv-test.iam.gserviceaccount.com";
  public static final String DEFAULT_TEST_DATA_BUCKET = "test_reports_data";
  public static final String DEFAULT_PROJECT_ID = "ps-msmt-aggserv-test";
  public static final String DEFAULT_ENVIRONMENT_NAME = "continuous_mp";

  protected CreateJobRequest createJobRequest;

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      String jobId,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      long totalReportsCount,
      int reportErrorThreshold) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket,
            inputDataBlobPrefix,
            outputDataBlobBucket,
            outputDataBlobPrefix,
            jobId)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                false, outputDomainBucketName, outputDomainPrefix, Optional.of(totalReportsCount), reportErrorThreshold))
        .build();
  }

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket, inputDataBlobPrefix, outputDataBlobBucket, outputDataBlobPrefix)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                debugRun,
                outputDomainBucketName,
                outputDomainPrefix,
                /* inputReportCount= */ Optional.empty(),
                /* reportErrorThreshold= */ 0))
        .build();
  }

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      Boolean debugRun,
      String jobId,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket,
            inputDataBlobPrefix,
            outputDataBlobBucket,
            outputDataBlobPrefix,
            jobId)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                debugRun, outputDomainBucketName, outputDomainPrefix, Optional.empty(), 0))
        .build();
  }

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      long totalReportsCount,
      int reportErrorThreshold) {
    return createDefaultJobRequestBuilder(
        inputDataBlobBucket, inputDataBlobPrefix, outputDataBlobBucket, outputDataBlobPrefix)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                debugRun,
                outputDomainBucketName,
                outputDomainPrefix,
                Optional.of(totalReportsCount),
                reportErrorThreshold))
        .build();
  }

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      String jobId,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    return createDefaultJobRequestBuilder(
        inputDataBlobBucket,
        inputDataBlobPrefix,
        outputDataBlobBucket,
        outputDataBlobPrefix,
        jobId)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                false,
                outputDomainBucketName,
                outputDomainPrefix,
                /* inputReportCount= */ Optional.empty(),
                /* reportErrorThreshold= */ 0))
        .build();
  }

  private static CreateJobRequest.Builder createDefaultJobRequestBuilder(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      String jobIdString) {
    if (FRONTEND_CLOUDFUNCTION_URL == null) {
      throw new IllegalStateException(
          "Required environment variable FRONTEND_CLOUDFUNCTION_URL not set");
    }
    return CreateJobRequest.newBuilder()
        .setJobRequestId(jobIdString)
        .setInputDataBucketName(inputDataBlobBucket)
        .setInputDataBlobPrefix(inputDataBlobPrefix)
        .setOutputDataBucketName(outputDataBlobBucket)
        .setOutputDataBlobPrefix(outputDataBlobPrefix)
        .setPostbackUrl("fizz.com/api/buzz")
        .putAllJobParameters(ImmutableMap.of());
  }

  public static CreateJobRequest createJobRequestWithAttributionReportTo(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket, inputDataBlobPrefix, outputDataBlobBucket, outputDataBlobPrefix)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                false, outputDomainBucketName, outputDomainPrefix, /* reportErrorThreshold= */ Optional.empty(), 0))
        .build();
  }

  public static CreateJobRequest createJobRequestWithReportingSite(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket, inputDataBlobPrefix, outputDataBlobBucket, outputDataBlobPrefix)
        .putAllJobParameters(
            getJobParamsWithReportingSite(
                false, outputDomainBucketName, outputDomainPrefix, /* reportErrorThreshold= */ 100))
        .build();
  }

  private static CreateJobRequest.Builder createDefaultJobRequestBuilder(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix) {
    return createDefaultJobRequestBuilder(
        inputDataBlobBucket,
        inputDataBlobPrefix,
        outputDataBlobBucket,
        outputDataBlobPrefix,
        UUID.randomUUID().toString());
  }

  public static JsonNode sendRequest(String uri, String method, Optional<String> body)
      throws IOException {
    HttpClient client = HttpClients.custom().build();
    HttpResponse response = client.execute(toApacheHttpRequest(uri, method, body));
    String content = EntityUtils.toString(response.getEntity());

    return new ObjectMapper().readTree(content);
  }

  public static JsonNode submitJobAndWaitForResult(
      CreateJobRequest createJobRequest, Duration timeout)
      throws IOException, InterruptedException {
    return submitJob(createJobRequest, timeout, true);
  }

  public static JsonNode submitJob(
      CreateJobRequest createJobRequest, Duration timeout, boolean waitForComplete)
      throws IOException, InterruptedException {
    String job = JsonFormat.printer().print(createJobRequest);

    String createJobAPIUri =
        String.format(CREATE_JOB_URI_PATTERN, FRONTEND_CLOUDFUNCTION_URL, API_GATEWAY_VERSION);
    callCreateJobAPI(createJobAPIUri, job);

    if (waitForComplete) {
      // Wait for job to complete.
      waitForJobDone(createJobRequest, timeout);
    } else {
      // Don't wait for job to complete. Just sleep.
      Thread.sleep(timeout.toMillis());
    }

    String getJobAPIUri =
        String.format(
            GET_JOB_URI_PATTERN,
            FRONTEND_CLOUDFUNCTION_URL,
            API_GATEWAY_VERSION,
            createJobRequest.getJobRequestId());

    return getJobResult(getJobAPIUri);
  }

  public static HttpUriRequest toApacheHttpRequest(String uri, String method, Optional<String> body)
      throws IOException {
    var builder = RequestBuilder.create(method);

    builder.setUri(uri);
    builder.addHeader("Authorization", String.format("Bearer %s", GCP_ACCESS_TOKEN));

    if (body.isPresent()) {
      var entity = new StringEntity(body.get());
      builder.setEntity(entity);
    }
    return builder.build();
  }

  protected abstract HttpUriRequest composeRequest(String uri, String method, Optional<String> body)
      throws IOException;

  protected abstract String composeGetJobUri();

  protected abstract String composeCreateJobUri();

  protected JsonNode submitAndWaitForResult(Duration timeout, boolean waitForComplete)
      throws IOException, InterruptedException {
    String job = JsonFormat.printer().print(createJobRequest);
    String createJobAPIUri = composeCreateJobUri();

    callCreateJobAPI(createJobAPIUri, job);
    if (waitForComplete) {
      // Wait for job to complete.
      waitForJobDone(createJobRequest, timeout);
    } else {
      // Don't wait for job to complete. Just sleep.
      Thread.sleep(timeout.toMillis());
    }

    return getJobResult(composeGetJobUri());
  }

  public static void waitForJobCompletions(List<CreateJobRequest> jobRequests, Duration timeout)
      throws IOException, InterruptedException {
    waitForJobCompletions(jobRequests, timeout, true);
  }

  public static void waitForJobCompletions(
      List<CreateJobRequest> jobRequests, Duration timeout, boolean log)
      throws IOException, InterruptedException {
    Instant waitMax = Instant.now().plus(timeout);
    while (!jobRequests.isEmpty() && Instant.now().isBefore(waitMax)) {
      Iterator<CreateJobRequest> jobRequestIterator = jobRequests.iterator();
      while (jobRequestIterator.hasNext()) {
        CreateJobRequest request = jobRequestIterator.next();
        if (SmokeTestBase.isCompleted(request)) {
          jobRequestIterator.remove();
        }
      }
      if (!jobRequests.isEmpty()) {
        List<String> remainingJobs =
            jobRequests.stream()
                .map(CreateJobRequest::getJobRequestId)
                .collect(Collectors.toList());
        if (log) {
          System.out.println(
              "Waiting for worker to process... Remaining jobs: "
                  + remainingJobs
                  + " Remaining time: "
                  + Duration.between(Instant.now(), waitMax));
        }
        Thread.sleep(10_000);
      }
    }
    if (!jobRequests.isEmpty()) {
      throw new IllegalStateException("Jobs did not finish in time. Timeout was " + timeout);
    }
  }

  public static void waitForJobDone(CreateJobRequest createJobRequest, Duration timeout)
      throws IOException, InterruptedException {
    List<CreateJobRequest> jobRequests = new ArrayList<>(Arrays.asList(createJobRequest));
    waitForJobCompletions(jobRequests, timeout);
  }

  public static boolean isCompleted(CreateJobRequest createJobRequest)
      throws IOException, InterruptedException {
    String uri =
        String.format(
            GET_JOB_URI_PATTERN,
            FRONTEND_CLOUDFUNCTION_URL,
            API_GATEWAY_VERSION,
            createJobRequest.getJobRequestId());
    JsonNode response = callGetJobAPI(uri);

    return response.get("job_status").asText().equals("FINISHED");
  }

  public static JsonNode getJobResult(CreateJobRequest createJobRequest)
      throws IOException, InterruptedException {
    String uri =
        String.format(
            GET_JOB_URI_PATTERN,
            FRONTEND_CLOUDFUNCTION_URL,
            API_GATEWAY_VERSION,
            createJobRequest.getJobRequestId());
    return callGetJobAPI(uri);
  }

  public static JsonNode getJobResult(String uri) throws IOException {
    return callGetJobAPI(uri);
  }

  public static void createJob(CreateJobRequest createJobRequest)
      throws IOException, InterruptedException {
    String job = JsonFormat.printer().print(createJobRequest);

    String createJobAPIUri =
        String.format(CREATE_JOB_URI_PATTERN, FRONTEND_CLOUDFUNCTION_URL, API_GATEWAY_VERSION);
    callCreateJobAPI(createJobAPIUri, job);
  }

  public static JsonNode callCreateJobAPI(String uri, String requestBody) throws IOException {
    System.out.println(String.format("callCreateJobAPI (%s) request: %s", uri, requestBody));
    JsonNode response = sendRequest(uri, "POST", Optional.ofNullable(requestBody));
    System.out.println(String.format("callCreateJobAPI response: %s", response.toPrettyString()));

    return response;
  }

  public static JsonNode callGetJobAPI(String uri) throws IOException {
    System.out.println(String.format("callGetJobAPI (%s) request", uri));
    JsonNode response = sendRequest(uri, "GET", Optional.empty());
    System.out.println(String.format("callGetJobAPI response: %s", response.toPrettyString()));
    return response;
  }

  /**
   * TEST_DATA_BUCKET is used for storing the input data and the output results. If TEST_DATA_BUCKET
   * is not set in the environment variable, DEFAULT_TEST_DATA_BUCKET is used.
   */
  public static String getTestDataBucket() {
    String testDataBucket = System.getenv("TEST_DATA_BUCKET");
    return testDataBucket != null ? testDataBucket : DEFAULT_TEST_DATA_BUCKET;
  }

  public static String getTestDataBucket(String defaultBucket) {
    String testDataBucket = System.getenv("TEST_DATA_BUCKET");
    return testDataBucket != null ? testDataBucket : defaultBucket;
  }

  public static String getTestProjectId() {
    String testProjectId = System.getenv("ADTECH_PROJECT_ID");
    return testProjectId != null ? testProjectId : DEFAULT_PROJECT_ID;
  }

  public static String getEnvironmentName() {
    String environmentName = System.getenv("ENVIRONMENT_NAME");
    return environmentName != null ? environmentName : DEFAULT_ENVIRONMENT_NAME;
  }

  public static String getTestServiceAccount() {
    String testSA = System.getenv("GCP_IMPERSONATED_SERVICE_ACCOUNT");
    return testSA != null ? testSA : DEFAULT_DEPLOY_SA;
  }

  private static AvroDebugResultsReader getReader(
      AvroDebugResultsReaderFactory readerFactory, Path avroFile) throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  protected static <T extends BlobStorageClient> ImmutableList<AggregatedFact> readResultsFromCloud(
      T blobStorageClient,
      AvroResultsFileReader avroResultsFileReader,
      String outputBucket,
      String outputPrefix)
      throws Exception {
    Path tempResultFile = Files.createTempFile(/* prefix= */ "results", /* suffix= */ "avro");
    try (InputStream resultStream =
            blobStorageClient.getBlob(
                DataLocation.ofBlobStoreDataLocation(
                    BlobStoreDataLocation.create(outputBucket, outputPrefix)));
        OutputStream outputStream = Files.newOutputStream(tempResultFile)) {
      ByteStreams.copy(resultStream, outputStream);
      outputStream.flush();
    }

    ImmutableList<AggregatedFact> facts = avroResultsFileReader.readAvroResultsFile(tempResultFile);
    Files.deleteIfExists(tempResultFile);

    return facts;
  }

  protected static <T extends BlobStorageClient>
      ImmutableList<AggregatedFact> readDebugResultsFromCloud(
          T blobStorageClient,
          AvroDebugResultsReaderFactory readerFactory,
          String outputBucket,
          String outputPrefix)
          throws Exception {
    Stream<AvroDebugResultsRecord> writtenResults;
    Path tempResultFile = Files.createTempFile(/* prefix= */ "debug_results", /* suffix= */ "avro");
    try (InputStream resultStream =
            blobStorageClient.getBlob(
                DataLocation.ofBlobStoreDataLocation(
                    BlobStoreDataLocation.create(outputBucket, outputPrefix)));
        OutputStream outputStream = Files.newOutputStream(tempResultFile)) {
      ByteStreams.copy(resultStream, outputStream);
      outputStream.flush();
    }
    AvroDebugResultsReader reader = getReader(readerFactory, tempResultFile);
    writtenResults = reader.streamRecords();
    ImmutableList<AggregatedFact> facts =
        writtenResults
            .map(
                writtenResult ->
                    AggregatedFact.create(
                        writtenResult.bucket(),
                        writtenResult.metric(),
                        writtenResult.unnoisedMetric()))
            .collect(toImmutableList());
    Files.deleteIfExists(tempResultFile);
    return facts;
  }

  protected static <T extends BlobStorageClient> boolean checkFileExists(
      T blobStorageClient, String bucket, String key) throws Exception {
    try (InputStream dataStream =
        blobStorageClient.getBlob(
            DataLocation.ofBlobStoreDataLocation(BlobStoreDataLocation.create(bucket, key)))) {
      return true;
    } catch (BlobStorageClientException e) {
      // TODO: Add NoSuchKeyException when integrated with AWS.
      return false;
    } catch (NullPointerException e) {
      // GCSBlobStorageClient object is null when a file doesn't exist.
      return false;
    }
  }

  private static ImmutableMap<String, String> getJobParamsWithReportingSite(
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      int reportErrorThresholdPercentage) {
    ImmutableMap.Builder<String, String> jobParams = ImmutableMap.builder();
    jobParams.put("reporting_site", getReportingSite());
    if (debugRun) {
      jobParams.put("debug_run", "true");
    }
    jobParams.put(
        "report_error_threshold_percentage", String.valueOf(reportErrorThresholdPercentage));
    if (outputDomainPrefix.isPresent() && outputDomainBucketName.isPresent()) {
      jobParams.put("output_domain_blob_prefix", outputDomainPrefix.get());
      jobParams.put("output_domain_bucket_name", outputDomainBucketName.get());
      return jobParams.build();
    } else if (outputDomainPrefix.isEmpty() && outputDomainBucketName.isEmpty()) {
      return jobParams.build();
    } else {
      throw new IllegalStateException(
          "outputDomainPrefix and outputDomainBucketName must both be provided or both be empty.");
    }
  }

  private static ImmutableMap<String, String> getJobParamsWithAttributionReportTo(
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      Optional<Long> inputReportCountOptional,
      int reportErrorThresholdPercentage) {
    ImmutableMap.Builder<String, String> jobParams = ImmutableMap.builder();
    jobParams.put("attribution_report_to", getAttributionReportTo());

    if (debugRun) {
      jobParams.put("debug_run", "true");
    }
    inputReportCountOptional.ifPresent(
        inputReportCount ->
            jobParams.put(JobUtils.JOB_PARAM_INPUT_REPORT_COUNT, String.valueOf(inputReportCount)));
    jobParams.put(
        "report_error_threshold_percentage", String.valueOf(reportErrorThresholdPercentage));
    if (outputDomainPrefix.isPresent() && outputDomainBucketName.isPresent()) {
      jobParams.put("output_domain_blob_prefix", outputDomainPrefix.get());
      jobParams.put("output_domain_bucket_name", outputDomainBucketName.get());
      return jobParams.build();
    } else if (outputDomainPrefix.isEmpty() && outputDomainBucketName.isEmpty()) {
      return jobParams.build();
    } else {
      throw new IllegalStateException(
          "outputDomainPrefix and outputDomainBucketName must both be provided or both be empty.");
    }
  }

  private static String getAttributionReportTo() {
    if (ENV_ATTRIBUTION_REPORT_TO != null) {
      return ENV_ATTRIBUTION_REPORT_TO;
    }
    return DEFAULT_ATTRIBUTION_REPORT_TO;
  }

  private static String getReportingSite() {
    if (ENV_REPORTING_SITE != null) {
      return ENV_REPORTING_SITE;
    }
    return DEFAULT_REPORTING_SITE;
  }

  protected static void checkJobExecutionResult(
      JsonNode result, String returnCode, int errorCount) {
    assertThat(result.get("result_info").get("return_code").asText()).isEqualTo(returnCode);
    if (errorCount == 0) {
      assertThat(result.get("result_info").get("error_summary").get("error_counts").isEmpty())
          .isTrue();
    } else {
      assertThat(
              result
                  .get("result_info")
                  .get("error_summary")
                  .get("error_counts")
                  .get(0)
                  .get("count")
                  .asInt())
          .isEqualTo(errorCount);
    }
  }
}
