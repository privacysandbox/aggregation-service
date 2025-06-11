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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.scp.shared.proto.ProtoUtil.toJavaInstant;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.aggregate.protos.shared.backend.ErrorCountProto.ErrorCount;
import com.google.aggregate.protos.shared.backend.JobErrorCategoryProto.JobErrorCategory;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Instant;
import java.util.Date;

/** Provides fake Job related objects for testing. */
public final class JobGenerator {

  private static final String DATA_HANDLE = "dataHandle";
  private static final String[] DATA_HANDLE_LIST = new String[] {"dataHandle1", "dataHandle2"};
  private static final String DATA_HANDLE_BUCKET = "bucket";
  private static final String POSTBACK_URL = "http://postback.com";
  private static final String ACCOUNT_IDENTITY = "service-account@testing.com";
  private static final String ATTRIBUTION_REPORT_TO = "https://foo.com";
  private static final Integer DEBUG_PRIVACY_BUDGET_LIMIT = 5;
  private static final Instant REQUEST_RECEIVED_AT = Instant.parse("2019-10-01T08:25:24.00Z");
  private static final Instant REQUEST_UPDATED_AT = Instant.parse("2019-10-01T08:29:24.00Z");

  private static final String JOB_PARAM_ATTRIBUTION_REPORT_TO = "attribution_report_to";
  private static final String JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT = "debug_privacy_budget_limit";

  private static final com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus
      JOB_STATUS_NEW_IMAGE_SHARED =
          com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.IN_PROGRESS;
  private static final com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus
      JOB_STATUS_OLD_IMAGE_SHARED =
          com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;
  private static final int RECORD_VERSION_NEW_IMAGE = 2;
  private static final int RECORD_VERSION_OLD_IMAGE = 1;
  private static final ImmutableList<ErrorCount> ERROR_COUNTS =
      ImmutableList.of(
          ErrorCount.newBuilder()
              .setCategory(JobErrorCategory.DECRYPTION_ERROR.name())
              .setCount(5L)
              .setDescription("Decryption error.")
              .build(),
          ErrorCount.newBuilder()
              .setCategory(JobErrorCategory.GENERAL_ERROR.name())
              .setCount(12L)
              .setDescription("General error.")
              .build(),
          ErrorCount.newBuilder()
              .setCategory(JobErrorCategory.NUM_REPORTS_WITH_ERRORS.name())
              .setCount(17L)
              .setDescription("Total number of reports with error.")
              .build());
  private static final com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary
      ERROR_SUMMARY_SHARED =
          com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary.newBuilder()
              .addAllErrorCounts(ERROR_COUNTS)
              .build();

  private static final com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo
      RESULT_INFO_SHARED =
          com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo.newBuilder()
              .setErrorSummary(ERROR_SUMMARY_SHARED)
              .setFinishedAt(ProtoUtil.toProtoTimestamp(Instant.parse("2019-10-01T13:25:24.00Z")))
              .setReturnCode(
                  com.google.aggregate.protos.shared.backend.ReturnCodeProto.ReturnCode.SUCCESS
                      .name())
              .setReturnMessage("Aggregation job successfully processed")
              .build();

