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
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.Annotations.AsgName;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.Annotations.ScalingRatio;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.ResourceContentionException;
import software.amazon.awssdk.services.autoscaling.model.ScalingActivityInProgressException;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;

/**
 * Configures and sets the desired capacity for the worker Auto Scaling Group based on the total
 * messages from the SQS job queue.
 */
public class SetDesiredCapacityTask {

  private static final Logger logger = LoggerFactory.getLogger(SetDesiredCapacityTask.class);
  private final AutoScalingClient autoScalingClient;
  private final String asgName;
  private final Double scalingRatio;

  @Inject
  public SetDesiredCapacityTask(
      AutoScalingClient autoScalingClient,
      @AsgName String asgName,
      @ScalingRatio Double scalingRatio) {
    this.autoScalingClient = autoScalingClient;
    this.asgName = asgName;
    this.scalingRatio = scalingRatio;
  }

  /**
   * Applies the scaling ratio (worker instances : job messages) to the total SQS messages,
   * configures the scaled capacity to within the ASG range, and sets the desired capacity for the
   * ASG.
   */
  public Integer setAsgDesiredCapacity(Integer totalMessages) throws ServiceException {
    try {
      // Round up to prioritize job completion
      Integer scaledCapacity = (int) Math.ceil(totalMessages * scalingRatio);
      Integer desiredCapacity = validateCapacity(scaledCapacity);

      SetDesiredCapacityRequest setDesiredCapacityRequest =
          SetDesiredCapacityRequest.builder()
              .autoScalingGroupName(asgName)
              .desiredCapacity(desiredCapacity)
              .build();
      autoScalingClient.setDesiredCapacity(setDesiredCapacityRequest);
      logger.info(
          String.format(
              "Total message count:%d, Scaling ratio:%.2f, Scaled capacity:%d, Desired capacity:%d",
              totalMessages, scalingRatio, scaledCapacity, desiredCapacity));
      return desiredCapacity;
    } catch (ScalingActivityInProgressException | ResourceContentionException e) {
      throw new ServiceException(Code.ABORTED, "INVALID_STATE", e);
    }
  }

  /** Returns the provided desired capacity, bounded by the configured ASG range */
  private Integer validateCapacity(Integer desiredCapacity) {
    DescribeAutoScalingGroupsRequest describeASGRequest =
        DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(asgName).build();
    DescribeAutoScalingGroupsResponse describeASGResponse =
        autoScalingClient.describeAutoScalingGroups(describeASGRequest);

    AutoScalingGroup workerAsg = describeASGResponse.autoScalingGroups().get(0);
    if (desiredCapacity < workerAsg.minSize()) {
      return workerAsg.minSize();
    } else if (desiredCapacity > workerAsg.maxSize()) {
      return workerAsg.maxSize();
    }
    return desiredCapacity;
  }
}
