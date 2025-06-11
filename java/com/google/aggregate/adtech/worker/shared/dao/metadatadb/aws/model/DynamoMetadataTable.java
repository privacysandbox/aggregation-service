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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model;

import static com.google.scp.shared.proto.ProtoUtil.toJavaInstant;
import static com.google.scp.shared.proto.ProtoUtil.toProtoTimestamp;

import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.attributeconverter.DynamoJobKeyConverter;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.attributeconverter.ErrorCountsAttributeConverter;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.attributeconverter.OptionalIntegerAttributeConverter;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.attributeconverter.OptionalStringAttributeConverter;
import com.google.aggregate.adtech.worker.shared.model.BackendModelUtil;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;

/** DynamoDB table schema for JobMetadata table. */
public final class DynamoMetadataTable {

  // CreateJobRequest Attributes
  private static final String JOB_REQUEST_ID = "JobRequestId";
  private static final String INPUT_DATA_BLOB_PREFIX = "InputDataBlobPrefix";
  private static final String input_data_blob_prefixes = "InputDataBlobPrefixes";
  private static final String INPUT_DATA_BLOB_BUCKET = "InputDataBlobBucket";
  private static final String OUTPUT_DATA_BLOB_PREFIX = "OutputDataBlobPrefix";
  private static final String OUTPUT_DATA_BLOB_BUCKET = "OutputDataBlobBucket";
  private static final String OUTPUT_DOMAIN_BLOB_PREFIX = "OutputDomainBlobPrefix";
  private static final String OUTPUT_DOMAIN_BLOB_BUCKET = "OutputDomainBlobBucket";
  private static final String POSTBACK_URL = "PostbackUrl";
  private static final String ATTRIBUTION_REPORT_TO = "AttributionReportTo";
  private static final String REPORTING_SITE = "ReportingSite";
  private static final String DEBUG_PRIVACY_BUDGET_LIMIT = "DebugPrivacyBudgetLimit";
  private static final String JOB_PARAMETERS = "JobParameters";

  // ResultInfo Attributes
  private static final String RETURN_CODE = "ReturnCode";
  private static final String RETURN_MESSAGE = "ReturnMessage";
  private static final String ERROR_SUMMARY = "ErrorSummary";
  private static final String FINISHED_AT = "FinishedAt";

  // ErrorSummary Attributes
  private static final String NUM_REPORTS_WITH_ERRORS = "NumReportsWithErrors";
  private static final String ERROR_COUNTS = "ErrorCounts";
  private static final String INTERNAL_ERROR_MESSAGES = "InternalErrorMessages";

  // JobMetadata Table Attributes
  private static final String JOB_KEY = "JobKey";
  private static final String REQUEST_RECEIVED_AT = "RequestReceivedAt";
  private static final String REQUEST_UPDATED_AT = "RequestUpdatedAt";
  private static final String NUM_ATTEMPTS = "NumAttempts";
  private static final String JOB_STATUS = "JobStatus";
  private static final String SERVER_JOB_ID = "ServerJobId";
  private static final String CREATE_JOB_REQUEST = "CreateJobRequest";
  private static final String REQUEST_INFO = "RequestInfo";
  private static final String RESULT_INFO = "ResultInfo";
  private static final String RECORD_VERSION = "RecordVersion";
  private static final String TTL = "Ttl";
  private static final String REQUEST_PROCESSING_STARTED_AT = "RequestProcessingStartedAt";

