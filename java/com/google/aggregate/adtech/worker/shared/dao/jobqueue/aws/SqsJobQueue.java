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

package com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws;

import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.Constants.JSON_BODY_TYPE;
import static com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.Constants.MESSAGE_BODY_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.JsonFormat;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobMessageProto.JobMessage;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.scp.shared.proto.ProtoUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** SQS-Backed implementation of the {@code JobQueue}. */
public final class SqsJobQueue implements JobQueue {

  // SQS allows for batch receipt of messages, the worker should only receive single messages so
  // this is set to 1.
  private static final int MAX_NUMBER_OF_MESSAGES_RECEIVED = 1;

  private static final Logger logger = LoggerFactory.getLogger(SqsJobQueue.class);
  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();

  private final SqsClient sqsClient;
  private final Provider<String> queueUrl;
  // Max time to wait to receive messages for. See AWS docs for more detail.
  private final int maxWaitTimeSeconds;
  // The "lease" length that the item receipt has. If the item is not acknowledged (deleted) within
  // this time window it will be visible on the queue again for another worker to pick up. See AWS
  // docs for more detail.
  private final int visibilityTimeoutSeconds;

  /** Creates a new instance of the {@code SqsJobQueue} class. */
  @Inject
  SqsJobQueue(
      SqsClient sqsClient,
      @JobQueueSqsQueueUrl Provider<String> queueUrl,
      @JobQueueSqsMaxWaitTimeSeconds int maxWaitTimeSeconds,
      @JobQueueMessageLeaseSeconds int visibilityTimeoutSeconds) {
    this.sqsClient = sqsClient;
    this.queueUrl = queueUrl;
    this.maxWaitTimeSeconds = maxWaitTimeSeconds;
    this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
  }

  @Override
  public void sendJob(JobKey jobKey, String serverJobId) throws JobQueueException {
    try {
      JobMessage jobMessage =
          JobMessage.newBuilder()
              .setJobRequestId(jobKey.getJobRequestId())
              .setServerJobId(serverJobId)
              .build();

      String messageBody = JSON_PRINTER.print(jobMessage);
      SendMessageRequest sendMessageRequest =
          SendMessageRequest.builder()
              .queueUrl(queueUrl.get())
              .messageBody(messageBody)
              .messageAttributes(
                  Map.of(
                      MESSAGE_BODY_TYPE,
                      MessageAttributeValue.builder()
                          .dataType("String")
                          .stringValue(JSON_BODY_TYPE)
                          .build()))
              .build();

      sqsClient.sendMessage(sendMessageRequest);
      logger.info("Placed job on queue: " + jobKey.getJobRequestId());
    } catch (SdkException | InvalidProtocolBufferException e) {
      throw new JobQueueException(e);
    }
  }

  @Override
  public Optional<JobQueueItem> receiveJob() throws JobQueueException {
    ReceiveMessageRequest receiveMessageRequest =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl.get())
            .maxNumberOfMessages(MAX_NUMBER_OF_MESSAGES_RECEIVED)
            .waitTimeSeconds(maxWaitTimeSeconds)
            .visibilityTimeout(visibilityTimeoutSeconds)
            .messageAttributeNames("All")
            .build();

    try {
      Optional<Message> receivedJobMessage =
          sqsClient.receiveMessage(receiveMessageRequest).messages().stream().collect(toOptional());

      Optional<JobQueueItem> receivedJob = Optional.empty();
      if (receivedJobMessage.isPresent()) {
        receivedJob = buildJobQueueItem(receivedJobMessage.get());
      }
      if (receivedJob.isPresent()) {
        logger.info(
            "Received job from queue: "
                + receivedJob.get().getJobKeyString()
                + " with server job id: "
                + receivedJob.get().getServerJobId());
      } else {
        logger.info("No job received from queue");
      }
      return receivedJob;
    } catch (SdkException | InvalidProtocolBufferException e) {
      throw new JobQueueException(e);
    }
  }

  @Override
  public void acknowledgeJobCompletion(JobQueueItem jobQueueItem) throws JobQueueException {
    DeleteMessageRequest deleteMessageRequest =
        DeleteMessageRequest.builder()
            .queueUrl(queueUrl.get())
            .receiptHandle(jobQueueItem.getReceiptInfo())
            .build();

    try {
      sqsClient.deleteMessage(deleteMessageRequest);
      logger.info("Reporting processing completion for job: " + jobQueueItem.getJobKeyString());
    } catch (SdkException e) {
      throw new JobQueueException(e);
    }
  }

  @Override
  public void modifyJobProcessingTime(JobQueueItem jobQueueItem, Duration processingTime)
      throws JobQueueException {
    ChangeMessageVisibilityRequest changeMessageVisibilityRequest =
        ChangeMessageVisibilityRequest.builder()
            .queueUrl(queueUrl.get())
            .receiptHandle(jobQueueItem.getReceiptInfo())
            .visibilityTimeout((int) processingTime.toSeconds())
            .build();

    try {
      sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
      logger.info("Updating processing time for job: " + jobQueueItem.getJobKeyString());
    } catch (SdkException e) {
      throw new JobQueueException(e);
    }
  }

  /**
   * Builds the JobQueueItem from the message received from the SQS Job Queue. The BodyType message
   * attribute is checked for backwards compatibility where older versions only had the job request
   * id as a string in the message body.
   */
  private Optional<JobQueueItem> buildJobQueueItem(Message jobMessage)
      throws InvalidProtocolBufferException, JobQueueException {
    String jobRequestId;
    String serverJobId = "";
    MessageAttributeValue bodyType = jobMessage.messageAttributes().get(MESSAGE_BODY_TYPE);

    // if null, this is an old job so body only contains job request id as a String
    if (bodyType == null) {
      jobRequestId = jobMessage.body();
    } else if (JSON_BODY_TYPE.equals(bodyType.stringValue())) {
      JobMessage.Builder builder = JobMessage.newBuilder();
      JSON_PARSER.merge(jobMessage.body(), builder);
      JobMessage messageBody = builder.build();

      jobRequestId = messageBody.getJobRequestId();
      serverJobId = messageBody.getServerJobId();
    } else {
      throw new JobQueueException("Invalid message body type: " + bodyType);
    }
    return Optional.of(
        JobQueueItem.newBuilder()
            .setJobKeyString(jobRequestId)
            .setServerJobId(serverJobId)
            .setJobProcessingTimeout(Durations.fromSeconds(visibilityTimeoutSeconds))
            .setJobProcessingStartTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setReceiptInfo(jobMessage.receiptHandle())
            .build());
  }

  /** The URL of the SQS queue in AWS. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface JobQueueSqsQueueUrl {}

  /** The value for the SQS Max Wait Time on message receipt. See AWS docs for more information. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface JobQueueSqsMaxWaitTimeSeconds {}
}
