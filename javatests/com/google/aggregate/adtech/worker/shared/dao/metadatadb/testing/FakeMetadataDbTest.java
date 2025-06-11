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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobKeyExistsException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataConflictException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeMetadataDbTest {

  FakeMetadataDb fakeMetadataDb;
  JobKey jobKey;
  JobMetadata jobMetadata;

  @Before
  public void setUp() {
    fakeMetadataDb = new FakeMetadataDb();

    String jobRequestId = "this-is-a-job-request-id";
    String attributionReportTo = "foo.com";
    jobKey = JobKey.newBuilder().setJobRequestId(jobRequestId).build();

    jobMetadata =
        JobMetadata.newBuilder()
            .setJobKey(jobKey)
            .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setRequestProcessingStartedAt(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setNumAttempts(0)
            .setJobStatus(JobStatus.RECEIVED)
            .setCreateJobRequest(
                CreateJobRequest.newBuilder()
                    .setJobRequestId(jobRequestId)
                    .setInputDataBlobPrefix("file.avro")
                    .setInputDataBucketName("bucket")
                    .setOutputDataBlobPrefix("file.avro")
                    .setOutputDataBucketName("bucket")
                    .setPostbackUrl("http://foo.com/api/endpoint")
                    .setAttributionReportTo(attributionReportTo)
                    .setDebugPrivacyBudgetLimit(5)
                    .putAllJobParameters(
                        ImmutableMap.of(
                            "attribution_report_to",
                            attributionReportTo,
                            "output_domain_blob_prefix",
                            "file.avro",
                            "output_domain_bucket_name",
                            "bucket",
                            "debug_privacy_budget_limit",
                            "5"))
                    .build())
            .build();
  }

  @Test
  public void testGetJobMetadata_present() throws Exception {
    fakeMetadataDb.setJobMetadataToReturn(Optional.of(jobMetadata));

    Optional<JobMetadata> retrievedJobMetadata =
        fakeMetadataDb.getJobMetadata(jobKey.getJobRequestId());

    assertThat(retrievedJobMetadata).isPresent();
    assertThat(retrievedJobMetadata.get()).isEqualTo(jobMetadata);
    assertThat(fakeMetadataDb.getLastJobKeyStringLookedUp()).isEqualTo(jobKey.getJobRequestId());
  }

  @Test
  public void testGetJobMetadata_empty() throws Exception {
    fakeMetadataDb.setJobMetadataToReturn(Optional.empty());

    Optional<JobMetadata> retrievedJobMetadata =
        fakeMetadataDb.getJobMetadata(jobKey.getJobRequestId());

    assertThat(retrievedJobMetadata).isEmpty();
    assertThat(fakeMetadataDb.getLastJobKeyStringLookedUp()).isEqualTo(jobKey.getJobRequestId());
  }

  @Test
  public void testGetJobMetadata_exception() throws Exception {
    fakeMetadataDb.setShouldThrowJobMetadataDbException(true);

    assertThrows(
        JobMetadataDbException.class,
        () -> fakeMetadataDb.getJobMetadata(jobKey.getJobRequestId()));
  }

  @Test
  public void testInsertJobMetadata_normal() throws Exception {
    // No setup

    fakeMetadataDb.insertJobMetadata(jobMetadata);

    assertThat(fakeMetadataDb.getLastJobMetadataInserted()).isEqualTo(jobMetadata);
  }

  @Test
  public void testInsertJobMetadata_jobMetadataDbException() throws Exception {
    fakeMetadataDb.setShouldThrowJobMetadataDbException(true);

    assertThrows(JobMetadataDbException.class, () -> fakeMetadataDb.insertJobMetadata(jobMetadata));
  }

  @Test
  public void testInsertJobMetadata_jobKeyExistsException() throws Exception {
    fakeMetadataDb.setShouldThrowJobKeyExistsException(true);

    assertThrows(JobKeyExistsException.class, () -> fakeMetadataDb.insertJobMetadata(jobMetadata));
  }

  @Test
  public void testUpdateJobMetadata_normal() throws Exception {
    // No setup

    fakeMetadataDb.updateJobMetadata(jobMetadata);

    assertThat(fakeMetadataDb.getLastJobMetadataUpdated()).isEqualTo(jobMetadata);
  }

  @Test
  public void testUpdateJobMetadata_jobMetadataDbException() throws Exception {
    fakeMetadataDb.setShouldThrowJobMetadataDbException(true);

    assertThrows(JobMetadataDbException.class, () -> fakeMetadataDb.updateJobMetadata(jobMetadata));
  }

  @Test
  public void testUpdateJobMetadata_jobMetadataConflictException() throws Exception {
    fakeMetadataDb.setShouldThrowJobMetadataConflictException(true);

    assertThrows(
        JobMetadataConflictException.class, () -> fakeMetadataDb.updateJobMetadata(jobMetadata));
  }
}
