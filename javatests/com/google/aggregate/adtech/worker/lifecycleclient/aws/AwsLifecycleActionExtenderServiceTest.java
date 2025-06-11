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

import com.google.aggregate.adtech.worker.lifecycleclient.aws.AwsLifecycleActionExtenderService;
import com.google.inject.Provider;
import com.google.protobuf.Timestamp;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.DynamoAsgInstancesDb;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import com.google.aggregate.adtech.worker.shared.testing.FakeClock;
import com.google.scp.shared.clients.configclient.ParameterClient;
import com.google.scp.shared.clients.configclient.ParameterClient.ParameterClientException;
import com.google.scp.shared.proto.ProtoUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;
import software.amazon.awssdk.services.autoscaling.model.RecordLifecycleActionHeartbeatResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for the AwsLifecycleActionExtenderService. */
@RunWith(JUnit4.class)
public final class AwsLifecycleActionExtenderServiceTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock AutoScalingClient autoScalingClient;
  @Mock DynamoAsgInstancesDb dynamoAsgInstancesDb;
  @Mock ParameterClient parameterClient;
  @Mock Provider<Optional<Integer>> lifecycleActionHeartbeatTimeout;
  @Mock Provider<Optional<Integer>> maxLifecycleActionTimeoutExtension;
  @Mock Provider<Optional<Integer>> lifecycleActionHeartbeatFrequency;

  @Mock Provider<Boolean> lifecycleActionHeartbeatEnabled;

  FakeClock fakeClock = new FakeClock();

  AsgInstance asgInstance;

  private final Optional<String> fakeGroup = Optional.of("fakeGroup");
  private final Optional<String> fakeHook = Optional.of("fakeHook");

  @Before
  public void setUp() {
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(ProtoUtil.toProtoTimestamp(Instant.now(fakeClock)))
            .setLastHeartbeatTime(ProtoUtil.toProtoTimestamp(Instant.now(fakeClock)))
            .build();
  }

  @Test
  public void shouldHeartbeat_true() {
    Timestamp startTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(10, ChronoUnit.MINUTES));
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(startTime)
            .setLastHeartbeatTime(startTime)
            .build();

    AwsLifecycleActionExtenderService extenderService = constructExtenderService();

    boolean shouldHeartbeat = extenderService.shouldHeartbeat(asgInstance, 12345678, 600);
    assertThat(shouldHeartbeat).isTrue();
  }

  @Test
  public void shouldHeartbeat_currentlyOverMaxExtension() throws ParameterClientException {
    Timestamp startTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(1, ChronoUnit.HOURS));
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(startTime)
            .setLastHeartbeatTime(startTime)
            .build();
    AwsLifecycleActionExtenderService extenderService = constructExtenderService();
    boolean shouldHeartbeat = extenderService.shouldHeartbeat(asgInstance, 600, 300);
    assertThat(shouldHeartbeat).isFalse();
  }

  @Test
  public void shouldHeartbeat_heartbeatedOverMaxExtension() {
    Timestamp startTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(55, ChronoUnit.MINUTES));
    Timestamp lastHeartbeatTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(9, ChronoUnit.MINUTES));
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(startTime)
            .setLastHeartbeatTime(lastHeartbeatTime)
            .build();

    AwsLifecycleActionExtenderService extenderService = constructExtenderService();
    boolean shouldHeartbeat = extenderService.shouldHeartbeat(asgInstance, 3600, 900);

    assertThat(shouldHeartbeat).isFalse();
  }

  @Test
  public void sendHeartbeat_success() throws AsgInstanceDaoException {
    Timestamp startTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(10, ChronoUnit.MINUTES));
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(startTime)
            .setLastHeartbeatTime(startTime)
            .build();
    RecordLifecycleActionHeartbeatResponse response =
        RecordLifecycleActionHeartbeatResponse.builder().build();
    when(autoScalingClient.recordLifecycleActionHeartbeat(
            any(RecordLifecycleActionHeartbeatRequest.class)))
        .thenReturn(response);
    doNothing().when(dynamoAsgInstancesDb).updateAsgInstance(any());

    AwsLifecycleActionExtenderService extenderService = constructExtenderService();
    extenderService.sendHeartbeat(asgInstance, "fakeGroup", "fakeHook");

    verify(autoScalingClient, times(1))
        .recordLifecycleActionHeartbeat(any(RecordLifecycleActionHeartbeatRequest.class));
    verify(dynamoAsgInstancesDb, times(1)).updateAsgInstance(any());
  }

  @Test(expected = AsgInstanceDaoException.class)
  public void sendHeartbeat_asgInstanceDaoException() throws AsgInstanceDaoException {
    Timestamp startTime =
        ProtoUtil.toProtoTimestamp(Instant.now(fakeClock).minus(10, ChronoUnit.MINUTES));
    asgInstance =
        AsgInstance.newBuilder()
            .setInstanceName("123")
            .setStatus(InstanceStatus.TERMINATING_WAIT)
            .setRequestTime(startTime)
            .setLastHeartbeatTime(startTime)
            .build();
    RecordLifecycleActionHeartbeatResponse response =
        RecordLifecycleActionHeartbeatResponse.builder().build();
    when(autoScalingClient.recordLifecycleActionHeartbeat(
            any(RecordLifecycleActionHeartbeatRequest.class)))
        .thenReturn(response);
    doThrow(new AsgInstanceDaoException(new RuntimeException()))
        .when(dynamoAsgInstancesDb)
        .updateAsgInstance(any());

    AwsLifecycleActionExtenderService extenderService = constructExtenderService();
    extenderService.sendHeartbeat(asgInstance, "fakeGroup", "fakeHook");

    verify(autoScalingClient, times(1))
        .recordLifecycleActionHeartbeat(any(RecordLifecycleActionHeartbeatRequest.class));
    verify(dynamoAsgInstancesDb, times(1)).updateAsgInstance(any());
  }

  private AwsLifecycleActionExtenderService constructExtenderService() {
    when(lifecycleActionHeartbeatEnabled.get()).thenReturn(true);
    return new AwsLifecycleActionExtenderService(
        autoScalingClient,
        dynamoAsgInstancesDb,
        fakeClock,
        fakeHook,
        fakeGroup,
        lifecycleActionHeartbeatEnabled,
        lifecycleActionHeartbeatTimeout,
        maxLifecycleActionTimeoutExtension,
        lifecycleActionHeartbeatFrequency);
  }
}
