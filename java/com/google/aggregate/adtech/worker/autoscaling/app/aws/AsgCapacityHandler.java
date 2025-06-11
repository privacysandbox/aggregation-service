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

package com.google.aggregate.adtech.worker.autoscaling.app.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.GetMessageCountTask;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.SetDesiredCapacityTask;
import com.google.scp.shared.api.exception.ServiceException;

/**
 * Lambda handler to set the desired capacity for worker Auto Scaling Group. The Lambda function
 * will be triggered by a Cloudwatch cron that runs periodically.
 */
public final class AsgCapacityHandler implements RequestHandler<ScheduledEvent, String> {

  private final GetMessageCountTask getMessageCountTask;
  private final SetDesiredCapacityTask setDesiredCapacityTask;

  public AsgCapacityHandler() {
    Injector injector = Guice.createInjector(new AsgCapacityModule());
    this.getMessageCountTask = injector.getInstance(GetMessageCountTask.class);
    this.setDesiredCapacityTask = injector.getInstance(SetDesiredCapacityTask.class);
  }

  /** Constructor for testing. */
  public AsgCapacityHandler(
      GetMessageCountTask getMessageCountTask, SetDesiredCapacityTask setDesiredCapacityTask) {
    this.getMessageCountTask = getMessageCountTask;
    this.setDesiredCapacityTask = setDesiredCapacityTask;
  }

  /**
   * Checks the total messages in the SQS job queue and sets desired capacity after applying the
   * scaling ratio (worker instances : jobs).
   */
  public String handleRequest(ScheduledEvent event, Context context) {
    try {
      Integer totalMessages = getMessageCountTask.getTotalMessages();
      Integer desiredCapacity = setDesiredCapacityTask.setAsgDesiredCapacity(totalMessages);
      return "Success! Desired capacity:" + desiredCapacity;
    } catch (ServiceException e) {
      throw new IllegalStateException(e);
    }
  }
}