  /**
   * Generate a stream record with a new image that matches the one returned by
   * createFakeJobMetadata. The old image is the same fields as the new image but has
   * jobStatus=RECEIVED and recordVersion=1.
   */
  public static DynamodbStreamRecord createFakeDynamodbStreamRecord(String requestId) {
    StreamRecord streamRecord = new StreamRecord();
    DynamodbStreamRecord dynamodbStreamRecord = new DynamodbStreamRecord();
    dynamodbStreamRecord.setDynamodb(streamRecord);
    ImmutableMap<String, AttributeValue> createJobRequestMap =
        ImmutableMap.<String, AttributeValue>builder()
            .put("JobRequestId", new AttributeValue().withS(requestId))
            .put("InputDataBlobPrefix", new AttributeValue().withS(DATA_HANDLE))
            .put("InputDataBlobBucket", new AttributeValue().withS(DATA_HANDLE_BUCKET))
            .put("OutputDataBlobPrefix", new AttributeValue().withS(DATA_HANDLE))
            .put("OutputDataBlobBucket", new AttributeValue().withS(DATA_HANDLE_BUCKET))
            .put("PostbackUrl", new AttributeValue().withS(POSTBACK_URL))
            .put("AttributionReportTo", new AttributeValue().withS(ATTRIBUTION_REPORT_TO))
            .put(
                "DebugPrivacyBudgetLimit",
                new AttributeValue().withN(DEBUG_PRIVACY_BUDGET_LIMIT.toString()))
            .put(
                "JobParameters",
                new AttributeValue()
                    .withM(
                        ImmutableMap.of(
                            JOB_PARAM_ATTRIBUTION_REPORT_TO,
                            attributeValueS(ATTRIBUTION_REPORT_TO),
                            JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
                            attributeValueS(DEBUG_PRIVACY_BUDGET_LIMIT.toString()))))
            .build();

    JobKey jobKey = JobKey.newBuilder().setJobRequestId(requestId).build();

    ImmutableMap<String, AttributeValue> requestInfoMap =
        ImmutableMap.<String, AttributeValue>builder()
            .put("JobRequestId", new AttributeValue().withS(requestId))
            .put("InputDataBlobPrefix", new AttributeValue().withS(DATA_HANDLE))
            .put("InputDataBlobBucket", new AttributeValue().withS(DATA_HANDLE_BUCKET))
            .put("OutputDataBlobPrefix", new AttributeValue().withS(DATA_HANDLE))
            .put("OutputDataBlobBucket", new AttributeValue().withS(DATA_HANDLE_BUCKET))
            .put("PostbackUrl", new AttributeValue().withS(POSTBACK_URL))
            .put(
                "JobParameters",
                new AttributeValue()
                    .withM(
                        ImmutableMap.of(
                            JOB_PARAM_ATTRIBUTION_REPORT_TO,
                            attributeValueS(ATTRIBUTION_REPORT_TO),
                            JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
                            attributeValueS(DEBUG_PRIVACY_BUDGET_LIMIT.toString()))))
            .build();

    AttributeValue resultInfo =
        new AttributeValue()
            .withM(
                ImmutableMap.of(
                    "ReturnMessage",
                    new AttributeValue().withS(RESULT_INFO_SHARED.getReturnMessage()),
                    "FinishedAt",
                    new AttributeValue()
                        .withS(toJavaInstant(RESULT_INFO_SHARED.getFinishedAt()).toString()),
                    "ReturnCode",
                    new AttributeValue().withS(RESULT_INFO_SHARED.getReturnCode().toString()),
                    "ErrorSummary",
                    createFakeErrorSummaryAttributeValue()));

    ImmutableMap<String, AttributeValue> commonImageValues =
        ImmutableMap.<String, AttributeValue>builder()
            .put("JobKey", new AttributeValue().withS(jobKey.getJobRequestId()))
            .put("CreateJobRequest", new AttributeValue().withM(createJobRequestMap))
            .put("RequestReceivedAt", new AttributeValue().withS(REQUEST_RECEIVED_AT.toString()))
            .put("NumAttempts", new AttributeValue().withN("0"))
            .put("RequestInfo", new AttributeValue().withM(requestInfoMap))
            .put("ResultInfo", resultInfo)
            .build();

    ImmutableMap<String, AttributeValue> oldImage =
        ImmutableMap.<String, AttributeValue>builder()
            .putAll(commonImageValues)
            .put("JobStatus", new AttributeValue().withS(JOB_STATUS_OLD_IMAGE_SHARED.toString()))
            .put("RequestUpdatedAt", new AttributeValue().withS(REQUEST_RECEIVED_AT.toString()))
            .put(
                "RecordVersion",
                new AttributeValue().withS(String.valueOf(RECORD_VERSION_OLD_IMAGE)))
            .build();

    ImmutableMap<String, AttributeValue> newImage =
        ImmutableMap.<String, AttributeValue>builder()
            .putAll(commonImageValues)
            .put("JobStatus", new AttributeValue().withS(JOB_STATUS_NEW_IMAGE_SHARED.toString()))
            .put("RequestUpdatedAt", new AttributeValue().withS(REQUEST_UPDATED_AT.toString()))
            .put(
                "RecordVersion",
                new AttributeValue().withS(String.valueOf(RECORD_VERSION_NEW_IMAGE)))
            .build();

    streamRecord.setOldImage(oldImage);
    streamRecord.setNewImage(newImage);
    streamRecord.setApproximateCreationDateTime(Date.from(REQUEST_RECEIVED_AT));
    streamRecord.setKeys(
        ImmutableMap.of("JobKey", new AttributeValue().withS(jobKey.getJobRequestId())));
    return dynamodbStreamRecord;
  }

