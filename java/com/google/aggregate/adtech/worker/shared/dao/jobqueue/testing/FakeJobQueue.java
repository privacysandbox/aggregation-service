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

import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import java.time.Duration;
import java.util.Optional;

/** Fake implementation of {@link JobQueue} for use in tests. */
public final class FakeJobQueue implements JobQueue {

  // Objects that have been passed to the job queue
  private JobKey lastJobKeySent;
  private String lastServerJobIdSent;
  private JobQueueItem lastJobQueueItemSent;

  // Item to be returned by receiveJob
  private Optional<JobQueueItem> jobQueueItemToBeReceived;

  // Flag to throw an exception on method calls
  private boolean shouldThrowException;

  /** Creates a new instance of the {@code FakeJobQueue} class. */
  public FakeJobQueue() {
    lastJobKeySent = null;
    lastServerJobIdSent = null;
    lastJobQueueItemSent = null;
    jobQueueItemToBeReceived = Optional.empty();
    shouldThrowException = false;
  }

  @Override
  public void sendJob(JobKey jobKey, String serverJobId) throws JobQueueException {
    if (shouldThrowException) {
      throw new JobQueueException(new IllegalStateException("was set to throw"));
    }

    lastJobKeySent = jobKey;
    lastServerJobIdSent = serverJobId;
  }

  @Override
  public Optional<JobQueueItem> receiveJob() throws JobQueueException {
    if (shouldThrowException) {
      throw new JobQueueException(new IllegalStateException("was set to throw"));
    }

    return jobQueueItemToBeReceived;
  }

  @Override
  public void acknowledgeJobCompletion(JobQueueItem jobQueueItem) throws JobQueueException {
    if (shouldThrowException) {
      throw new JobQueueException(new IllegalStateException("was set to throw"));
    }

    lastJobQueueItemSent = jobQueueItem;
    jobQueueItemToBeReceived = Optional.empty();
  }

  @Override
  public void modifyJobProcessingTime(JobQueueItem jobQueueItem, Duration processingTime)
      throws JobQueueException {
    if (shouldThrowException) {
      throw new JobQueueException(new IllegalStateException("was set to throw"));
    }
  }

  /** Get the job key used in the last call to the {@code sendJob} method. */
  public JobKey getLastJobKeySent() {
    return lastJobKeySent;
  }

  /** Get the server job id used in the last call to the {@code sendJob} method. */
  public String getLastServerJobIdSent() {
    return lastServerJobIdSent;
  }

  /**
   * Get the job queue item used in the last call to the {@code acknowledgeJobCompletion} method.
   */
  public JobQueueItem getLastJobQueueItemSent() {
    return lastJobQueueItemSent;
  }

  /** Set the item to be returned from the {@code receiveJob} method. */
  public void setJobQueueItemToBeReceived(Optional<JobQueueItem> jobQueueItemToBeReceived) {
    this.jobQueueItemToBeReceived = jobQueueItemToBeReceived;
  }

  /**
   * Sets whether an exception should be thrown by the {@code sendJob}, {@code receiveJob}, and
   * {@code acknowledgeJobCompletion} methods.
   */
  public void setShouldThrowException(boolean shouldThrowException) {
    this.shouldThrowException = shouldThrowException;
  }
}
