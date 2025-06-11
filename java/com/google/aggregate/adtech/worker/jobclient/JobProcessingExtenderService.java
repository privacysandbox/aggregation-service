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

package com.google.aggregate.adtech.worker.jobclient;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.aggregate.protos.shared.backend.jobqueue.JobQueueProto.JobQueueItem;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueException;
import com.google.scp.shared.proto.ProtoUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class JobProcessingExtenderService extends AbstractExecutionThreadService {

  private static final Logger logger =
      Logger.getLogger(JobProcessingExtenderService.class.getName());

  private final JobQueue jobQueue;
  private boolean shouldRun;
  private ConcurrentHashMap<String, JobQueueItem> jobs;
  private final Duration defaultExtensionDuration = Duration.ofMinutes(5);

  /**
   * Periodically goes through the JobClient job cache and extends the job processing timeout for
   * the message if it has not exceeded the maximum processing time.
   */
  public JobProcessingExtenderService(
      JobQueue jobQueue, ConcurrentHashMap<String, JobQueueItem> jobs) {
    this.jobQueue = jobQueue;
    this.shouldRun = true;
    this.jobs = jobs;
  }

  @Override
  protected void run() {
    while (shouldRun) {
      try {
        for (JobQueueItem jobQueueItem : jobs.values()) {
          try {
            Instant jobProcessingStartTime =
                ProtoUtil.toJavaInstant(jobQueueItem.getJobProcessingStartTime());
            Duration jobProcessingTimeout =
                ProtoUtil.toJavaDuration(jobQueueItem.getJobProcessingTimeout());

            Duration timeUntilMaxTimeout =
                Duration.between(Instant.now(), jobProcessingStartTime.plus(jobProcessingTimeout));
            if (!timeUntilMaxTimeout.isNegative() && !timeUntilMaxTimeout.isZero()) {
              Duration extensionTime =
                  timeUntilMaxTimeout.compareTo(defaultExtensionDuration) < 0
                      ? timeUntilMaxTimeout
                      : defaultExtensionDuration;
              logger.info(
                  "Extending processing time by " + extensionTime + " for job: " + jobQueueItem);
              jobQueue.modifyJobProcessingTime(jobQueueItem, extensionTime);
            }
          } catch (JobQueueException e) {
            logger.info(
                String.format(
                    "An issue occurred while modifying the process timeout for: %s\n%s",
                    jobQueueItem.getJobKeyString(), e));
          }
        }
        logger.info("Sleeping the job processing extender service.");
        Thread.sleep(60 * 1000);
      } catch (Exception e) {
        logger.info(
            String.format(
                "There was an issue running the job processing extender service for jobs: %s\n%s",
                jobs, e));
      }
    }
  }

  @Override
  protected void triggerShutdown() {
    shouldRun = false;
  }
}
