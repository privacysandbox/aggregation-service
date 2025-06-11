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

package com.google.aggregate.adtech.worker.lifecycleclient.gcp;

import com.google.protobuf.Timestamp;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient.LifecycleClientException;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.GcpInstanceGroupClient;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.GcpLifecycleClient;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.FakeAsgInstancesDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus.TERMINATING_WAIT;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Unit tests for {@code GcpLifecycleClient}. */
@RunWith(JUnit4.class)
public final class GcpLifecycleClientTest {

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private GcpInstanceGroupClient instanceGroupClient;

  private FakeAsgInstancesDao fakeAsgInstancesDao;

  private String instanceUrl;
  private GcpLifecycleClient lifecycleClient;

  @Before
  public void setup() {
    instanceUrl = "worker-" + UUID.randomUUID();
    fakeAsgInstancesDao = new FakeAsgInstancesDao();
  }

  @Test
  public void getLifecycleState_exists() throws Exception {
    AsgInstance asgInstance =
        AsgInstance.newBuilder()
            .setRequestTime(Timestamp.newBuilder().setSeconds(0).build())
            .setInstanceName(instanceUrl)
            .setStatus(TERMINATING_WAIT)
            .build();
    fakeAsgInstancesDao.setAsgInstanceToReturn(Optional.of(asgInstance));
    lifecycleClient = new GcpLifecycleClient(fakeAsgInstancesDao, instanceGroupClient, instanceUrl);

    Optional<String> instanceLifecycleState = lifecycleClient.getLifecycleState();
    assertThat(instanceLifecycleState).isPresent();
    assertThat(instanceLifecycleState).hasValue(TERMINATING_WAIT.name());
  }

  @Test
  public void getLifecycleState_doesNotExist() throws Exception {
    lifecycleClient = new GcpLifecycleClient(fakeAsgInstancesDao, instanceGroupClient, instanceUrl);

    Optional<String> instanceLifecycleState = lifecycleClient.getLifecycleState();
    assertThat(instanceLifecycleState).isEmpty();
  }

  @Test
  public void getLifecycleState_failToQuery() {
    fakeAsgInstancesDao.setShouldThrowAsgInstancesDaoException(true);
    lifecycleClient = new GcpLifecycleClient(fakeAsgInstancesDao, instanceGroupClient, instanceUrl);

    assertThrows(LifecycleClientException.class, () -> lifecycleClient.getLifecycleState());
  }

  @Test
  public void handleScaleInLifecycleAction_terminatingWait() throws Exception {
    AsgInstance asgInstance =
        AsgInstance.newBuilder()
            .setRequestTime(Timestamp.newBuilder().setSeconds(0).build())
            .setInstanceName(instanceUrl)
            .setStatus(TERMINATING_WAIT)
            .build();
    fakeAsgInstancesDao.setAsgInstanceToReturn(Optional.of(asgInstance));
    doNothing().when(instanceGroupClient).deleteInstance();
    lifecycleClient = new GcpLifecycleClient(fakeAsgInstancesDao, instanceGroupClient, instanceUrl);

    boolean handledScaleIn = lifecycleClient.handleScaleInLifecycleAction();
    verify(instanceGroupClient, times(1)).deleteInstance();
    assertThat(handledScaleIn).isTrue();
  }

  @Test
  public void handleScaleInLifecycleAction_notTerminationCandidate() throws Exception {
    lifecycleClient = new GcpLifecycleClient(fakeAsgInstancesDao, instanceGroupClient, instanceUrl);

    boolean handledScaleIn = lifecycleClient.handleScaleInLifecycleAction();
    verify(instanceGroupClient, times(0)).deleteInstance();
    assertThat(handledScaleIn).isFalse();
  }
}
