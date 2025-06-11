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

package com.google.aggregate.adtech.worker.frontend.service.converter;

import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_ATTRIBUTION_REPORT_TO;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME;
import static com.google.aggregate.adtech.worker.frontend.service.model.Constants.JOB_PARAM_REPORTING_SITE;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.aggregate.protos.frontend.api.v1.GetJobResponseProto;
import com.google.aggregate.protos.frontend.api.v1.JobStatusProto;
import com.google.aggregate.protos.frontend.api.v1.ResultInfoProto;
import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts between the {@link
 * JobMetadata} and {@link
 * GetJobResponseProto.GetJobResponse}. *
 */
public final class GetJobResponseConverter
    extends Converter<JobMetadata, GetJobResponseProto.GetJobResponse> {

  private final Converter<
          com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo,
          ResultInfoProto.ResultInfo>
      resultInfoConverter;
  private final Converter<
          com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
          JobStatusProto.JobStatus>
      jobStatusConverter;

  /** Creates a new instance of the {@code GetJobResponseConverter} class. */
  @Inject
  public GetJobResponseConverter(
      Converter<
              com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo,
              ResultInfoProto.ResultInfo>
          resultInfoConverter,
      Converter<
              com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
              JobStatusProto.JobStatus>
          jobStatusConverter) {

    this.resultInfoConverter = resultInfoConverter;
    this.jobStatusConverter = jobStatusConverter;
  }

  /** Converts the shared metadata model into the frontend response model. */
  @Override
  protected GetJobResponseProto.GetJobResponse doForward(JobMetadata jobMetadata) {
    Map<String, String> jobParameters = new HashMap<>();
    ResultInfoProto.ResultInfo resultInfo =
        jobMetadata.hasResultInfo()
            ? resultInfoConverter.convert(jobMetadata.getResultInfo())
            : ResultInfoProto.ResultInfo.getDefaultInstance();
    if (jobMetadata.hasRequestInfo()) {
      RequestInfo requestInfo = jobMetadata.getRequestInfo();
      jobParameters.putAll(jobMetadata.getRequestInfo().getJobParameters());
      GetJobResponseProto.GetJobResponse.Builder builder =
          GetJobResponseProto.GetJobResponse.newBuilder()
              .setJobRequestId(requestInfo.getJobRequestId())
              .addAllInputDataBlobPrefixes(requestInfo.getInputDataBlobPrefixesList())
              .setInputDataBucketName(requestInfo.getInputDataBucketName())
              .setInputDataBlobPrefix(requestInfo.getInputDataBlobPrefix())
              .setOutputDataBucketName(requestInfo.getOutputDataBucketName())
              .setOutputDataBlobPrefix(requestInfo.getOutputDataBlobPrefix())
              .setPostbackUrl(requestInfo.getPostbackUrl())
              .setJobStatus(jobStatusConverter.convert(jobMetadata.getJobStatus()))
              .setRequestReceivedAt(jobMetadata.getRequestReceivedAt())
              .setRequestUpdatedAt(jobMetadata.getRequestUpdatedAt())
              .putAllJobParameters(ImmutableMap.copyOf(jobParameters));
      if (jobMetadata.hasResultInfo()) {
        builder.setResultInfo(resultInfo);
      }
      // If the worker hasn't picked up the job, there is no requestProcessedTime in getJob API.
      if (jobMetadata.hasRequestProcessingStartedAt()) {
        builder.setRequestProcessingStartedAt(jobMetadata.getRequestProcessingStartedAt());
      }
      return builder.build();
    } else {
      // This else block should be removed after CreateJobRequest is fully deprecated
      CreateJobRequest createJobRequest = jobMetadata.getCreateJobRequest();
      jobParameters.putAll(createJobRequest.getJobParameters());
      if (!createJobRequest.getAttributionReportTo().isEmpty()) {
        jobParameters.put(
            JOB_PARAM_ATTRIBUTION_REPORT_TO, createJobRequest.getAttributionReportTo());
      }
      if (!createJobRequest.getReportingSite().isEmpty()) {
        jobParameters.put(JOB_PARAM_REPORTING_SITE, createJobRequest.getReportingSite());
      }
      if (!createJobRequest.getOutputDomainBucketName().equals("")) {
        jobParameters.put(
            JOB_PARAM_OUTPUT_DOMAIN_BUCKET_NAME, createJobRequest.getOutputDomainBucketName());
      }
      if (!createJobRequest.getOutputDomainBlobPrefix().equals("")) {
        jobParameters.put(
            JOB_PARAM_OUTPUT_DOMAIN_BLOB_PREFIX, createJobRequest.getOutputDomainBlobPrefix());
      }
      if (createJobRequest.getDebugPrivacyBudgetLimit() != 0) {
        jobParameters.put(
            JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
            Long.toString(createJobRequest.getDebugPrivacyBudgetLimit()));
      }
      GetJobResponseProto.GetJobResponse.Builder builder =
          GetJobResponseProto.GetJobResponse.newBuilder()
              .setJobRequestId(createJobRequest.getJobRequestId())
              .setInputDataBucketName(createJobRequest.getInputDataBucketName())
              .setInputDataBlobPrefix(createJobRequest.getInputDataBlobPrefix())
              .addAllInputDataBlobPrefixes(createJobRequest.getInputDataBlobPrefixesList())
              .setOutputDataBucketName(createJobRequest.getOutputDataBucketName())
              .setOutputDataBlobPrefix(createJobRequest.getOutputDataBlobPrefix())
              .setPostbackUrl(createJobRequest.getPostbackUrl())
              .setJobStatus(jobStatusConverter.convert(jobMetadata.getJobStatus()))
              .setRequestReceivedAt(jobMetadata.getRequestReceivedAt())
              .setRequestUpdatedAt(jobMetadata.getRequestUpdatedAt())
              .putAllJobParameters(ImmutableMap.copyOf(jobParameters));
      if (jobMetadata.hasResultInfo()) {
        builder.setResultInfo(resultInfo);
      }
      // If the worker hasn't picked up the job, there is no requestProcessedTime in getJob API.
      if (jobMetadata.hasRequestProcessingStartedAt()) {
        builder.setRequestProcessingStartedAt(jobMetadata.getRequestProcessingStartedAt());
      }
      return builder.build();
    }
  }

  /** Converts the frontend response model into the shared metadata model. */
  @Override
  protected JobMetadata doBackward(GetJobResponseProto.GetJobResponse getJobResponse) {
    throw new UnsupportedOperationException();
  }
}
