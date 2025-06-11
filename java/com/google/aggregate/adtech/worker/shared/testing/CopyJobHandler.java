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

import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.aws.DynamoStreamsJobMetadataHandler;
import com.google.aggregate.adtech.worker.frontend.service.aws.changehandler.JobMetadataChangeHandler;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.DynamoMetadataDb;

import java.util.logging.Logger;

/**
 * Used for testing the {@link DynamoStreamsJobMetadataHandler} by inserting {@code JobMetadata}
 * into another table.
 */
public final class CopyJobHandler implements JobMetadataChangeHandler {

  private final DynamoMetadataDb dynamoMetadataDb;

  /** Creates a new instance of the {@code CopyJobHandler} class. */
  @Inject
  CopyJobHandler(@Copy DynamoMetadataDb dynamoMetadataDb) {
    this.dynamoMetadataDb = dynamoMetadataDb;
  }

  @Override
  public boolean canHandle(JobMetadata data) {
    return data.getJobStatus() == JobStatus.IN_PROGRESS
        || data.getJobStatus() == JobStatus.RECEIVED;
  }

  @Override
  public void handle(JobMetadata data) {
    try {
      Logger.getLogger(CopyJobHandler.class.getName()).info("Inserting job " + data);
      dynamoMetadataDb.insertJobMetadata(data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
