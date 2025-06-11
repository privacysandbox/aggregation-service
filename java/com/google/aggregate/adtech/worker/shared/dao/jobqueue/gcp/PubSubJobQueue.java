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

package com.google.aggregate.adtech.worker.shared.dao.jobqueue.gcp;

import static com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.Constants.JSON_BODY_TYPE;
import static com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.Constants.MESSAGE_BODY_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.JsonFormat;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobMessageProto.JobMessage;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.model.BackendModelUtil;
import com.google.scp.shared.proto.ProtoUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pub/Sub-Backed implementation of the {@code JobQueue}. */
public final class PubSubJobQueue implements JobQueue {

  private static final Logger logger = LoggerFactory.getLogger(PubSubJobQueue.class);
  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();

  // PubSub allows for batch receipt of messages, the worker should only receive single messages so
  // this is set to 1.
  private static final int MAX_NUMBER_OF_MESSAGES_RECEIVED = 1;

  private final SubscriberStub subscriber;
  private final Publisher publisher;
  private final Provider<String> subscriptionName;
  // The "lease" length that the item receipt has. If the item is not acknowledged (deleted) within
  // this time window it will be visible on the queue again for another worker to pick up. See GCP
  // docs for more detail.
  private final int messageLeaseSeconds;

  /** Creates a new instance of the {@code PubSubJobQueue} class. */
  @Inject
  PubSubJobQueue(
      Publisher publisher,
      SubscriberStub subscriber,
      @JobQueuePubSubSubscriptionName Provider<String> subscriptionName,
      @JobQueueMessageLeaseSeconds int messageLeaseSeconds) {
    this.publisher = publisher;
    this.subscriber = subscriber;
    this.subscriptionName = subscriptionName;
    this.messageLeaseSeconds = messageLeaseSeconds;
  }

  @Override
  public void sendJob(JobKey jobKey, String serverJobId) throws JobQueueException {
    Optional<String> messageId;
    ByteString data;

    try {
      JobMessage jobMessage =
          JobMessage.newBuilder()
              .setJobRequestId(jobKey.getJobRequestId())
              .setServerJobId(serverJobId)
              .build();
      data = ByteString.copyFromUtf8(JSON_PRINTER.print(jobMessage));
      PubsubMessage pubsubMessage =
          PubsubMessage.newBuilder()
              .setData(data)
              .putAttributes(MESSAGE_BODY_TYPE, JSON_BODY_TYPE)
              .build();

      ApiFuture<String> publisherFuture = publisher.publish(pubsubMessage);

      messageId = Optional.of(publisherFuture.get());
    } catch (ApiException
        | ExecutionException
        | InterruptedException
        | InvalidProtocolBufferException e) {
      throw new JobQueueException(e);
    }

    logger.info(
        String.format(
            "Job '%s' was successfully added to job queue with message ID '%s'.",
            BackendModelUtil.toJobKeyString(jobKey), messageId));
  }

  /**
   * Synchronously pulls a job from Pub/Sub job queue.
   *
   * <p>Pulls a job from job queue, and sets the lease time-out of the job.
   *
   * @return an {@code Optional} of a {@code JobQueueItem} that will be empty if there are no jobs
   *     on the queue.
   */
  @Override
  public Optional<JobQueueItem> receiveJob() throws JobQueueException {
    try {
      PullRequest pullRequest =
          PullRequest.newBuilder()
              .setMaxMessages(MAX_NUMBER_OF_MESSAGES_RECEIVED)
              .setSubscription(subscriptionName.get())
              .build();

      PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

      pullResponse.getReceivedMessagesList().stream()
          .forEach(
              message -> {
                // Modify the ack deadline of each received message from the default.
                ModifyAckDeadlineRequest modifyAckDeadlineRequest =
                    ModifyAckDeadlineRequest.newBuilder()
                        .setSubscription(subscriptionName.get())
                        .addAckIds(message.getAckId())
                        // max 10 minutes for pubsub ack deadline
                        .setAckDeadlineSeconds(Math.min(600, messageLeaseSeconds))
                        .build();

                subscriber.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
              });

      Optional<ReceivedMessage> receivedJobMessage =
          pullResponse.getReceivedMessagesList().stream().findFirst();

      Optional<JobQueueItem> receivedJob = Optional.empty();
      if (receivedJobMessage.isPresent()) {
        receivedJob = buildJobQueueItem(receivedJobMessage.get());
      }
      if (receivedJob.isPresent()) {
        logger.info("Received job from queue:" + receivedJob.get().getJobKeyString());
      } else {
        logger.info("No job received from queue");
      }
      return receivedJob;
    } catch (ApiException | InvalidProtocolBufferException e) {
      throw new JobQueueException(e);
    }
  }

  /** Synchronously acknowledges the processing of a job on Pub/Sub job queue. */
  @Override
  public void acknowledgeJobCompletion(JobQueueItem jobQueueItem) throws JobQueueException {
    try {
      AcknowledgeRequest acknowledgeRequest =
          AcknowledgeRequest.newBuilder()
              .setSubscription(subscriptionName.get())
              .addAckIds(jobQueueItem.getReceiptInfo())
              .build();

      subscriber.acknowledgeCallable().call(acknowledgeRequest);
    } catch (ApiException e) {
      throw new JobQueueException(e);
    }
  }

  @Override
  public void modifyJobProcessingTime(JobQueueItem jobQueueItem, Duration processingTime)
      throws JobQueueException {
    try {
      ModifyAckDeadlineRequest modifyAckDeadlineRequest =
          ModifyAckDeadlineRequest.newBuilder()
              .setSubscription(subscriptionName.get())
              .setAckDeadlineSeconds((int) processingTime.toSeconds())
              .addAckIds(jobQueueItem.getReceiptInfo())
              .build();

      subscriber.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
    } catch (ApiException e) {
      throw new JobQueueException(e);
    }
  }

  private Optional<JobQueueItem> buildJobQueueItem(ReceivedMessage jobMessage)
      throws InvalidProtocolBufferException {
    logger.info("Received job message body: " + jobMessage.getMessage().getData().toStringUtf8());

    JobMessage.Builder builder = JobMessage.newBuilder();
    JSON_PARSER.merge(jobMessage.getMessage().getData().toStringUtf8(), builder);
    JobMessage messageBody = builder.build();

    return Optional.of(
        JobQueueItem.newBuilder()
            .setJobKeyString(messageBody.getJobRequestId())
            .setServerJobId(messageBody.getServerJobId())
            .setJobProcessingTimeout(Durations.fromSeconds(messageLeaseSeconds))
            .setJobProcessingStartTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setReceiptInfo(jobMessage.getAckId())
            .build());
  }

  /** The subscription name for the Pub/Sub queue. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface JobQueuePubSubSubscriptionName {}

  /** Topic ID of the Pub/Sub queue. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface JobQueuePubSubTopicId {}

  /** Maximum message size in bytes. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface JobQueuePubSubMaxMessageSizeBytes {}
}
