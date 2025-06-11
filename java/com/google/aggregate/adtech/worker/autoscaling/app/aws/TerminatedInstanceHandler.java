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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.aggregate.adtech.worker.autoscaling.tasks.aws.ManageTerminatedInstanceTask;

import java.util.Map;

/**
 * Lambda handler to do initial filtering of the terminated instance from the worker Auto Scaling
 * Group. The Lambda function will be triggered by EventBridge from Auto Scaling on instance
 * termination.
 */
public final class TerminatedInstanceHandler
    implements RequestHandler<Map<String, Object>, String> {

  private final ManageTerminatedInstanceTask manageTerminatedInstanceTask;

  public TerminatedInstanceHandler() {
    Injector injector = Guice.createInjector(new TerminatedInstanceModule());
    this.manageTerminatedInstanceTask = injector.getInstance(ManageTerminatedInstanceTask.class);
  }

  /** Constructor for testing. */
  public TerminatedInstanceHandler(ManageTerminatedInstanceTask manageTerminatedInstanceTask) {
    this.manageTerminatedInstanceTask = manageTerminatedInstanceTask;
  }

  /**
   * Checks the health status of terminated instances from the worker Auto Scaling Group and
   * completes the termination lifecycle action if unhealthy. Otherwise, inserts a record into the
   * AsgInstances table and the worker will handle continuing the termination when it is not running
   * a job.
   *
   * <p>Instance termination event format:
   * https://docs.aws.amazon.com/autoscaling/ec2/userguide/cloud-watch-events.html#terminate-lifecycle-action
   */
  public String handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Instance termination event: " + event);
    Map<String, String> detailMap = (Map<String, String>) event.get("detail");
    manageTerminatedInstanceTask.manageTerminatedInstance(
        detailMap.get("AutoScalingGroupName"),
        detailMap.get("EC2InstanceId"),
        detailMap.get("LifecycleHookName"),
        detailMap.get("LifecycleActionToken"));
    return "Success!";
  }
}