  private static TableSchema<CreateJobRequest> getCreateJobRequestSchema() {
    return StaticImmutableTableSchema.builder(
            CreateJobRequest.class, CreateJobRequest.Builder.class)
        .newItemBuilder(CreateJobRequest::newBuilder, CreateJobRequest.Builder::build)
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(JOB_REQUEST_ID)
                    .getter(CreateJobRequest::getJobRequestId)
                    .setter(CreateJobRequest.Builder::setJobRequestId))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(INPUT_DATA_BLOB_PREFIX)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getInputDataBlobPrefix()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setInputDataBlobPrefix)))
        .addAttribute(
            EnhancedType.listOf(String.class),
            attribute ->
                attribute
                    .name(input_data_blob_prefixes)
                    .getter(CreateJobRequest::getInputDataBlobPrefixesList)
                    .setter(CreateJobRequest.Builder::addAllInputDataBlobPrefixes))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(INPUT_DATA_BLOB_BUCKET)
                    .getter(CreateJobRequest::getInputDataBucketName)
                    .setter(CreateJobRequest.Builder::setInputDataBucketName))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(OUTPUT_DATA_BLOB_PREFIX)
                    .getter(CreateJobRequest::getOutputDataBlobPrefix)
                    .setter(CreateJobRequest.Builder::setOutputDataBlobPrefix))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(OUTPUT_DATA_BLOB_BUCKET)
                    .getter(CreateJobRequest::getOutputDataBucketName)
                    .setter(CreateJobRequest.Builder::setOutputDataBucketName))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(OUTPUT_DOMAIN_BLOB_PREFIX)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getOutputDomainBlobPrefix()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setOutputDomainBlobPrefix)))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(OUTPUT_DOMAIN_BLOB_BUCKET)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getOutputDomainBucketName()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setOutputDomainBucketName)))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(POSTBACK_URL)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue -> optionalFromProtoField(attributeValue.getPostbackUrl()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setPostbackUrl)))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(ATTRIBUTION_REPORT_TO)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getAttributionReportTo()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setAttributionReportTo)))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(REPORTING_SITE)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue -> optionalFromProtoField(attributeValue.getReportingSite()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setReportingSite)))
        .addAttribute(
            EnhancedType.optionalOf(Integer.class),
            attribute ->
                attribute
                    .name(DEBUG_PRIVACY_BUDGET_LIMIT)
                    .attributeConverter(OptionalIntegerAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getDebugPrivacyBudgetLimit()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setDebugPrivacyBudgetLimit)))
        .addAttribute(
            EnhancedType.mapOf(String.class, String.class),
            attribute ->
                attribute
                    .name(JOB_PARAMETERS)
                    .getter(CreateJobRequest::getJobParameters)
                    .setter(CreateJobRequest.Builder::putAllJobParameters))
        .build();
  }

  private static TableSchema<RequestInfo> getRequestInfoSchema() {
    return StaticImmutableTableSchema.builder(RequestInfo.class, RequestInfo.Builder.class)
        .newItemBuilder(RequestInfo::newBuilder, RequestInfo.Builder::build)
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(JOB_REQUEST_ID)
                    .getter(RequestInfo::getJobRequestId)
                    .setter(RequestInfo.Builder::setJobRequestId))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(INPUT_DATA_BLOB_PREFIX)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getInputDataBlobPrefix()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setInputDataBlobPrefix)))
        .addAttribute(
            EnhancedType.listOf(String.class),
            attribute ->
                attribute
                    .name(input_data_blob_prefixes)
                    .getter(RequestInfo::getInputDataBlobPrefixesList)
                    .setter(RequestInfo.Builder::addAllInputDataBlobPrefixes))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(INPUT_DATA_BLOB_BUCKET)
                    .getter(RequestInfo::getInputDataBucketName)
                    .setter(RequestInfo.Builder::setInputDataBucketName))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(OUTPUT_DATA_BLOB_PREFIX)
                    .getter(RequestInfo::getOutputDataBlobPrefix)
                    .setter(RequestInfo.Builder::setOutputDataBlobPrefix))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(OUTPUT_DATA_BLOB_BUCKET)
                    .getter(RequestInfo::getOutputDataBucketName)
                    .setter(RequestInfo.Builder::setOutputDataBucketName))
        .addAttribute(
            EnhancedType.optionalOf(String.class),
            attribute ->
                attribute
                    .name(POSTBACK_URL)
                    .attributeConverter(OptionalStringAttributeConverter.create())
                    .getter(
                        attributeValue -> optionalFromProtoField(attributeValue.getPostbackUrl()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setPostbackUrl)))
        .addAttribute(
            EnhancedType.mapOf(String.class, String.class),
            attribute ->
                attribute
                    .name(JOB_PARAMETERS)
                    .getter(RequestInfo::getJobParameters)
                    .setter(RequestInfo.Builder::putAllJobParameters))
        .build();
  }

  private static TableSchema<ErrorSummary> getErrorSummarySchema() {
    return StaticImmutableTableSchema.builder(ErrorSummary.class, ErrorSummary.Builder.class)
        .newItemBuilder(ErrorSummary::newBuilder, ErrorSummary.Builder::build)
        .addAttribute(
            OptionalLong.class,
            attribute ->
                attribute
                    .name(NUM_REPORTS_WITH_ERRORS)
                    .getter(
                        attributeValue ->
                            optionalFromProtoField(attributeValue.getNumReportsWithErrors()))
                    .setter(
                        (builder, optionalValue) ->
                            optionalValue.ifPresent(builder::setNumReportsWithErrors)))
        .addAttribute(
            EnhancedType.listOf(ErrorCount.class),
            attribute ->
                attribute
                    .name(ERROR_COUNTS)
                    .attributeConverter(ErrorCountsAttributeConverter.create())
                    .getter(ErrorSummary::getErrorCountsList)
                    .setter(ErrorSummary.Builder::addAllErrorCounts))
        .addAttribute(
            EnhancedType.listOf(String.class),
            attribute ->
                attribute
                    .name(INTERNAL_ERROR_MESSAGES)
                    .getter(ErrorSummary::getErrorMessagesList)
                    .setter(ErrorSummary.Builder::addAllErrorMessages))
        .build();
  }

  private static TableSchema<ResultInfo> getResultInfoSchema() {
    return StaticImmutableTableSchema.builder(ResultInfo.class, ResultInfo.Builder.class)
        .newItemBuilder(ResultInfo::newBuilder, ResultInfo.Builder::build)
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(RETURN_CODE)
                    .getter(ResultInfo::getReturnCode)
                    .setter(ResultInfo.Builder::setReturnCode))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(RETURN_MESSAGE)
                    .getter(ResultInfo::getReturnMessage)
                    .setter(ResultInfo.Builder::setReturnMessage))
        .addAttribute(
            EnhancedType.documentOf(ErrorSummary.class, getErrorSummarySchema()),
            attribute ->
                attribute
                    .name(ERROR_SUMMARY)
                    .getter(ResultInfo::getErrorSummary)
                    .setter(ResultInfo.Builder::setErrorSummary))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(FINISHED_AT)
                    .getter(attributeValue -> toJavaInstant(attributeValue.getFinishedAt()))
                    .setter((builder, instant) -> builder.setFinishedAt(toProtoTimestamp(instant))))
        .build();
  }

  /** Returns the table schema for the DynamoDB representation of {@code JobMetadata}. */
  public static TableSchema<JobMetadata> getDynamoDbTableSchema() {
    return StaticImmutableTableSchema.builder(JobMetadata.class, JobMetadata.Builder.class)
        .newItemBuilder(JobMetadata::newBuilder, JobMetadata.Builder::build)
        .addAttribute(
            JobKey.class,
            attribute ->
                attribute
                    .name(JOB_KEY)
                    .attributeConverter(DynamoJobKeyConverter.create())
                    .getter(JobMetadata::getJobKey)
                    .setter(JobMetadata.Builder::setJobKey)
                    .tags(StaticAttributeTags.primaryPartitionKey()))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(REQUEST_RECEIVED_AT)
                    .getter(attributeValue -> toJavaInstant(attributeValue.getRequestReceivedAt()))
                    .setter(
                        (builder, instant) ->
                            builder.setRequestReceivedAt(toProtoTimestamp(instant))))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(REQUEST_UPDATED_AT)
                    .getter(attributeValue -> toJavaInstant(attributeValue.getRequestUpdatedAt()))
                    .setter(
                        (builder, instant) ->
                            builder.setRequestUpdatedAt(toProtoTimestamp(instant))))
        .addAttribute(
            Integer.class,
            attribute ->
                attribute
                    .name(NUM_ATTEMPTS)
                    .getter(JobMetadata::getNumAttempts)
                    .setter(JobMetadata.Builder::setNumAttempts))
        .addAttribute(
            JobStatus.class,
            attribute ->
                attribute
                    .name(JOB_STATUS)
                    .getter(JobMetadata::getJobStatus)
                    .setter(JobMetadata.Builder::setJobStatus))
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(SERVER_JOB_ID)
                    .getter(JobMetadata::getServerJobId)
                    .setter(JobMetadata.Builder::setServerJobId))
        .addAttribute(
            EnhancedType.documentOf(CreateJobRequest.class, getCreateJobRequestSchema()),
            attribute ->
                attribute
                    .name(CREATE_JOB_REQUEST)
                    .getter(BackendModelUtil::getCreateJobRequestValue)
                    .setter(BackendModelUtil::setCreateJobRequestValue))
        .addAttribute(
            EnhancedType.documentOf(RequestInfo.class, getRequestInfoSchema()),
            attribute ->
                attribute
                    .name(REQUEST_INFO)
                    .getter(BackendModelUtil::getRequestInfoValue)
                    .setter(BackendModelUtil::setRequestInfoValue))
        .addAttribute(
            EnhancedType.documentOf(ResultInfo.class, getResultInfoSchema()),
            attribute ->
                attribute
                    .name(RESULT_INFO)
                    .getter(BackendModelUtil::getResultInfoValue)
                    .setter(BackendModelUtil::setResultInfoValue))
        .addAttribute(
            OptionalInt.class,
            attribute ->
                attribute
                    .name(RECORD_VERSION)
                    .getter(
                        attributeValue ->
                            optionalIntFromProtoField(attributeValue.getRecordVersion()))
                    .setter(
                        (builder, optionalInt) -> optionalInt.ifPresent(builder::setRecordVersion))
                    .tags(AttributeTags.versionAttribute()))
        .addAttribute(
            OptionalLong.class,
            attribute ->
                attribute
                    .name(TTL)
                    .getter(attributeValue -> optionalFromProtoField(attributeValue.getTtl()))
                    .setter((builder, optionalLong) -> optionalLong.ifPresent(builder::setTtl)))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(REQUEST_PROCESSING_STARTED_AT)
                    .getter(BackendModelUtil::getRequestProcessingStartedTimeValue)
                    .setter(BackendModelUtil::setRequestProcessingStartedTimeValue))
        .build();
  }

  private static Optional<String> optionalFromProtoField(String stringValue) {
    if (stringValue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(stringValue);
  }

  private static OptionalLong optionalFromProtoField(long longValue) {
    if (longValue == 0) {
      return OptionalLong.empty();
    }
    return OptionalLong.of(longValue);
  }

  private static Optional<Integer> optionalFromProtoField(int intValue) {
    if (intValue == 0) {
      return Optional.empty();
    }
    return Optional.of(intValue);
  }

  private static OptionalInt optionalIntFromProtoField(int intValue) {
    if (intValue == 0) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(intValue);
  }
}
