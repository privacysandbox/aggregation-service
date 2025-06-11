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

import com.google.scp.shared.api.exception.ServiceException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.ScalingActivityInProgressException;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public final class SetDesiredCapacityTaskTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock AutoScalingClient autoScalingClientMock;
  private static final String asgName = "test-asg";

  @Test
  public void setAsgDesiredCapacity_lessThanMin() throws ServiceException {
    setupAutoscalingGroup(3, 10);

    var setDesiredCapacityTask = new SetDesiredCapacityTask(autoScalingClientMock, asgName, 1.0);
    Integer desiredCapacity = setDesiredCapacityTask.setAsgDesiredCapacity(1);

    Assert.assertEquals((Integer) 3, desiredCapacity);
  }

  @Test
  public void setAsgDesiredCapacity_greaterThanMax() throws ServiceException {
    setupAutoscalingGroup(3, 10);

    var setDesiredCapacityTask = new SetDesiredCapacityTask(autoScalingClientMock, asgName, 1.0);
    Integer desiredCapacity = setDesiredCapacityTask.setAsgDesiredCapacity(20);

    Assert.assertEquals((Integer) 10, desiredCapacity);
  }

  @Test
  public void setAsgDesiredCapacity_scalingRatioLessThanMin() throws ServiceException {
    setupAutoscalingGroup(5, 7);

    var setDesiredCapacityTask = new SetDesiredCapacityTask(autoScalingClientMock, asgName, 0.5);
    Integer desiredCapacity = setDesiredCapacityTask.setAsgDesiredCapacity(7);

    Assert.assertEquals((Integer) 5, desiredCapacity);
  }

  @Test
  public void setAsgDesiredCapacity_scalingRatioRoundUp() throws ServiceException {
    setupAutoscalingGroup(1, 7);

    var setDesiredCapacityTask = new SetDesiredCapacityTask(autoScalingClientMock, asgName, 0.3);
    Integer desiredCapacity = setDesiredCapacityTask.setAsgDesiredCapacity(7);

    Mockito.verify(autoScalingClientMock).setDesiredCapacity(any(SetDesiredCapacityRequest.class));
    Assert.assertEquals((Integer) 3, desiredCapacity);
  }

  @Test
  public void setAsgDesiredCapacity_scalingActivityInProgress() {
    ScalingActivityInProgressException exception =
        ScalingActivityInProgressException.builder().build();
    when(autoScalingClientMock.describeAutoScalingGroups(
            any(DescribeAutoScalingGroupsRequest.class)))
        .thenThrow(exception);

    var setDesiredCapacityTask = new SetDesiredCapacityTask(autoScalingClientMock, asgName, 0.3);
    Assert.assertThrows(
        ServiceException.class, () -> setDesiredCapacityTask.setAsgDesiredCapacity(1));
  }

  private void setupAutoscalingGroup(Integer minInstances, Integer maxInstances) {
    AutoScalingGroup testAsg =
        AutoScalingGroup.builder().minSize(minInstances).maxSize(maxInstances).build();
    DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(testAsg).build();
    when(autoScalingClientMock.describeAutoScalingGroups(
            any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeAutoScalingGroupsResponse);
  }
}
