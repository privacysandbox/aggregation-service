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

package com.google.aggregate.adtech.worker.frontend.tasks.aws;

import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_ATTRIBUTION_REPORT_TO;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_REPORTING_SITE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DB_ERROR_MESSAGE;
import static com.google.aggregate.adtech.worker.frontend.tasks.ErrorMessages.DUPLICATE_JOB_MESSAGE;
import static com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;

import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.tasks.CreateJobTaskBase;
import com.google.aggregate.adtech.worker.frontend.tasks.ErrorReasons;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobKeyExistsException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb.JobMetadataDbException;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Task to create a job in AWS. */
public final class AwsCreateJobTask extends CreateJobTaskBase {

  private final JobMetadataDb jobMetadataDb;
  private final Clock clock;

  /** Creates a new instance of the {@code AwsCreateJobTask} class. */
  @Inject
  public AwsCreateJobTask(
      JobMetadataDb jobMetadataDb, Clock clock, Set<RequestInfoValidator> requestInfoValidators) {
    super(requestInfoValidators);
    this.jobMetadataDb = jobMetadataDb;
    this.clock = clock;
  }

  @Override
  public void createJob(RequestInfo requestInfo) throws ServiceException {
    validate(requestInfo);

    Instant currentTime = Instant.now(clock);
    JobMetadata jobMetadata =
        JobMetadata.newBuilder()
            .setJobKey(JobKey.newBuilder().setJobRequestId(requestInfo.getJobRequestId()).build())
            .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(currentTime))
            .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(currentTime))
            .setNumAttempts(0)
            .setJobStatus(RECEIVED)
            .setServerJobId(UUID.randomUUID().toString())
            .setCreateJobRequest(convertCreateJobRequestBackend(requestInfo))
            .setRequestInfo(requestInfo)
            .build();

    try {
      jobMetadataDb.insertJobMetadata(jobMetadata);
    } catch (JobMetadataDbException e) {
      throw new ServiceException(
          Code.INTERNAL, ErrorReasons.SERVER_ERROR.toString(), DB_ERROR_MESSAGE, e);
    } catch (JobKeyExistsException e) {
      throw new ServiceException(
          Code.ALREADY_EXISTS,
          ErrorReasons.DUPLICATE_JOB_KEY.toString(),
          String.format(DUPLICATE_JOB_MESSAGE, requestInfo.getJobRequestId()));
    }
  }

  /**
   * Converts between the {@link
   * RequestInfo} and {@link
   * CreateJobRequest}.
   *
   * <p>TODO: Added for backwards compatibility and to be removed when CreateJobRequest shared model
   * no longer needs to be populated.
   */
  private CreateJobRequest convertCreateJobRequestBackend(RequestInfo requestInfo) {
    CreateJobRequest.Builder builder =
        CreateJobRequest.newBuilder()
            .setJobRequestId(requestInfo.getJobRequestId())
            .setAttributionReportTo(
                requestInfo.getJobParametersMap().getOrDefault(JOB_PARAM_ATTRIBUTION_REPORT_TO, ""))
            .setReportingSite(
                requestInfo.getJobParametersMap().getOrDefault(JOB_PARAM_REPORTING_SITE, ""))
            .setInputDataBucketName(requestInfo.getInputDataBucketName())
            .setInputDataBlobPrefix(requestInfo.getInputDataBlobPrefix())
            .addAllInputDataBlobPrefixes(requestInfo.getInputDataBlobPrefixesList())
            .setOutputDataBucketName(requestInfo.getOutputDataBucketName())
            .setOutputDataBlobPrefix(requestInfo.getOutputDataBlobPrefix())
            .setPostbackUrl(requestInfo.getPostbackUrl())
            .putAllJobParameters(requestInfo.getJobParametersMap());

    Optional.ofNullable(requestInfo.getJobParametersMap().get(JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX))
        .ifPresent(builder::setOutputDomainBlobPrefix);

    Optional.ofNullable(requestInfo.getJobParametersMap().get(JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME))
        .ifPresent(builder::setOutputDomainBucketName);

    Optional.ofNullable(requestInfo.getJobParameters().get(JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT))
        .map(Integer::parseInt)
        .ifPresent(builder::setDebugPrivacyBudgetLimit);

    return builder.build();
  }
}
