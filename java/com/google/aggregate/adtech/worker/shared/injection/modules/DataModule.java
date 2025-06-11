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

import com.google.auto.service.AutoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.JobRequestIdCharactersValidator;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.JobRequestIdLengthValidator;
import com.google.aggregate.adtech.worker.frontend.tasks.validation.RequestInfoValidator;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import java.time.Clock;

/** Contains all the necessary bindings needed for the data layer. */
@AutoService(BaseDataModule.class)
public final class DataModule extends BaseDataModule {

  @Override
  protected void configureModule() {
    bindValidators();
  }

  @Override
  public String getJobMetadataTableName() {
    return System.getenv(EnvironmentVariables.JOB_METADATA_TABLE_ENV_VAR);
  }

  @Override
  public int getJobMetadataTtl() {
    return Integer.parseInt(System.getenv(EnvironmentVariables.JOB_METADATA_TTL_ENV_VAR));
  }

  @Override
  public Class<? extends JobMetadataDb> getJobMetadataDbImplementation() {
    return DynamoMetadataDb.class;
  }

  @Override
  @Provides
  @Singleton
  public Clock provideClock() {
    return Clock.systemUTC();
  }

  private void bindValidators() {
    Multibinder<RequestInfoValidator> requestInfoValidatorMultibinder =
        Multibinder.newSetBinder(binder(), RequestInfoValidator.class);

    requestInfoValidatorMultibinder.addBinding().to(JobRequestIdCharactersValidator.class);
    requestInfoValidatorMultibinder.addBinding().to(JobRequestIdLengthValidator.class);
  }
}
