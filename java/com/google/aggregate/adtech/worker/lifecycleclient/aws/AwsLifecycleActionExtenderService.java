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

package com.google.aggregate.adtech.worker.lifecycleclient.aws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatEnabled;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatFrequency;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatTimeout;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.MaxLifecycleActionTimeoutExtension;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.WorkerAutoscalingGroupName;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.WorkerScaleInHookName;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import com.google.scp.shared.proto.ProtoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class AwsLifecycleActionExtenderService extends AbstractExecutionThreadService {

  private static final Logger logger =
      LoggerFactory.getLogger(AwsLifecycleActionExtenderService.class);
  DynamoAsgInstancesDb dynamoAsgInstancesDb;
  AutoScalingClient autoScalingClient;
  private final Clock clock;
  private final Optional<String> workerScaleInHook;
  private final Optional<String> workerAutoscalingGroup;
  Provider<Boolean> lifecycleActionHeartbeatEnabled;
  Provider<Optional<Integer>> lifecycleActionHeartbeatTimeout;
  Provider<Optional<Integer>> maxLifecycleActionTimeoutExtension;
  Provider<Optional<Integer>> lifecycleActionHeartbeatFrequency;

  /** Default value for thread run frequency in seconds. 30 minutes. */
  Integer DEFAULT_THREAD_SLEEP_SEC = 30 * 60;

  /** Sleep one minute before retry handling heartbeat lifecycle action after failure. */
  Integer FAIL_SLEEP_MILLIS = 60 * 1000;

  /**
   * A service to check if the current EC2 instance is in a TERMINATING_WAIT state and extend the
   * termination timeout by recording lifecycle action heartbeats.
   */
  @Inject
  AwsLifecycleActionExtenderService(
      AutoScalingClient autoScalingClient,
      DynamoAsgInstancesDb dynamoAsgInstancesDb,
      Clock clock,
      @WorkerScaleInHookName Optional<String> workerScaleInHook,
      @WorkerAutoscalingGroupName Optional<String> workerAutoscalingGroup,
      @LifecycleActionHeartbeatEnabled Provider<Boolean> lifecycleActionHeartbeatEnabled,
      @LifecycleActionHeartbeatTimeout Provider<Optional<Integer>> lifecycleActionHeartbeatTimeout,
      @MaxLifecycleActionTimeoutExtension
          Provider<Optional<Integer>> maxLifecycleActionTimeoutExtension,
      @LifecycleActionHeartbeatFrequency
          Provider<Optional<Integer>> lifecycleActionHeartbeatFrequency) {
    this.dynamoAsgInstancesDb = dynamoAsgInstancesDb;
    this.autoScalingClient = autoScalingClient;
    this.clock = clock;
    this.workerScaleInHook = workerScaleInHook;
    this.workerAutoscalingGroup = workerAutoscalingGroup;
    this.lifecycleActionHeartbeatEnabled = lifecycleActionHeartbeatEnabled;
    this.lifecycleActionHeartbeatTimeout = lifecycleActionHeartbeatTimeout;
    this.maxLifecycleActionTimeoutExtension = maxLifecycleActionTimeoutExtension;
    this.lifecycleActionHeartbeatFrequency = lifecycleActionHeartbeatFrequency;
  }

  /** Periodically handle heartbeat lifecycle action, if enabled. */
  @Override
  protected void run() throws InterruptedException {
    while (true) {
      try {
        if (lifecycleActionHeartbeatEnabled.get()) {
          handleHeartbeatLifecycleAction();
        }
        Integer threadSleepTimeSec =
            lifecycleActionHeartbeatFrequency.get().orElse(DEFAULT_THREAD_SLEEP_SEC);
        Thread.sleep(threadSleepTimeSec * 1000);
      } catch (InterruptedException e) {
        logger.info("AWS Lifecycle Action Extender service interrupted.", e);
        throw e;
      } catch (Exception e) {
        logger.info(
            "AWS Lifecycle Action Extender Service run failed. Will be retried shortly.", e);
        Thread.sleep(FAIL_SLEEP_MILLIS);
      }
    }
  }

  /** Gather the necessary parameters and send lifecycle action heartbeat if needed. */
  private void handleHeartbeatLifecycleAction() throws AsgInstanceDaoException {
    String instanceId = EC2MetadataUtils.getInstanceId();
    Optional<AsgInstance> asgInstance = dynamoAsgInstancesDb.getAsgInstance(instanceId);

    // If parameters are not populated, assume lifecycle extension is disabled.
    if (lifecycleActionHeartbeatTimeout.get().isEmpty()
        || maxLifecycleActionTimeoutExtension.get().isEmpty()
        || workerAutoscalingGroup.isEmpty()
        || workerScaleInHook.isEmpty()) {
      logger.info("Lifecycle heartbeat parameters are not populated.");
      return;
    }

    Integer maxHeartbeatExtension = maxLifecycleActionTimeoutExtension.get().get();
    Integer heartbeatTimeout = lifecycleActionHeartbeatTimeout.get().get();

    if (asgInstance.isPresent()
        && shouldHeartbeat(asgInstance.get(), maxHeartbeatExtension, heartbeatTimeout)) {
      sendHeartbeat(asgInstance.get(), workerAutoscalingGroup.get(), workerScaleInHook.get());
    }
  }

  /**
   * Determines if a lifecycle action heartbeat is needed by checking if the heartbeat timeout is
   * approaching and that the max timeout has not been exceeded.
   */
  @VisibleForTesting
  boolean shouldHeartbeat(
      AsgInstance asgInstance, Integer maxHeartbeatExtension, Integer heartbeatTimeout) {
    if (!InstanceStatus.TERMINATING_WAIT.equals(asgInstance.getStatus())
        && !asgInstance.hasLastHeartbeatTime()) {
      return false;
    }

    Instant currentTime = Instant.now(clock);
    Instant lastHeartbeatTime = ProtoUtil.toJavaInstant(asgInstance.getLastHeartbeatTime());
    Instant maxExtensionTime =
        ProtoUtil.toJavaInstant(asgInstance.getRequestTime())
            .plus(maxHeartbeatExtension, ChronoUnit.SECONDS);
    Boolean hasNotExceededMaxTimeout =
        maxExtensionTime.isAfter(lastHeartbeatTime.plus(heartbeatTimeout, ChronoUnit.SECONDS))
            && maxExtensionTime.isAfter(currentTime);
    return hasNotExceededMaxTimeout;
  }

  /** Record a lifecycle action heartbeat and update the AsgInstances table lastHeartbeatTime. */
  @VisibleForTesting
  void sendHeartbeat(AsgInstance asgInstance, String autoscalingGroup, String scaleInLifecycleHook)
      throws AsgInstanceDaoException {
    logger.info(
        "Sending lifecycle action heartbeat. Last heartbeat time: "
            + ProtoUtil.toJavaInstant(asgInstance.getLastHeartbeatTime())
            + " Current time: "
            + Instant.now(clock));
    RecordLifecycleActionHeartbeatRequest heartbeatRequest =
        RecordLifecycleActionHeartbeatRequest.builder()
            .lifecycleHookName(scaleInLifecycleHook)
            .autoScalingGroupName(autoscalingGroup)
            .instanceId(asgInstance.getInstanceName())
            .build();
    autoScalingClient.recordLifecycleActionHeartbeat(heartbeatRequest);
    Instant newHeartbeatTime = Instant.now(clock);
    AsgInstance updatedAsgInstance =
        asgInstance.toBuilder()
            .setLastHeartbeatTime(ProtoUtil.toProtoTimestamp(newHeartbeatTime))
            .build();
    dynamoAsgInstancesDb.updateAsgInstance(updatedAsgInstance);
  }
}
