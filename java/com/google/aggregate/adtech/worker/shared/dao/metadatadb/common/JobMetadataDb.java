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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

/** Interface for accessing job metadata DB. */
public interface JobMetadataDb {

  /**
   * Retrieve metadata for a given job. Optional will be empty if no record exists.
   *
   * @param jobKeyString the string representation of the job key, either obtained from
   *     JobQueueItem.jobKeyString() or from JobKey.toKeyString()
   * @throws JobMetadataDbException for other failures to read
   */
  Optional<JobMetadata> getJobMetadata(String jobKeyString) throws JobMetadataDbException;

  /**
   * Insert a metadata entry for a job, throwing an exception if the job-key is already in use. This
   * method is intended to be used to ensure insertions are safe and prevent conflicting writes.
   *
   * @throws JobKeyExistsException if the JobKey is already in use by an item
   * @throws JobMetadataDbException for other failures to write
   */
  void insertJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobKeyExistsException;

  /**
   * Updates an existing {@code JobMetadata} in the metadata DB. Exceptions are thrown if the
   * metadata item does not exist.
   *
   * @throws JobMetadataConflictException if the item does not exist or if the record version field
   *     is out of date, indicating that the record has been updated in between the time it was read
   *     and the time it was written.
   * @throws JobMetadataDbException for other failures to write
   */
  void updateJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobMetadataConflictException;

  /** Represents an exception thrown by the {@code JobMetadataDb} class. */
  class JobMetadataDbException extends Exception {
    /** Creates a new instance of the {@code JobMetadataDbException} class. */
    public JobMetadataDbException(Throwable cause) {
      super(cause);
    }
  }

  /** Thrown if an insertion is attempted for a JobKey that is already in use. */
  class JobKeyExistsException extends Exception {
    /** Creates a new instance of the {@code JobKeyExistsException} class. */
    public JobKeyExistsException(Throwable cause) {
      super(cause);
    }

    /** Creates a new instance of the {@code JobKeyExistsException} class with error message. */
    public JobKeyExistsException(String message) {
      super(message);
    }
  }

  /**
   * Thrown if the state of the JobMetadataDb is in conflict with the state an update expected. Can
   * be thrown when updates are performed for non-existent entries or if the item being updated has
   * been updated by another process causing a write conflict.
   */
  class JobMetadataConflictException extends Exception {
    /**
     * Creates a new instance of the {@code JobMetadataConflictException} class from a {@code
     * Throwable}.
     */
    public JobMetadataConflictException(Throwable cause) {
      super(cause);
    }
    /**
     * Creates a new instance of the {@code JobMetadataConflictException} class from a message
     * String.
     */
    public JobMetadataConflictException(String message) {
      super(message);
    }
  }

  /** Annotation for the database client to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface JobMetadataDbClient {}
}
