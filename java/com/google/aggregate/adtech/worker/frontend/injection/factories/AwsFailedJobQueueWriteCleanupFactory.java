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

package com.google.aggregate.adtech.worker.frontend.injection.factories;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.aggregate.adtech.worker.frontend.injection.modules.BaseAwsChangeHandlerModule;
import com.google.aggregate.adtech.worker.frontend.service.aws.DdbStreamBatchInfoParser;
import com.google.aggregate.adtech.worker.frontend.service.aws.DdbStreamJobMetadataLookup;
import com.google.aggregate.adtech.worker.frontend.service.aws.DynamoStreamsJobMetadataUpdateChecker;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.adtech.worker.shared.injection.factories.ModuleFactory;
import com.google.aggregate.adtech.worker.shared.injection.modules.BaseDataModule;
import java.util.Set;

/** Exposes components for the {@code AwsFailedJobQueueWriteCleanupModule}. */
public final class AwsFailedJobQueueWriteCleanupFactory {

  private static final Injector injector =
      Guice.createInjector(
          ModuleFactory.getModule(BaseDataModule.class),
          ModuleFactory.getModule(BaseAwsChangeHandlerModule.class));

  /** Gets an instance of the {@code DdbStreamBatchInfoParser} class. */
  public static DdbStreamBatchInfoParser getDdbStreamBatchInfoParser() {
    return injector.getInstance(DdbStreamBatchInfoParser.class);
  }

  /** Gets an instance of the {@code DdbStreamJobMetadataLookup} class. */
  public static DdbStreamJobMetadataLookup getDdbStreamJobMetadataLookup() {
    return injector.getInstance(DdbStreamJobMetadataLookup.class);
  }

  /** Gets a {@code Set} of instances of the {@code JobMetadataChangeHandler} class. */
  public static Set<JobMetadataChangeHandler> getJobMetadataChangeHandlers() {
    return injector.getInstance(Key.get(new TypeLiteral<Set<JobMetadataChangeHandler>>() {}));
  }

  /** Gets an instance of the {@code DynamoStreamsJobMetadataUpdateChecker} class. */
  public static DynamoStreamsJobMetadataUpdateChecker getJobMetadataUpdateChecker() {
    return injector.getInstance(DynamoStreamsJobMetadataUpdateChecker.class);
  }
}
