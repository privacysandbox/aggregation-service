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

package com.google.aggregate.adtech.worker.autoscaling.tasks.aws;

import com.google.inject.Inject;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb;
import com.google.scp.shared.proto.ProtoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingInstanceDetails;
import software.amazon.awssdk.services.autoscaling.model.CompleteLifecycleActionRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Instant;

/** Determines whether to complete the EC2 Instance termination lifecycle action. */
public class ManageTerminatedInstanceTask {

  private static final Logger logger = LoggerFactory.getLogger(ManageTerminatedInstanceTask.class);
  private final AutoScalingClient autoScalingClient;
  private final DynamoAsgInstancesDb dynamoAsgInstancesDb;
  private final Clock clock;

  @Inject
  public ManageTerminatedInstanceTask(
      AutoScalingClient autoScalingClient, DynamoAsgInstancesDb dynamoAsgInstancesDb, Clock clock) {
    this.autoScalingClient = autoScalingClient;
    this.dynamoAsgInstancesDb = dynamoAsgInstancesDb;
    this.clock = clock;
  }

  /**
   * Checks the terminating EC2 Instance's lifecycle and health status. If the lifecycle state is
   * Terminating:Wait and health status is UNHEALTHY, complete the lifecycle action to continue with
   * instance termination and return true. If the instance is in Terminating:Wait state and health
   * status is Healthy, insert a record into the AsgInstances table to let the worker handle the
   * instance termination and return true. Otherwise, return false.
   */
  public Boolean manageTerminatedInstance(
      String asgName, String instanceId, String lifecycleHookName, String lifecycleActionToken) {
    AutoScalingInstanceDetails instanceDetails = getInstanceDetails(instanceId);
    logger.info(
        String.format(
            "EC2 Instance %s has lifecycle state %s and health status %s.",
            instanceId, instanceDetails.lifecycleState(), instanceDetails.healthStatus()));

    if (instanceDetails.lifecycleState().equals(LifecycleState.TERMINATING_WAIT.toString())) {
      if (instanceDetails.healthStatus().equals("UNHEALTHY")) {
        logger.info(String.format("Completing lifecycle action for instance: %s.", instanceId));
        completeLifecycleAction(asgName, instanceId, lifecycleHookName, lifecycleActionToken);
        return true;
      } else {
        logger.info(
            String.format(
                "Inserting instance %s to the AsgInstances table to be handled by the worker.",
                instanceId));
        try {
          Instant currentTime = Instant.now(clock);
          AsgInstance asgInstance =
              AsgInstance.newBuilder()
                  .setInstanceName(instanceId)
                  .setStatus(InstanceStatus.TERMINATING_WAIT)
                  .setRequestTime(ProtoUtil.toProtoTimestamp(currentTime))
                  .setLastHeartbeatTime(ProtoUtil.toProtoTimestamp(currentTime))
                  .build();
          dynamoAsgInstancesDb.upsertAsgInstance(asgInstance);
          return true;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    logger.info("Lifecycle action completion skipped.");
    return false;
  }

  @Nullable
  private AutoScalingInstanceDetails getInstanceDetails(String instanceId) {
    DescribeAutoScalingInstancesRequest describeInstanceRequest =
        DescribeAutoScalingInstancesRequest.builder().instanceIds(instanceId).build();
    DescribeAutoScalingInstancesResponse describeInstanceResponse =
        autoScalingClient.describeAutoScalingInstances(describeInstanceRequest);

    if (describeInstanceResponse.autoScalingInstances().isEmpty()) {
      logger.info(
          String.format(
              "EC2 Instance %s has already been terminated or does not exist.", instanceId));
      return null;
    }
    return describeInstanceResponse.autoScalingInstances().get(0);
  }

  private void completeLifecycleAction(
      String asgName, String instanceId, String lifecycleHookName, String lifecycleActionToken) {
    CompleteLifecycleActionRequest completeLifecycleActionRequest =
        CompleteLifecycleActionRequest.builder()
            .instanceId(instanceId)
            .autoScalingGroupName(asgName)
            .lifecycleHookName(lifecycleHookName)
            .lifecycleActionToken(lifecycleActionToken)
            .lifecycleActionResult("CONTINUE")
            .build();
    autoScalingClient.completeLifecycleAction(completeLifecycleActionRequest);
    logger.info(
        String.format("EC2 Instance %s termination lifecycle action completed.", instanceId));
  }
}
