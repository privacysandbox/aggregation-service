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

import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb.AsgInstancesDbDynamoTableName;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.clients.configclient.model.WorkerParameter;
import com.google.scp.shared.proto.ProtoUtil;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.CompleteLifecycleActionRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/** Lifecycle Client for AWS Cloud instances. */
public final class AwsLifecycleClient implements LifecycleClient {

  private static final Logger logger = Logger.getLogger(AwsLifecycleClient.class.getName());
  private final AutoScalingClient autoScalingClient;
  private final ParameterClient parameterClient;
  private final DynamoAsgInstancesDb dynamoAsgInstancesDb;
  private final Clock clock;
  private final Boolean useAsgInstancesTable;

  /** Creates a new instance of the {@code AwsLifecycleClient} class. */
  @Inject
  AwsLifecycleClient(
      AutoScalingClient autoScalingClient,
      ParameterClient parameterClient,
      DynamoAsgInstancesDb dynamoAsgInstancesDb,
      Clock clock,
      @AsgInstancesDbDynamoTableName String asgInstancesTableName,
      AwsLifecycleActionExtenderService awsLifecycleActionExtenderService) {
    this.autoScalingClient = autoScalingClient;
    this.parameterClient = parameterClient;
    this.dynamoAsgInstancesDb = dynamoAsgInstancesDb;
    this.clock = clock;
    this.useAsgInstancesTable = !asgInstancesTableName.isEmpty();
    if (useAsgInstancesTable) {
      awsLifecycleActionExtenderService.startAsync();
    }
  }

  @Override
  public Optional<String> getLifecycleState() throws LifecycleClientException {
    String instanceId = EC2MetadataUtils.getInstanceId();
    try {
      DescribeAutoScalingInstancesRequest request =
          DescribeAutoScalingInstancesRequest.builder().instanceIds(instanceId).build();
      DescribeAutoScalingInstancesResponse response =
          autoScalingClient.describeAutoScalingInstances(request);
      return Optional.ofNullable(response.autoScalingInstances().get(0).lifecycleState());
    } catch (SdkException exception) {
      throw new LifecycleClientException(exception);
    }
  }

  @Override
  public boolean handleScaleInLifecycleAction() throws LifecycleClientException {
    Optional<String> scaleInLifecycleHook = Optional.empty();
    try {
      scaleInLifecycleHook = parameterClient.getParameter(WorkerParameter.SCALE_IN_HOOK.name());
    } catch (ParameterClientException e) {
      logger.info("WorkerParameter.SCALE_IN_HOOK not found" + e);
    }

    if (scaleInLifecycleHook.isEmpty() || scaleInLifecycleHook.get().isEmpty()) {
      return false;
    }

    try {
      String instanceId = EC2MetadataUtils.getInstanceId();

      if (useAsgInstancesTable) {
        return handleLifecycleActionUsingDb(instanceId, scaleInLifecycleHook.get());
      } else {
        return handleLifecycleActionUsingApi(instanceId, scaleInLifecycleHook.get());
      }
    } catch (SdkException | AsgInstanceDaoException | ParameterClientException exception) {
      throw new LifecycleClientException(exception);
    }
  }

  private void completeLifecycleAction(
      String autoScalingGroup, String instanceId, String scaleInLifecycleHook) {
    CompleteLifecycleActionRequest lifecycleRequest =
        CompleteLifecycleActionRequest.builder()
            .autoScalingGroupName(autoScalingGroup)
            .lifecycleActionResult("CONTINUE")
            .instanceId(instanceId)
            .lifecycleHookName(scaleInLifecycleHook)
            .build();
    autoScalingClient.completeLifecycleAction(lifecycleRequest);
  }

  private boolean handleLifecycleActionUsingDb(String instanceId, String scaleInLifecycleHook)
      throws ParameterClientException, AsgInstanceDaoException {
    Optional<String> autoscalingGroup =
        parameterClient.getParameter(WorkerParameter.WORKER_AUTOSCALING_GROUP.name());
    if (autoscalingGroup.isEmpty()) {
      return false;
    }
    Optional<AsgInstance> asgInstance = dynamoAsgInstancesDb.getAsgInstance(instanceId);
    if (asgInstance.isPresent()) {
      logger.info("Found asg instance record in the AsgInstances table: " + asgInstance.get());
      if (asgInstance.get().getStatus().equals(InstanceStatus.TERMINATING_WAIT)) {
        completeLifecycleAction(autoscalingGroup.get(), instanceId, scaleInLifecycleHook);
        AsgInstance updatedAsgInstance =
            AsgInstance.newBuilder(asgInstance.get())
                .setTerminationTime(ProtoUtil.toProtoTimestamp((Instant.now(clock))))
                .setStatus(InstanceStatus.TERMINATED)
                .build();
        dynamoAsgInstancesDb.updateAsgInstance(updatedAsgInstance);
      }
      return true;
    }
    return false;
  }

  private boolean handleLifecycleActionUsingApi(String instanceId, String scaleInLifecycleHook) {
    DescribeAutoScalingInstancesRequest request =
        DescribeAutoScalingInstancesRequest.builder().instanceIds(instanceId).build();
    DescribeAutoScalingInstancesResponse response =
        autoScalingClient.describeAutoScalingInstances(request);
    if (response.autoScalingInstances().isEmpty()) {
      return false;
    }
    String lifecycleState = response.autoScalingInstances().get(0).lifecycleState();
    String autoScalingGroup = response.autoScalingInstances().get(0).autoScalingGroupName();

    if (lifecycleState.equals(LifecycleState.TERMINATING_WAIT.toString())) {
      completeLifecycleAction(autoScalingGroup, instanceId, scaleInLifecycleHook);
      return true;
    }

    // Scale-in Lifecycle hook was already completed, no-op and return true for scale-in action.
    if (lifecycleState.equals(LifecycleState.TERMINATING_PROCEED.toString())) {
      return true;
    }
    return false;
  }
}
