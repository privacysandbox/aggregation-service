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

import static com.google.common.collect.ImmutableList.toImmutableList;

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
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.util.JsonFormat;
import com.google.scp.operator.cpio.blobstorageclient.BlobStorageClient.BlobStorageClientException;
import com.google.scp.operator.cpio.blobstorageclient.aws.S3BlobStorageClient;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.protos.frontend.api.v1.CreateJobRequestProto.CreateJobRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsWorkerContinuousTestHelper {

  public static final Region AWS_API_GATEWAY_REGION = Region.US_EAST_1;
  public static final Region AWS_S3_BUCKET_REGION = Region.US_EAST_1;

  public static final String DEFAULT_ATTRIBUTION_REPORT_TO = "https://subdomain.fakeurl.com";
  public static final String DEFAULT_REPORTING_SITE = "https://fakeurl.com";

  public static final String ENV_ATTRIBUTION_REPORT_TO = System.getenv("ATTRIBUTION_REPORT_TO");
  public static final String ENV_REPORTING_SITE = System.getenv("REPORTING_SITE");
  public static final String FRONTEND_API = System.getenv("FRONTEND_API");
  public static final String KOKORO_BUILD_ID = System.getenv("KOKORO_BUILD_ID");

  public static final String CREATE_JOB_URI_PATTERN =
      "https://%s.execute-api.us-east-1.amazonaws.com/%s/%s/createJob";
  public static final String GET_JOB_URI_PATTERN =
      "https://%s.execute-api.us-east-1.amazonaws.com/%s/%s/getJob?job_request_id=%s";
  public static final String API_GATEWAY_STAGE = "stage";
  public static final String API_GATEWAY_VERSION = "v1alpha";
  public static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
  public static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
  public static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
  // Parses "s3://$bucket/$key" into two named groups: bucket and key.
  private static final Pattern S3_PATTERN = Pattern.compile("s3://(?<bucket>[^/]+)/(?<key>.+)");

  private static final String AVRO_EXT = ".avro";

  private static Matcher parseS3Uri(String inputUri) {
    var matcher = S3_PATTERN.matcher(inputUri);
    if (!matcher.find()) {
      throw new IllegalArgumentException(
          String.format("Unable to parse s3 URI '%s', expecting s3://bucket/key", inputUri));
    }
    return matcher;
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

  /** Helper for extracting a bucket name from an S3 URI. */
  public static String getS3Bucket(String s3Uri) {
    return parseS3Uri(s3Uri).group("bucket");
  }

  /** Helper for extracting an object key from an S3 URI. */
  public static String getS3Key(String s3Uri) {
    return parseS3Uri(s3Uri).group("key");
  }

  /** Helper for generating output file name from an output key assuming 1 shard in the output. */
  public static String getOutputFileName(String outputKey) {
    return getOutputFileName(outputKey, 1, 1);
  }

  /** Helper for generating output file name from an output key. */
  public static String getOutputFileName(String outputKey, int shardId, int numShard) {
    String outputSuffix = "-" + shardId + "-of-" + numShard;
    return outputKey.endsWith(AVRO_EXT)
        ? outputKey.substring(0, outputKey.length() - AVRO_EXT.length()) + outputSuffix + AVRO_EXT
        : outputKey + outputSuffix;
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
                /* reportErrorThresholdPercentage= */ 0,
                /* inputReportCount= */ Optional.empty(),
                /* filteringIds= */ Optional.empty()))
        .build();
  }

  public static CreateJobRequest createJobRequestWithReportingSite(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      String jobId,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix) {
    ImmutableMap<String, String> jobParams =
        getJobParamsWithReportingSite(
            false,
            outputDomainBucketName,
            outputDomainPrefix,
            /* reportErrorThresholdPercentage= */ 100,
            /* inputReportCount= */ Optional.empty());
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket,
            inputDataBlobPrefix,
            outputDataBlobBucket,
            outputDataBlobPrefix,
            jobId)
        .putAllJobParameters(jobParams)
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
                debugRun,
                outputDomainBucketName,
                outputDomainPrefix,
                /* reportErrorThresholdPercentage= */ 0,
                /* inputReportCount= */ Optional.empty(),
                /* filteringIds= */ Optional.empty()))
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
      Optional<String> outputDomainPrefix,
      int reportErrorThresholdPercentage,
      Optional<Long> inputReportCount,
      Set<UnsignedLong> filteringIds) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket,
            inputDataBlobPrefix,
            outputDataBlobBucket,
            outputDataBlobPrefix,
            jobId)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                debugRun,
                outputDomainBucketName,
                outputDomainPrefix,
                reportErrorThresholdPercentage,
                inputReportCount,
                Optional.of(filteringIds)))
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
      Optional<String> outputDomainPrefix,
      int reportErrorThresholdPercentage,
      Optional<Long> inputReportCount) {
    return createDefaultJobRequestBuilder(
            inputDataBlobBucket,
            inputDataBlobPrefix,
            outputDataBlobBucket,
            outputDataBlobPrefix,
            jobId)
        .putAllJobParameters(
            getJobParamsWithAttributionReportTo(
                debugRun,
                outputDomainBucketName,
                outputDomainPrefix,
                reportErrorThresholdPercentage,
                inputReportCount,
                /* filteringIds= */ Optional.empty()))
        .build();
  }

  private static CreateJobRequest.Builder createDefaultJobRequestBuilder(
      String inputDataBlobBucket,
      String inputDataBlobPrefix,
      String outputDataBlobBucket,
      String outputDataBlobPrefix,
      String jobIdString) {
    if (FRONTEND_API == null) {
      throw new IllegalStateException("Required environment variable FRONTEND_API not set");
    }
    if (AWS_SECRET_ACCESS_KEY == null || AWS_ACCESS_KEY_ID == null) {
      throw new IllegalStateException(
          "Required environment variable AWS_SECRET_ACCESS_KEY or AWS_ACCESS_KEY_ID not set");
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

  private static ImmutableMap<String, String> getJobParamsWithAttributionReportTo(
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      int reportErrorThresholdPercentage,
      Optional<Long> inputReportCountOptional,
      Optional<Set<UnsignedLong>> filteringIdsOptional) {
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
    } else if (!(outputDomainPrefix.isEmpty() && outputDomainBucketName.isEmpty())) {
      throw new IllegalStateException(
          "outputDomainPrefix and outputDomainBucketName must both be provided or both be empty.");
    }

    if (filteringIdsOptional.isPresent()) {
      Set<UnsignedLong> filteringIds = filteringIdsOptional.get();
      jobParams.put(
          JobUtils.JOB_PARAM_FILTERING_IDS,
          String.join(
              ",",
              filteringIds.stream()
                  .map(id -> id.toString())
                  .collect(ImmutableSet.toImmutableSet())));
    }

    return jobParams.build();
  }

  private static ImmutableMap<String, String> getJobParamsWithReportingSite(
      Boolean debugRun,
      Optional<String> outputDomainBucketName,
      Optional<String> outputDomainPrefix,
      int reportErrorThresholdPercentage,
      Optional<Long> inputReportCountOptional) {
    ImmutableMap.Builder<String, String> jobParams = ImmutableMap.builder();
    jobParams.put("reporting_site", getReportingSite());
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
        String.format(CREATE_JOB_URI_PATTERN, FRONTEND_API, API_GATEWAY_STAGE, API_GATEWAY_VERSION);
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
            FRONTEND_API,
            API_GATEWAY_STAGE,
            API_GATEWAY_VERSION,
            createJobRequest.getJobRequestId());

    return getJobResult(getJobAPIUri);
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
        if (AwsWorkerContinuousTestHelper.isCompleted(request)) {
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
    return getJobResult(createJobRequest).get("job_status").asText().equals("FINISHED");
  }

  public static JsonNode getJobResult(CreateJobRequest createJobRequest)
      throws IOException, InterruptedException {
    String uri =
        String.format(
            GET_JOB_URI_PATTERN,
            FRONTEND_API,
            API_GATEWAY_STAGE,
            API_GATEWAY_VERSION,
            createJobRequest.getJobRequestId());
    return callGetJobAPI(uri);
  }

  public static JsonNode getJobResult(String uri) throws IOException {
    return callGetJobAPI(uri);
  }

  /** Returns a list of AggregatedFact from the file matching the bucket and key. */
  public static ImmutableList<AggregatedFact> readResultsFromS3(
      S3BlobStorageClient s3BlobStorageClient,
      AvroResultsFileReader avroResultsFileReader,
      String bucket,
      String key)
      throws BlobStorageClientException, IOException {

    Path tempResultFile = Files.createTempFile(/* prefix= */ "results", /* suffix= */ "avro");

    try (InputStream resultStream =
            s3BlobStorageClient.getBlob(
                DataLocation.ofBlobStoreDataLocation(BlobStoreDataLocation.create(bucket, key)));
        OutputStream outputStream = Files.newOutputStream(tempResultFile)) {
      ByteStreams.copy(resultStream, outputStream);
      outputStream.flush();
    }

    ImmutableList<AggregatedFact> facts = avroResultsFileReader.readAvroResultsFile(tempResultFile);
    Files.deleteIfExists(tempResultFile);
    return facts;
  }

  /** Returns a list of AggregatedFacts from a list of files matching the bucket and prefix. */
  public static ImmutableList<AggregatedFact> readResultsFromMultipleFiles(
      S3BlobStorageClient s3BlobStorageClient,
      AvroResultsFileReader avroResultsFileReader,
      String bucket,
      String prefix)
      throws BlobStorageClientException, IOException {
    BlobStoreDataLocation blobsPrefixLocation = BlobStoreDataLocation.create(bucket, prefix);
    DataLocation prefixLocation = DataLocation.ofBlobStoreDataLocation(blobsPrefixLocation);
    ImmutableList<String> shardBlobs = s3BlobStorageClient.listBlobs(prefixLocation);

    ImmutableList.Builder<AggregatedFact> aggregatedFactBuilder = ImmutableList.builder();
    for (String shard : shardBlobs) {
      aggregatedFactBuilder.addAll(
          readResultsFromS3(
              s3BlobStorageClient, avroResultsFileReader, blobsPrefixLocation.bucket(), shard));
    }
    return aggregatedFactBuilder.build();
  }

  private static AvroDebugResultsReader getReader(
      AvroDebugResultsReaderFactory readerFactory, Path avroFile) throws Exception {
    return readerFactory.create(Files.newInputStream(avroFile));
  }

  public static ImmutableList<AggregatedFact> readDebugResultsFromS3(
      S3BlobStorageClient s3BlobStorageClient,
      AvroDebugResultsReaderFactory readerFactory,
      String outputBucket,
      String outputPrefix)
      throws Exception {

    Stream<AvroDebugResultsRecord> writtenResults;
    Path tempResultFile = Files.createTempFile(/* prefix= */ "debug_results", /* suffix= */ "avro");

    try (InputStream resultStream =
            s3BlobStorageClient.getBlob(
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

  /** Read stopwatches from S3 bucket and write them to a local file. */
  public static void getAndWriteStopwatchesFromS3(
      S3BlobStorageClient blobStorageClient, String bucket, String key, Path writeLocation)
      throws Exception {

    Path stopwatchFile = Files.createFile(writeLocation);
    try (InputStream resultStream =
            blobStorageClient.getBlob(
                DataLocation.ofBlobStoreDataLocation(BlobStoreDataLocation.create(bucket, key)));
        OutputStream outputStream = Files.newOutputStream(stopwatchFile)) {
      ByteStreams.copy(resultStream, outputStream);
      outputStream.flush();
    }
  }

  public static JsonNode callCreateJobAPI(String uri, String requestBody) throws IOException {
    System.out.println(String.format("callCreateJobAPI (%s) request: %s", uri, requestBody));

    JsonNode response =
        sendRequest(URI.create(uri), SdkHttpMethod.POST, Optional.ofNullable(requestBody));

    System.out.println(String.format("callCreateJobAPI response: %s", response.toPrettyString()));

    return response;
  }

  public static void createJob(CreateJobRequest createJobRequest)
      throws IOException, InterruptedException {
    String job = JsonFormat.printer().print(createJobRequest);

    String createJobAPIUri =
        String.format(CREATE_JOB_URI_PATTERN, FRONTEND_API, API_GATEWAY_STAGE, API_GATEWAY_VERSION);
    callCreateJobAPI(createJobAPIUri, job);
  }

  public static JsonNode callGetJobAPI(String uri) throws IOException {
    System.out.println(String.format("callGetJobAPI (%s) request", uri));

    JsonNode response = sendRequest(URI.create(uri), SdkHttpMethod.GET, Optional.empty());

    System.out.println(String.format("callGetJobAPI response: %s", response.toPrettyString()));

    return response;
  }

  public static boolean checkS3FileExists(
      S3BlobStorageClient blobStorageClient, String bucket, String key) throws Exception {
    try (InputStream dataStream =
        blobStorageClient.getBlob(
            DataLocation.ofBlobStoreDataLocation(BlobStoreDataLocation.create(bucket, key)))) {
      return true;
    } catch (BlobStorageClientException | NoSuchKeyException e) {
      return false;
    }
  }

  private static JsonNode sendRequest(URI uri, SdkHttpMethod method, Optional<String> body)
      throws IOException {
    SdkHttpFullRequest sdkHttpFullRequest = toSdkHttpRequest(uri, method, body);
    AwsCredentials awsCreds =
        AWS_SESSION_TOKEN == null
            ? AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
            : AwsSessionCredentials.create(
                AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN);
    SdkHttpFullRequest signedSdkHttpFullRequest =
        signHttpRequest(sdkHttpFullRequest, "execute-api", AWS_API_GATEWAY_REGION, awsCreds);
    HttpUriRequest signedRequest = toApacheHttpRequest(signedSdkHttpFullRequest, body);
    HttpClient client = HttpClients.custom().build();
    HttpResponse response = client.execute(signedRequest);
    String content = EntityUtils.toString(response.getEntity());
    return new ObjectMapper().readTree(content);
  }

  private static SdkHttpFullRequest toSdkHttpRequest(
      URI uri, SdkHttpMethod method, Optional<String> body) {
    var builder =
        SdkHttpFullRequest.builder()
            .uri(uri)
            .method(method)
            .appendHeader("Content-Type", "application/json")
            .appendHeader("Host", uri.getHost());
    body.ifPresent(b -> builder.contentStreamProvider(() -> new StringInputStream(b)));
    return builder.build();
  }

  private static SdkHttpFullRequest signHttpRequest(
      SdkHttpFullRequest request, String serviceName, Region region, AwsCredentials credentials) {

    Aws4SignerParams sdkRequestParams =
        Aws4SignerParams.builder()
            .signingName(serviceName)
            .signingRegion(region)
            .awsCredentials(credentials)
            .doubleUrlEncode(true)
            .build();

    return Aws4Signer.create().sign(request, sdkRequestParams);
  }

  private static HttpUriRequest toApacheHttpRequest(
      SdkHttpFullRequest request, Optional<String> body) throws IOException {
    var builder = RequestBuilder.create(request.method().toString());

    builder.setUri(request.getUri().toString());

    for (var header : request.headers().entrySet()) {
      // Handle corner-case where header may have multiple values and add each of them to the
      // request (generally calling code should not do this though).
      for (var value : header.getValue()) {
        builder.addHeader(header.getKey(), value);
      }
    }

    if (body.isPresent()) {
      var entity = new StringEntity(body.get());
      builder.setEntity(entity);
    }
    return builder.build();
  }
}
