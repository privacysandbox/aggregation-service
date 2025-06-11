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

import com.google.common.collect.ImmutableList;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import java.util.ArrayList;
import java.util.List;

/** The fake implementation of the {@link JobMetadataChangeHandler}. */
public final class FakeJobMetadataChangeHandler implements JobMetadataChangeHandler {

  private final boolean canHandle;
  private List<JobMetadata> jobMetadatasHandled;

  /** Returns a new instance of the {@code FakeJobMetadataChangeHandler} class. */
  public FakeJobMetadataChangeHandler(boolean canHandle) {
    this.canHandle = canHandle;
    reset();
  }

  /** Resets the state of the handler. */
  public void reset() {
    jobMetadatasHandled = new ArrayList<>();
  }

  @Override
  public boolean canHandle(JobMetadata data) {
    return canHandle;
  }

  @Override
  public void handle(JobMetadata data) {
    jobMetadatasHandled.add(data);
  }

  /** Returns a list of jobs that have been handled. */
  public ImmutableList<JobMetadata> getHandled() {
    return ImmutableList.copyOf(jobMetadatasHandled);
  }
}
