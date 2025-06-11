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

package com.google.aggregate.adtech.worker.shared.testing;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.injection.modules.BaseAwsChangeHandlerModule;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.adtech.worker.frontend.testing.FakeJobMetadataChangeHandler;

/** Dependencies used for unit testing. */
@AutoService(BaseAwsChangeHandlerModule.class)
public final class TestBaseAwsChangeHandlerModule extends BaseAwsChangeHandlerModule {

  /** A list of changes handlers for job metadata. */
  public static ImmutableList<FakeJobMetadataChangeHandler> jobMetadataChangeHandlers =
      ImmutableList.of(
          // The first FakeJobMetadataChangeHandler will not handle any events
          new FakeJobMetadataChangeHandler(/*canHandle=*/ false),
          // The second FakeJobMetadataChangeHandler will handle events
          new FakeJobMetadataChangeHandler(/*canHandle=*/ true),
          // The third FakeJobMetadataChangeHandler will handle events
          new FakeJobMetadataChangeHandler(/*canHandle=*/ true));

  @Override
  public ImmutableList<Class<? extends JobMetadataChangeHandler>> getChangeHandlerImpls() {
    return ImmutableList.of();
  }

  @Override
  protected void configure() {
    // Bind all classes needed for data related services
    Multibinder<JobMetadataChangeHandler> multibinder =
        Multibinder.newSetBinder(binder(), JobMetadataChangeHandler.class);
    jobMetadataChangeHandlers.forEach(handler -> multibinder.addBinding().toInstance(handler));
  }
}
