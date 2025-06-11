/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.jobclient.local;

import com.google.aggregate.adtech.worker.jobclient.JobClient;
import com.google.aggregate.adtech.worker.jobclient.model.ErrorReason;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.model.JobRetryRequest;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerPath;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerResultPath;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.util.JsonFormat;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Job client that provides a job with path to a file from the local filesystem for aggregation,
 * only once (exhausts afterwards).
 *
 * <p>The path to the file should be injected from the CLI flags.
 */
final class LocalFileJobClient implements JobClient {

  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();

  private final Path localFilePath;
  private final Optional<Path> localJobResultFilePath;
  private final Supplier<ImmutableMap<String, String>> localFileJobParameters;

  // Flag indicating whether the local file was "pulled", thus exhausting this puller. Subsequent
  // pulls are empty.
  private boolean exhausted;

  /** Creates a new instance of the {@code LocalFileJobClient} class. */
  @Inject
  LocalFileJobClient(
      @LocalFileJobHandlerPath Path localFilePath,
      @LocalFileJobHandlerResultPath Optional<Path> localJobResultFilePath,
      @LocalFileJobParameters Supplier<ImmutableMap<String, String>> localFileJobParameters) {
    this.localFilePath = localFilePath;
    this.localJobResultFilePath = localJobResultFilePath;
    this.localFileJobParameters = localFileJobParameters;
    exhausted = false;
  }

  @Override
  public Optional<Job> getJob() throws JobClientException {
    if (exhausted) {
      return Optional.empty();
    }

    exhausted = true;

    Job defaultJob = FakeJobGenerator.generate("request");

    Job.Builder jobBuilder = defaultJob.toBuilder();

    // Creates jobParams from defaultJob and localFileJobParameters if provided.
    Optional<ImmutableMap<String, String>> jobParams =
        Optional.ofNullable(
            combineJobParams(
                defaultJob.requestInfo().getJobParametersMap(), localFileJobParameters.get()));

    String inputBucket =
        Files.isDirectory(localFilePath)
            ? localFilePath.toAbsolutePath().toString()
            : localFilePath.getParent().toAbsolutePath().toString();
    String inputPrefix =
        Files.isDirectory(localFilePath) ? "" : localFilePath.getFileName().toString();

    // Update the generated job to have values for a newly created job (e.g. no resultInfo set) and
    // set any other input values.
    jobBuilder
        .setRequestInfo(
            defaultJob.requestInfo().toBuilder()
                .setInputDataBucketName(inputBucket)
                .setInputDataBlobPrefix(inputPrefix)
                .putAllJobParameters(jobParams.orElse(ImmutableMap.of()))
                .build())
        .setResultInfo(Optional.empty());

    return Optional.of(jobBuilder.build());
  }

  private static ImmutableMap<String, String> combineJobParams(
      Map<String, String> defaultJobParams, Map<String, String> userJobParams) {
    Map<String, String> map = Maps.newHashMap();
    map.putAll(defaultJobParams);
    map.putAll(userJobParams);
    return ImmutableMap.copyOf(map);
  }

  @Override
  public void markJobCompleted(JobResult jobResult) throws JobClientException {
    // Print out the resultInfo for convenience, but also save to a file for automated flows that
    // need to check the result
    System.out.println(jobResult.resultInfo());

    if (localJobResultFilePath.isEmpty()) {
      return;
    }

    try {
      Files.writeString(
          localJobResultFilePath.get(),
          JSON_PRINTER.print(jobResult.resultInfo()),
          StandardCharsets.US_ASCII,
          StandardOpenOption.CREATE);
    } catch (IOException e) {
      throw new JobClientException(e, ErrorReason.JOB_MARK_COMPLETION_FAILED);
    }
  }

  @Override
  public void returnJobForRetry(JobRetryRequest jobRetryRequest) throws JobClientException {
    // Requires access to Job Metadata DB and the job cache.
  }

  @Override
  public void appendJobErrorMessage(JobKey jobKey, String error) throws JobClientException {
    // Requires access to Job Metadata DB to save the error message from Result Info
  }
}
