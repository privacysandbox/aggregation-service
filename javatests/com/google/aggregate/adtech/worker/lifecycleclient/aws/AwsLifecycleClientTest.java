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

import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleClient;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorARoleArn;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorBRoleArn;
import com.google.scp.operator.cpio.configclient.local.Annotations.CoordinatorKmsArnParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.DdbJobMetadataTableNameParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.MaxJobNumAttemptsParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.MaxJobProcessingTimeSecondsParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.ScaleInHookParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.SqsJobQueueUrlParameter;
import com.google.scp.operator.cpio.configclient.local.Annotations.WorkerAutoscalingGroup;
import com.google.scp.operator.cpio.configclient.local.LocalOperatorParameterModule;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatEnabled;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatFrequency;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.LifecycleActionHeartbeatTimeout;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.MaxLifecycleActionTimeoutExtension;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.WorkerAutoscalingGroupName;
import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleModule.WorkerScaleInHookName;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb.AsgInstancesDbDynamoTableName;
import com.google.aggregate.adtech.worker.shared.testing.FakeClock;
import com.google.scp.shared.proto.ProtoUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingInstanceDetails;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AwsLifecycleClientTest {

  @Bind(lazy = true)
  private @ScaleInHookParameter String scaleInHookParameter;

  @Bind(lazy = true)
  @Mock
  private AutoScalingClient autoScalingClient;

  @Bind(lazy = true)
  @Mock
  private DynamoAsgInstancesDb dynamoAsgInstancesDb;

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Inject private AwsLifecycleClient lifecycleClient;

  public void testSetUp(Boolean useAsgInstancesDb) throws Exception {
    Guice.createInjector(
            BoundFieldModule.of(this),
            new LocalOperatorParameterModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(LifecycleClient.class).to(AwsLifecycleClient.class);
                bind(String.class).annotatedWith(SqsJobQueueUrlParameter.class).toInstance("dummy");
                bind(String.class)
                    .annotatedWith(DdbJobMetadataTableNameParameter.class)
                    .toInstance("DdbJobMetadataTable");
                bind(String.class).annotatedWith(MaxJobNumAttemptsParameter.class).toInstance("1");
                bind(String.class)
                    .annotatedWith(MaxJobProcessingTimeSecondsParameter.class)
                    .toInstance("60");
                bind(String.class)
                    .annotatedWith(CoordinatorARoleArn.class)
                    .toInstance("test-assume-role-arn-a");
                bind(String.class)
                    .annotatedWith(CoordinatorBRoleArn.class)
                    .toInstance("test-assume-role-arn-b");
                bind(String.class)
                    .annotatedWith(CoordinatorKmsArnParameter.class)
                    .toInstance("test-kms-arn");
                bind(String.class)
                    .annotatedWith(AsgInstancesDbDynamoTableName.class)
                    .toInstance(useAsgInstancesDb ? "test" : "");
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(String.class, WorkerAutoscalingGroup.class))
                    .setBinding()
                    .toInstance("replacedGroup");
                bind(Clock.class).to(FakeClock.class);
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(String.class, WorkerAutoscalingGroupName.class))
                    .setBinding()
                    .toInstance("replacedGroup");
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(String.class, WorkerScaleInHookName.class))
                    .setBinding()
                    .toInstance("fakeHook");
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(Boolean.class, LifecycleActionHeartbeatEnabled.class))
                    .setBinding()
                    .toInstance(false);
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(Integer.class, LifecycleActionHeartbeatTimeout.class))
                    .setBinding()
                    .toInstance(300);
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(Integer.class, MaxLifecycleActionTimeoutExtension.class))
                    .setBinding()
                    .toInstance(600);
                OptionalBinder.newOptionalBinder(
                        binder(), Key.get(Integer.class, LifecycleActionHeartbeatFrequency.class))
                    .setBinding()
                    .toInstance(150);
              }
            })
        .injectMembers(this);
  }

  @Test
  public void handleScaleInLifecycleAction_Successful() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ false);
    when(autoScalingClient.describeAutoScalingInstances(
            any(DescribeAutoScalingInstancesRequest.class)))
        .thenReturn(
            DescribeAutoScalingInstancesResponse.builder()
                .autoScalingInstances(
                    AutoScalingInstanceDetails.builder()
                        .autoScalingGroupName("auto-scaling-group-name")
                        .lifecycleState(LifecycleState.TERMINATING_WAIT.toString())
                        .instanceId("i-32874928359")
                        .build())
                .build());

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isTrue();
  }

  @Test
  public void handleScaleInLifecycleAction_NotSuccessful() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ false);
    when(autoScalingClient.describeAutoScalingInstances(
            any(DescribeAutoScalingInstancesRequest.class)))
        .thenReturn(
            DescribeAutoScalingInstancesResponse.builder()
                .autoScalingInstances(
                    AutoScalingInstanceDetails.builder()
                        .autoScalingGroupName("auto-scaling-group-name")
                        .lifecycleState(LifecycleState.IN_SERVICE.toString())
                        .instanceId("i-32874928359")
                        .build())
                .build());

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isFalse();
  }

  @Test
  public void handleScaleInLifecycleAction_HookNotSet() throws Exception {
    scaleInHookParameter = "";
    testSetUp(/* useAsgInstancesDb= */ false);
    when(autoScalingClient.describeAutoScalingInstances(
            any(DescribeAutoScalingInstancesRequest.class)))
        .thenReturn(
            DescribeAutoScalingInstancesResponse.builder()
                .autoScalingInstances(
                    AutoScalingInstanceDetails.builder()
                        .autoScalingGroupName("auto-scaling-group-name")
                        .lifecycleState(LifecycleState.TERMINATING_WAIT.toString())
                        .instanceId("i-32874928359")
                        .build())
                .build());

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isFalse();
  }

  @Test
  public void handleScaleInLifecycleAction_NoAutoscalingInstances() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ false);

    DescribeAutoScalingInstancesResponse response =
        DescribeAutoScalingInstancesResponse.builder()
            .autoScalingInstances(new AutoScalingInstanceDetails[] {}) // Empty Array
            .build();
    when(autoScalingClient.describeAutoScalingInstances(
            any(DescribeAutoScalingInstancesRequest.class)))
        .thenReturn(response);

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isFalse();
    verify(autoScalingClient, times(1))
        .describeAutoScalingInstances(any(DescribeAutoScalingInstancesRequest.class));
  }

  @Test
  public void handleScaleInLifecycleAction_successfulWithDb() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ true);

    AsgInstance asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .build();
    when(dynamoAsgInstancesDb.getAsgInstance(any())).thenReturn(Optional.of(asgInstance));

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isTrue();
    verify(dynamoAsgInstancesDb, times(1)).updateAsgInstance(any(AsgInstance.class));
  }

  @Test
  public void handleScaleInLifecycleAction_nonterminatingInstanceWithDb() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ true);

    when(dynamoAsgInstancesDb.getAsgInstance(any())).thenReturn(Optional.empty());
    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isFalse();
  }

  @Test
  public void handleScaleInLifecycleAction_alreadyTerminatedWithDb() throws Exception {
    scaleInHookParameter = "scale-in-hook-name";
    testSetUp(/* useAsgInstancesDb= */ true);

    AsgInstance asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATED)
            .setRequestTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .build();
    when(dynamoAsgInstancesDb.getAsgInstance(any())).thenReturn(Optional.of(asgInstance));

    assertThat(lifecycleClient.handleScaleInLifecycleAction()).isTrue();
    verify(dynamoAsgInstancesDb, times(0)).updateAsgInstance(any(AsgInstance.class));
  }
}
