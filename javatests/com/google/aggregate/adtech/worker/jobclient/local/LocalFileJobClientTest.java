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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.protobuf.util.JsonFormat;
import com.google.aggregate.adtech.worker.jobclient.JobClient.JobClientException;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerPath;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobHandlerResultPath;
import com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobHandlerModule.LocalFileJobParameters;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode;
import com.google.scp.shared.mapper.TimeObjectMapper;
import com.google.scp.shared.proto.ProtoUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertThrows;

@RunWith(JUnit4.class)
public class LocalFileJobClientTest {

  private static final String DATA_DIRECTORY = "/data";
  private static final String INPUT_FILE_NAME = "foo";
  private static final String RESULT_FILE_NAME = "results.json";
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject @LocalFileJobHandlerResultPath Optional<Path> localJobResultFilePath;
  @Inject JobParamsSupplier jobParamsSupplier;
  @Inject FileSystem inMemoryFileSystem;

  Path directory;
  Job expectedJob;

  // Under test
  @Inject
  com.google.aggregate.adtech.worker.jobclient.local.LocalFileJobClient jobClient;

  @Before
  public void setUp() throws Exception {
    directory = inMemoryFileSystem.getPath(DATA_DIRECTORY);
    Job generatedJob = FakeJobGenerator.generate("request");
    expectedJob =
        generatedJob.toBuilder()
            .setRequestInfo(
                generatedJob.requestInfo().toBuilder()
                    .setInputDataBucketName(DATA_DIRECTORY)
                    .setInputDataBlobPrefix(INPUT_FILE_NAME)
                    .build())
            .setResultInfo(Optional.empty())
            .build();
    Files.createDirectory(directory);
  }

  @Test
  public void readsAndExhausts() throws JobClientException {
    // No set up, injection is basically the set up.

    jobClient.getJob();
    Optional<Job> jobExhausted = jobClient.getJob();

    assertThat(jobExhausted).isEmpty();
  }

  @Test
  public void readsAndExhausts_withJobParameters() throws JobClientException {
    // Setup job parameters.
    jobParamsSupplier.setJobParams(ImmutableMap.of("test-param", "test-value-1"));

    Optional<Job> job = jobClient.getJob();
    Optional<Job> jobExhausted = jobClient.getJob();

    ImmutableMap.Builder<String, String> expectedJobParamsBuilder = ImmutableMap.builder();
    ImmutableMap<String, String> expectedJobParams =
        expectedJobParamsBuilder
            .putAll(expectedJob.requestInfo().getJobParameters())
            .put("test-param", "test-value-1")
            .build();
    assertThat(job)
        .hasValue(
            expectedJob.toBuilder()
                .setRequestInfo(
                    expectedJob.requestInfo().toBuilder()
                        .putAllJobParameters(expectedJobParams)
                        .build())
                .build());
    assertThat(jobExhausted).isEmpty();
  }

  @Test
  public void writesJobResult() throws Exception {
    JobResult result = makeJobResult();

    jobClient.markJobCompleted(result);
    String actualResultSerialized = readJobResult();

    ResultInfo.Builder builder = ResultInfo.newBuilder();
    JSON_PARSER.merge(actualResultSerialized, builder);
    ResultInfo actualResult = builder.build();

    assertThat(actualResult).isEqualTo(result.resultInfo());
  }

  @Test
  public void writesJobResult_withFileError() throws Exception {
    // Removing the data directory will cause the job client to fail with an excepetion when trying
    // to write to that directory.
    Files.delete(inMemoryFileSystem.getPath(DATA_DIRECTORY));
    JobResult result = makeJobResult();

    assertThrows(JobClientException.class, () -> jobClient.markJobCompleted(result));
  }

  private String readJobResult() throws IOException {
    return Files.readString(localJobResultFilePath.get(), US_ASCII);
  }

  private JobResult makeJobResult() throws JobClientException {
    return JobResult.builder()
        .setJobKey(JobKey.newBuilder().setJobRequestId("foo").build())
        .setResultInfo(
            ResultInfo.newBuilder()
                .setErrorSummary(
                    ErrorSummary.newBuilder()
                        .setNumReportsWithErrors(5)
                        .addAllErrorCounts(ImmutableList.of())
                        .build())
                .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.ofEpochMilli(123)))
                .setReturnCode(ReturnCode.SUCCESS.name())
                .setReturnMessage("return-message")
                .build())
        .build();
  }

  private static final class OutputDomainSupplier implements Supplier<Optional<Path>> {

    private Optional<Path> outputDomain;

    OutputDomainSupplier() {
      outputDomain = Optional.empty();
    }

    private void setOutputDomain(Path outputDomainPath) {
      outputDomain = Optional.of(outputDomainPath);
    }

    @Override
    public Optional<Path> get() {
      return outputDomain;
    }
  }

  private static final class JobParamsSupplier implements Supplier<ImmutableMap<String, String>> {

    private ImmutableMap<String, String> jobParams = ImmutableMap.of();

    @Override
    public ImmutableMap<String, String> get() {
      return jobParams;
    }

    private void setJobParams(ImmutableMap<String, String> jobParams) {
      this.jobParams = jobParams;
    }
  }

  private static final class TestEnv extends AbstractModule {

    @Provides
    @TestScoped
    FileSystem provideInMemoryFileSystem() {
      return Jimfs.newFileSystem(Configuration.unix().toBuilder().setWorkingDirectory("/").build());
    }

    @Provides
    @TestScoped
    @LocalFileJobHandlerResultPath
    Optional<Path> provideLocalResultPath(FileSystem fs) {
      return Optional.of(fs.getPath(DATA_DIRECTORY).resolve(RESULT_FILE_NAME));
    }

    @Provides
    @LocalFileJobHandlerPath
    Path provideLocalFilePath(FileSystem fs) {
      return fs.getPath(DATA_DIRECTORY).resolve(INPUT_FILE_NAME);
    }

    @Provides
    @LocalFileJobParameters
    Supplier<ImmutableMap<String, String>> provideLocalFileJobParameters(
        JobParamsSupplier supplier) {
      return supplier;
    }

    @Override
    protected void configure() {
      bind(ObjectMapper.class).to(TimeObjectMapper.class);
      bind(JobParamsSupplier.class).in(TestScoped.class);
    }
  }
}
