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
import static org.junit.Assert.assertThrows;

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
  public void validate_noAttributionReportToKeyInParams_fails() {
    ImmutableMap jobParams = ImmutableMap.of();
    Job job = buildJob(jobParams).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job), /* domainOptional= */ false));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "Exactly one of 'attribution_report_to' and 'reporting_site' fields should be specified"
                + " for the Job");
  }

  @Test
  public void validate_noAttributionReportTo_fails() {
    ImmutableMap jobParams = ImmutableMap.of("attribution_report_to", "");
    Job job = buildJob(jobParams).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job), /* domainOptional= */ false));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "The 'attribution_report_to' field in the Job parameters is empty for the Job");
  }

  @Test
  public void validate_noOutputDomain_domainOptional_succeeds() {
    ImmutableMap jobParams = ImmutableMap.of("attribution_report_to", "foo.com");
    Job job = buildJob(jobParams).build();

    JobValidator.validate(Optional.of(job), /* domainOptional= */ true);
  }

  @Test
  public void validate_noOutputDomain_domainNotOptional_fails() {
    ImmutableMap jobParams = ImmutableMap.of("attribution_report_to", "foo.com");
    Job job = buildJob(jobParams).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job), /* domainOptional= */ false));
    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job '' does not have output domain location specified in"
                + " 'output_domain_bucket_name' and 'output_domain_blob_prefix' fields. Please"
                + " refer to the API documentation for output domain parameters at"
                + " https://github.com/privacysandbox/aggregation-service/blob/main/docs/api.md");
  }

  @Test
  public void validate_outputDomainPresent_domainNotOptional_succeeds() {
    ImmutableMap jobParams =
        ImmutableMap.of(
            "attribution_report_to",
            "foo.com",
            "output_domain_blob_prefix",
            "prefix_",
            "output_domain_bucket_name",
            "bucket");
    Job job = buildJob(jobParams).build();

    JobValidator.validate(Optional.of(job), /* domainOptional= */ false);
  }

  @Test
  public void validate_noJob_fails() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.empty(), /* domainOptional= */ false));

    assertThat(exception).hasMessageThat().isEqualTo("Job metadata not found.");
  }

  @Test
  public void validate_validReportErrorThresholdPercentage_succeeds() {
    Job job1 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "report_error_threshold_percentage", "0"))
            .build();
    Job job2 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to",
                    "foo.com",
                    "report_error_threshold_percentage",
                    "67.0%"))
            .build();
    Job job3 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to",
                    "foo.com",
                    "report_error_threshold_percentage",
                    "100     "))
            .build();
    Job job4 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "report_error_threshold_percentage", "0.1"))
            .build();

    JobValidator.validate(Optional.of(job1), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(job2), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(job3), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(job4), /* domainOptional= */ true);
  }

  @Test
  public void validate_invalidReportErrorThresholdPercentage_fails() {
    Job job1 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "report_error_threshold_percentage", "-1"))
            .build();
    Job job2 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "report_error_threshold_percentage", "121"))
            .build();
    Job job3 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to",
                    "foo.com",
                    "report_error_threshold_percentage",
                    "100.1"))
            .build();

    IllegalArgumentException exception1 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job1), /* domainOptional= */ true));
    assertThat(exception1)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
    IllegalArgumentException exception2 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job2), /* domainOptional= */ true));
    assertThat(exception2)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
    IllegalArgumentException exception3 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job3), /* domainOptional= */ true));
    assertThat(exception3)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
  }

  @Test
  public void validate_validInputReportCount_succeeds() {
    Job jobWithoutCount = buildJob(ImmutableMap.of("attribution_report_to", "foo.com")).build();
    Job jobWithEmptyString =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "input_report_count", " "))
            .build();
    Job jobWithTrailingSpace =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "input_report_count", "100     "))
            .build();
    Job jobWithZeroReportCount =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "input_report_count", "0"))
            .build();

    JobValidator.validate(Optional.of(jobWithoutCount), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithEmptyString), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithTrailingSpace), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithZeroReportCount), /* domainOptional= */ true);
  }

  @Test
  public void validate_invalidInputReportCount_fails() {
    Job job1 =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "input_report_count", "-1"))
            .build();
    Job job2 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "input_report_count", "not a number"))
            .build();
    Job job3 =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "input_report_count", "100.1"))
            .build();

    assertThrows(
        IllegalArgumentException.class,
        () -> JobValidator.validate(Optional.of(job1), /* domainOptional= */ true));
    assertThrows(
        IllegalArgumentException.class,
        () -> JobValidator.validate(Optional.of(job2), /* domainOptional= */ true));
    assertThrows(
        IllegalArgumentException.class,
        () -> JobValidator.validate(Optional.of(job3), /* domainOptional= */ true));
  }

  @Test
  public void validate_reportErrorThresholdPercentageNotANumber_fails() {
    Job job1 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "report_error_threshold_percentage", ""))
            .build();
    Job job2 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to",
                    "foo.com",
                    "report_error_threshold_percentage",
                    "Not a number"))
            .build();
    Job job3 =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to",
                    "foo.com",
                    "report_error_threshold_percentage",
                    "%   "))
            .build();

    IllegalArgumentException exception1 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job1), /* domainOptional= */ true));
    assertThat(exception1)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
    IllegalArgumentException exception2 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job2), /* domainOptional= */ true));
    assertThat(exception2)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
    IllegalArgumentException exception3 =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job3), /* domainOptional= */ true));
    assertThat(exception2)
        .hasMessageThat()
        .containsMatch(
            "Job parameters for the job .* should have a valid value between 0 and 100 for"
                + " 'report_error_threshold_percentage' parameter");
  }

  @Test
  public void validate_validfilteringIds_succeeds() {
    Job jobWithNoFilteringId =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com")).build();
    Job jobWithEmptyFilteringId =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "filtering_ids", "      "))
            .build();
    Job jobWithOneFilteringId =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "filtering_ids", "   100, ,     "))
            .build();
    Job jobWithManyFilteringIds =
        buildJob(
                ImmutableMap.of(
                    "attribution_report_to", "foo.com", "filtering_ids", " 1,  2  , 3, ,,4,5, 6"))
            .build();

    JobValidator.validate(Optional.of(jobWithNoFilteringId), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithEmptyFilteringId), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithOneFilteringId), /* domainOptional= */ true);
    JobValidator.validate(Optional.of(jobWithManyFilteringIds), /* domainOptional= */ true);
  }

  @Test
  public void validate_invalidFilteringIds_throws() {
    Job jobWithNonNumberIds =
        buildJob(ImmutableMap.of("attribution_report_to", "foo.com", "filtering_ids", "1,2,null,5"))
            .build();

    assertThrows(
        IllegalArgumentException.class,
        () -> JobValidator.validate(Optional.of(jobWithNonNumberIds), /* domainOptional= */ true));
  }

  @Test
  public void validate_noReportingSite_fails() {
    ImmutableMap<String, String> jobParams = ImmutableMap.of("reporting_site", "");
    Job job = buildJob(jobParams).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job), /* domainOptional= */ false));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch("The 'reporting_site' field in the Job parameters is empty for the Job");
  }

  @Test
  public void validate_attributionReportToAndReportingSiteBothPresent_fails() {
    ImmutableMap<String, String> jobParams =
        ImmutableMap.of("attribution_report_to", "someOrigin", "reporting_site", "someSite");
    Job job = buildJob(jobParams).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> JobValidator.validate(Optional.of(job), /* domainOptional= */ false));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "Exactly one of 'attribution_report_to' and 'reporting_site' fields should be specified"
                + " for the Job");
  }

  private Job.Builder buildJob(ImmutableMap jobParams) {
    return jobBuilder.setRequestInfo(requestInfoBuilder.putAllJobParameters(jobParams).build());
  }
}
