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

package com.google.aggregate.adtech.worker.validation;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.cpio.jobclient.testing.FakeJobGenerator;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class JobValidatorTest {

  private Job.Builder jobBuilder;
  private RequestInfo.Builder requestInfoBuilder;

  @Before
  public void setUp() {
    jobBuilder = FakeJobGenerator.generateBuilder("");
    requestInfoBuilder =
        RequestInfo.newBuilder()
            .setJobRequestId("123")
            .setInputDataBlobPrefix("foo")
            .setInputDataBucketName("foo")
            .setOutputDataBlobPrefix("foo")
            .setOutputDataBucketName("foo");
  }

  @Test
  public void test_nonEmptyReportValidationSucceeds() {
    ImmutableMap jobParams = ImmutableMap.of("attribution_report_to", "foo.com");
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    boolean result = JobValidator.validate(Optional.of(job));

    assertThat(result).isTrue();
  }

  @Test
  public void test_emptyReportValidationEmptyString() {
    ImmutableMap jobParams = ImmutableMap.of("attribution_report_to", "");
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    boolean result = JobValidator.validate(Optional.of(job));

    assertThat(result).isFalse();
  }

  @Test
  public void test_emptyReportValidationMissingParam() {
    ImmutableMap jobParams = ImmutableMap.of();
    Job job =
        jobBuilder
            .setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build())
            .build();

    boolean result = JobValidator.validate(Optional.of(job));

    assertThat(result).isFalse();
  }
}