  /** Creates an instance of the {@code JobMetadata} class with fake values. */
  public static JobMetadata createFakeJobMetadata(String requestId) {
    JobKey jobKey =
        JobKey.newBuilder()
            .setJobRequestId(requestId)
            .build();

    com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest
        createJobRequest = createFakeCreateJobRequestShared(requestId);

    RequestInfo requestInfo = createFakeRequestInfo(requestId);

    return JobMetadata.newBuilder()
        .setCreateJobRequest(createJobRequest)
        .setJobStatus(JOB_STATUS_NEW_IMAGE_SHARED)
        .setRequestReceivedAt(ProtoUtil.toProtoTimestamp(REQUEST_RECEIVED_AT))
        .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(REQUEST_UPDATED_AT))
        .setNumAttempts(0)
        .setJobKey(jobKey)
        .setRecordVersion(RECORD_VERSION_NEW_IMAGE)
        .setRequestInfo(requestInfo)
        .setResultInfo(RESULT_INFO_SHARED)
        .build();
  }

  /** Creates an instance of the {@code CreateJobRequest} class with fake values. */
  public static com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest
      createFakeCreateJobRequestShared(String requestId) {

    return com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest
        .newBuilder()
        .setJobRequestId(requestId)
        .setInputDataBlobPrefix(DATA_HANDLE)
        .setInputDataBucketName(DATA_HANDLE_BUCKET)
        .setOutputDataBlobPrefix(DATA_HANDLE)
        .setOutputDataBucketName(DATA_HANDLE_BUCKET)
        .setPostbackUrl(POSTBACK_URL)
        .setAttributionReportTo(ATTRIBUTION_REPORT_TO)
        .setDebugPrivacyBudgetLimit(DEBUG_PRIVACY_BUDGET_LIMIT)
        .putAllJobParameters(
            ImmutableMap.of(
                JOB_PARAM_ATTRIBUTION_REPORT_TO,
                ATTRIBUTION_REPORT_TO,
                JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
                DEBUG_PRIVACY_BUDGET_LIMIT.toString()))
        .build();
  }

  public static RequestInfo createFakeRequestInfo(String requestId) {
    return RequestInfo.newBuilder()
        .setJobRequestId(requestId)
        .setInputDataBlobPrefix(DATA_HANDLE)
        .setInputDataBucketName(DATA_HANDLE_BUCKET)
        .setOutputDataBlobPrefix(DATA_HANDLE)
        .setOutputDataBucketName(DATA_HANDLE_BUCKET)
        .setPostbackUrl(POSTBACK_URL)
        .putAllJobParameters(
            ImmutableMap.of(
                JOB_PARAM_ATTRIBUTION_REPORT_TO,
                ATTRIBUTION_REPORT_TO,
                JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
                DEBUG_PRIVACY_BUDGET_LIMIT.toString()))
        .build();
  }

  public static RequestInfo createFakeRequestInfoWithPrefixList(String requestId) {
    return RequestInfo.newBuilder()
        .setJobRequestId(requestId)
        .setInputDataBucketName(DATA_HANDLE_BUCKET)
        .addAllInputDataBlobPrefixes(ImmutableList.copyOf(DATA_HANDLE_LIST))
        .setOutputDataBucketName(DATA_HANDLE_BUCKET)
        .setPostbackUrl(POSTBACK_URL)
        .putAllJobParameters(
            ImmutableMap.of(
                JOB_PARAM_ATTRIBUTION_REPORT_TO,
                ATTRIBUTION_REPORT_TO,
                JOB_PARAM_DEBUG_PRIVACY_BUDGET_LIMIT,
                DEBUG_PRIVACY_BUDGET_LIMIT.toString()))
        .build();
  }

  public static RequestInfo createFakeRequestInfoWithAccountIdentity(String requestId) {
    return createFakeRequestInfo(requestId).toBuilder()
        .setAccountIdentity(ACCOUNT_IDENTITY)
        .build();
  }

  /** Creates an instance of the {@code ResultInfo} class with fake values. */
  public static com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo
      createFakeResultInfoShared() {
    return RESULT_INFO_SHARED;
  }

  /** Creates an instance of the {@code ErrorSummary} class with fake values. */
  public static com.google.aggregate.protos.shared.backend.ErrorSummaryProto.ErrorSummary
      createFakeErrorSummaryShared() {
    return ERROR_SUMMARY_SHARED;
  }

  /** Creates a list of {@code ErrorCount} instances with fake values. */
  public static ImmutableList<ErrorCount> createFakeErrorCounts() {
    return ERROR_COUNTS;
  }

  private static AttributeValue createFakeErrorSummaryAttributeValue() {
    return new AttributeValue()
        .withM(
            ImmutableMap.of(
                "ErrorCounts",
                new AttributeValue()
                    .withL(
                        RESULT_INFO_SHARED.getErrorSummary().getErrorCountsList().stream()
                            .map(
                                i ->
                                    new AttributeValue()
                                        .withM(
                                            ImmutableMap.of(
                                                "Category",
                                                new AttributeValue()
                                                    .withS(i.getCategory().toString()),
                                                "Count",
                                                new AttributeValue()
                                                    .withN(Long.toString(i.getCount())),
                                                "Description",
                                                new AttributeValue()
                                                    .withS(i.getDescription().toString()))))
                            .collect(toImmutableList()))));
  }

  private static AttributeValue attributeValueS(String s) {
    return new AttributeValue().withS(s);
  }
}
