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

package com.google.aggregate.adtech.worker.shared.dao.jobqueue.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.util.Durations;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueException;
import com.google.scp.shared.proto.ProtoUtil;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeJobQueueTest {

  // Under test
  FakeJobQueue fakeJobQueue;

  JobKey jobKey;
  String serverJobId;
  JobQueueItem jobQueueItem;

  @Before
  public void setUp() {
    fakeJobQueue = new FakeJobQueue();

    jobKey = JobKey.newBuilder().setJobRequestId("job-abc-123").build();
    serverJobId = UUID.randomUUID().toString();

    jobQueueItem =
        JobQueueItem.newBuilder()
            .setJobKeyString(jobKey.getJobRequestId())
            .setServerJobId(serverJobId)
            .setJobProcessingTimeout(Durations.fromSeconds(10))
            .setJobProcessingStartTime(ProtoUtil.toProtoTimestamp(Instant.now()))
            .setReceiptInfo("fizzbuzz")
            .build();
  }

  @Test
  public void testSendJob_simple() throws Exception {
    // No setup

    fakeJobQueue.sendJob(jobKey, serverJobId);

    assertThat(fakeJobQueue.getLastJobKeySent()).isEqualTo(jobKey);
  }

  @Test
  public void testSendJob_exception() throws Exception {
    fakeJobQueue.setShouldThrowException(true);

    assertThrows(JobQueueException.class, () -> fakeJobQueue.sendJob(jobKey, serverJobId));
  }

  @Test
  public void testReceiveJob_present() throws Exception {
    fakeJobQueue.setJobQueueItemToBeReceived(Optional.of(jobQueueItem));

    Optional<JobQueueItem> receivedJobQueueItem = fakeJobQueue.receiveJob();

    assertThat(receivedJobQueueItem).isPresent();
    assertThat(receivedJobQueueItem).hasValue(jobQueueItem);
  }

  @Test
  public void testReceiveJob_empty() throws Exception {
    fakeJobQueue.setJobQueueItemToBeReceived(Optional.empty());

    Optional<JobQueueItem> receivedJobQueueItem = fakeJobQueue.receiveJob();

    assertThat(receivedJobQueueItem).isEmpty();
  }

  @Test
  public void testReceiveJob_exception() throws Exception {
    fakeJobQueue.setShouldThrowException(true);

    assertThrows(JobQueueException.class, () -> fakeJobQueue.receiveJob());
  }

  @Test
  public void testAcknowledgeJobCompletion_simple() throws Exception {
    // No setup

    fakeJobQueue.acknowledgeJobCompletion(jobQueueItem);

    assertThat(fakeJobQueue.getLastJobQueueItemSent()).isEqualTo(jobQueueItem);
  }

  @Test
  public void testAcknowledgeJobCompletion_exception() throws Exception {
    fakeJobQueue.setShouldThrowException(true);

    assertThrows(
        JobQueueException.class, () -> fakeJobQueue.acknowledgeJobCompletion(jobQueueItem));
  }
}
