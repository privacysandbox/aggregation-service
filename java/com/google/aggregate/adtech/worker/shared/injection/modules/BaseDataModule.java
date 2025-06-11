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

package com.google.aggregate.adtech.worker.shared.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoTableName;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb.MetadataDbDynamoTtlDays;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.injection.factories.ModuleFactory;
import java.time.Clock;

/** Allows swappable/discoverable DataModules when an inheritor's package is referenced. */
public abstract class BaseDataModule extends AbstractModule {

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  protected void configureModule() {}

  /** Gets the job metadata table name. */
  public abstract String getJobMetadataTableName();

  /** Gets the number of days that a job metadata record is live before expiration. */
  public abstract int getJobMetadataTtl();

  /** Gets the implementation of the {@code Clock} class. */
  public abstract Clock provideClock();

  /** Returns an implementation of the {@code JobMetadataDb} class. */
  public abstract Class<? extends JobMetadataDb> getJobMetadataDbImplementation();

  /** Configures injected dependencies for this module. */
  @Override
  protected void configure() {
    install(ModuleFactory.getModule(BaseAwsClientsModule.class));
    bind(String.class)
        .annotatedWith(MetadataDbDynamoTableName.class)
        .toInstance(getJobMetadataTableName());
    bind(int.class).annotatedWith(MetadataDbDynamoTtlDays.class).toInstance(getJobMetadataTtl());
    bind(getJobMetadataDbImplementation()).in(Singleton.class);
    bind(JobMetadataDb.class).to(getJobMetadataDbImplementation()).in(Singleton.class);
    configureModule();
  }
}
