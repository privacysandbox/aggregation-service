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

package com.google.aggregate.adtech.worker.shared.dao.jobqueue.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Optional;

/** Interface for accessing the job processing queue. */
public interface JobQueue {

  /**
   * Place an item on the queue.
   *
   * @param jobKey the identifier for the job to be placed the queue
   */
  void sendJob(JobKey jobKey, String serverJobId) throws JobQueueException;

  /**
   * Blocking call to receive a message.
   *
   * @return an {@code Optional} of a {@code JobQueueItem}. The {@code JobQueueItem} will be present
   *     if a message was received within the receipt timeout. It will be empty if no message was
   *     received.
   */
  Optional<JobQueueItem> receiveJob() throws JobQueueException;

  /**
   * Acknowledge that a job was successfully processed so that it can be deleted from the queue.
   *
   * @param jobQueueItem the item representing a job that was completed, containing receipt
   *     information for the queue.
   */
  void acknowledgeJobCompletion(JobQueueItem jobQueueItem) throws JobQueueException;

  /**
   * Modifies the job processing timeout for the queue item.
   *
   * @param jobQueueItem the item to extend the processing time for
   * @param processingTime the duration after current in which the queue item will be returned to
   *     the queue
   * @throws JobQueueException
   */
  void modifyJobProcessingTime(JobQueueItem jobQueueItem, Duration processingTime)
      throws JobQueueException;

  /** Represents an exception thrown by the {@code JobQueue} class. */
  class JobQueueException extends Exception {
    /** Creates a new instance of the {@code JobQueueException} class from a throwable cause. */
    public JobQueueException(Throwable cause) {
      super(cause);
    }

    /** Creates a new instance of the {@code JobQueueException} class with error message. */
    public JobQueueException(String message) {
      super(message);
    }
  }

  /** The value for the message lease timeout. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface JobQueueMessageLeaseSeconds {}
}
