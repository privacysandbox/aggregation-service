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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.testing;

import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import java.util.Optional;

/** Fake implementation of the {@link JobMetadataDb} for use in tests. */
public final class FakeMetadataDb implements JobMetadataDb {

  // Values to return
  private Optional<JobMetadata> jobMetadataToReturn;

  // Last values requested with
  private String lastJobKeyStringLookedUp;
  private JobMetadata lastJobMetadataInserted;
  private JobMetadata lastJobMetadataUpdated;

  // Flags to throw exceptions
  private boolean shouldThrowJobMetadataDbException;
  private boolean shouldThrowJobKeyExistsException;
  private boolean shouldThrowJobMetadataConflictException;

  // Counts how many times a lookup has occurred for the same value consecutively
  private int jobMetadataLookupCount;

  // Number of times a GetJobMetadata call will fail before succeeding.
  private int initialLookupFailureCount = 0;

  /** Creates a new instance of the {@code FakeMetadataDb} class. */
  public FakeMetadataDb() {
    reset();
  }

  @Override
  public Optional<JobMetadata> getJobMetadata(String jobKeyString) throws JobMetadataDbException {
    if (shouldThrowJobMetadataDbException) {
      throw new JobMetadataDbException(
          new IllegalStateException("Was set to throw (shouldThrowJobMetadataDbException)"));
    }
    if (initialLookupFailureCount > 0) {
      jobMetadataLookupCount++;
      if (lastJobKeyStringLookedUp != null && lastJobKeyStringLookedUp.equals(jobKeyString)) {
        if (jobMetadataLookupCount > initialLookupFailureCount) {
          return jobMetadataToReturn;
        }
      } else {
        jobMetadataLookupCount = 1;
      }
      lastJobKeyStringLookedUp = jobKeyString;
      return Optional.empty();
    } else {
      lastJobKeyStringLookedUp = jobKeyString;
      return jobMetadataToReturn;
    }
  }

  @Override
  public void insertJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobKeyExistsException {
    if (shouldThrowJobMetadataDbException) {
      throw new JobMetadataDbException(
          new IllegalStateException("Was set to throw (shouldThrowJobMetadataDbException)"));
    }

    if (shouldThrowJobKeyExistsException) {
      throw new JobKeyExistsException(
          new IllegalStateException("Was set to throw (shouldThrowJobKeyExistsException)"));
    }

    lastJobMetadataInserted = jobMetadata;
  }

  @Override
  public void updateJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobMetadataConflictException {
    if (shouldThrowJobMetadataDbException) {
      throw new JobMetadataDbException(
          new IllegalStateException("Was set to throw (shouldThrowJobMetadataDbException)"));
    }

    if (shouldThrowJobMetadataConflictException) {
      throw new JobMetadataConflictException(
          new IllegalStateException(
              "Was set to throw (shouldThrowJobMetadataDoesNotExistException)"));
    }

    lastJobMetadataUpdated = jobMetadata;
  }

  /** Set the job metadata to be returned from the {@code getJobMetadata} method. */
  public void setJobMetadataToReturn(Optional<JobMetadata> jobMetadataToReturn) {
    this.jobMetadataToReturn = jobMetadataToReturn;
  }

  /** Set the number of job metadata lookups that have failed. */
  public void setInitialLookupFailureCount(int initialLookupFailureCount) {
    this.initialLookupFailureCount = initialLookupFailureCount;
  }

  /** Get the most recent job metadata that was inserted. */
  public JobMetadata getLastJobMetadataInserted() {
    return lastJobMetadataInserted;
  }

  /** Get the most recent job metadata that was updated. */
  public JobMetadata getLastJobMetadataUpdated() {
    return lastJobMetadataUpdated;
  }

  /**
   * Set if the {@code getJobMetadata}, {@code insertJobMetadata}, and {@code updateJobMetadata}
   * methods should throw the {@code jobMetadataDbException}.
   */
  public void setShouldThrowJobMetadataDbException(boolean shouldThrowJobMetadataDbException) {
    this.shouldThrowJobMetadataDbException = shouldThrowJobMetadataDbException;
  }

  /** Set if the {@code insertJobMetadata} method should throw the {@code JobKeyExistsException}. */
  public void setShouldThrowJobKeyExistsException(boolean shouldThrowJobKeyExistsException) {
    this.shouldThrowJobKeyExistsException = shouldThrowJobKeyExistsException;
  }

  /**
   * Set if the {@code updateJobMetadata} method should throw the {@code
   * JobMetadataConflictException}.
   */
  public void setShouldThrowJobMetadataConflictException(
      boolean shouldThrowJobMetadataConflictException) {
    this.shouldThrowJobMetadataConflictException = shouldThrowJobMetadataConflictException;
  }

  /** Get the last job key used to try and retrieve job metadata. */
  public String getLastJobKeyStringLookedUp() {
    return lastJobKeyStringLookedUp;
  }

  /** Sets all internal fields to their default values. */
  public void reset() {
    jobMetadataToReturn = Optional.empty();
    lastJobKeyStringLookedUp = null;
    lastJobMetadataInserted = null;
    lastJobMetadataUpdated = null;
    shouldThrowJobMetadataDbException = false;
    shouldThrowJobKeyExistsException = false;
    shouldThrowJobMetadataConflictException = false;
    jobMetadataLookupCount = 0;
    initialLookupFailureCount = 0;
  }
}
