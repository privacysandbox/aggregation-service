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

package com.google.aggregate.adtech.worker.shared.injection.modules.testing;

import com.google.auto.service.AutoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing.FakeMetadataDb;
import com.google.aggregate.adtech.worker.shared.injection.modules.BaseDataModule;
import com.google.aggregate.adtech.worker.shared.testing.FakeClock;
import java.time.Clock;

/** Provides a fake implementation for the {@code BaseDataModule} class. */
@AutoService(BaseDataModule.class)
public final class FakeDataModule extends BaseDataModule {

  @Override
  public String getJobMetadataTableName() {
    return "JOB_METADATA_TABLE";
  }

  @Override
  public int getJobMetadataTtl() {
    return 90;
  }

  @Override
  @Provides
  @Singleton
  public Clock provideClock() {
    return new FakeClock();
  }

  @Override
  public Class<? extends JobMetadataDb> getJobMetadataDbImplementation() {
    return FakeMetadataDb.class;
  }
}
