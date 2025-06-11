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

package com.google.aggregate.adtech.worker.frontend.injection.modules;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobQueueWriteHandler;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsMaxWaitTimeSeconds;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.aws.SqsJobQueue.JobQueueSqsQueueUrl;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue;
import com.google.aggregate.adtech.worker.shared.dao.jobqueue.common.JobQueue.JobQueueMessageLeaseSeconds;
import com.google.aggregate.adtech.worker.shared.injection.factories.ModuleFactory;
import com.google.aggregate.adtech.worker.shared.injection.modules.BaseAwsClientsModule;

/** Module for the change handler lambda that monitors changes to the DynamoDb Metadata table. */
@AutoService(BaseAwsChangeHandlerModule.class)
public final class AwsChangeHandlerModule extends BaseAwsChangeHandlerModule {

  // JobQueue SQS URL will be provided via environment variable
  private static final String JOB_QUEUE_AWS_SQS_URL_ENV_VAR = "AWS_SQS_URL";

  @Override
  protected void configureModule() {
    // JobQueueWriteHandler dependencies
    bind(JobQueue.class).to(SqsJobQueue.class);

    // JobQueue dependencies
    install(ModuleFactory.getModule(BaseAwsClientsModule.class));
    bind(String.class)
        .annotatedWith(JobQueueSqsQueueUrl.class)
        .toInstance(System.getenv(JOB_QUEUE_AWS_SQS_URL_ENV_VAR));
    // These values aren't actually used. They're only used for SqsJobQueue.receiveJob which isn't
    // called
    bind(Integer.class).annotatedWith(JobQueueSqsMaxWaitTimeSeconds.class).toInstance(5);
    bind(Integer.class).annotatedWith(JobQueueMessageLeaseSeconds.class).toInstance(10);
  }

  @Override
  public ImmutableList<Class<? extends JobMetadataChangeHandler>> getChangeHandlerImpls() {
    return ImmutableList.of(JobQueueWriteHandler.class);
  }
}
