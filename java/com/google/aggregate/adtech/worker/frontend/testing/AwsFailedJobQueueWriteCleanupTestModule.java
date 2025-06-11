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

package com.google.aggregate.adtech.worker.frontend.testing;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.aggregate.adtech.worker.frontend.injection.modules.BaseAwsChangeHandlerModule;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.MarkJobFailedToEnqueueHandler;

/** Module for the lambda tested in AwsFailedJobQueueWriteCleanupTest. */
@AutoService(BaseAwsChangeHandlerModule.class)
public final class AwsFailedJobQueueWriteCleanupTestModule extends BaseAwsChangeHandlerModule {

  @Override
  public ImmutableList<Class<? extends JobMetadataChangeHandler>> getChangeHandlerImpls() {
    return ImmutableList.of(MarkJobFailedToEnqueueHandler.class);
  }
}
