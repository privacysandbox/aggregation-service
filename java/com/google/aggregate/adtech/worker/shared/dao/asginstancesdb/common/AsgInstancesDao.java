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

package com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common;

import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import java.util.List;
import java.util.Optional;

/** Interface for accessing autoscaling group instances table. */
public interface AsgInstancesDao {

  /**
   * Retrieves information on autoscaling group instances. Optional will be empty if no record
   * exists.
   *
   * @param instanceName the compute instance name to get instance info on
   * @throws AsgInstanceDaoException for other failures to read
   */
  Optional<AsgInstance> getAsgInstance(String instanceName) throws AsgInstanceDaoException;

  /**
   * Retrieves information on the autoscaling instance with a particular status. Empty list is
   * returned if no records exist.
   *
   * @param status the instance termination status to query for
   * @throws AsgInstanceDaoException for other failures to read
   */
  List<AsgInstance> getAsgInstancesByStatus(String status) throws AsgInstanceDaoException;

  /**
   * Inserts a terminated instance entry, if the instance already exists, it will update the record.
   *
   * @throws AsgInstanceDaoException for other failures to write
   */
  void upsertAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException;

  /**
   * Updates an existing {@code AsgInstance} and throws exception if the record doesn't exist.
   *
   * @throws AsgInstanceDaoException for other failures to write
   */
  void updateAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException;

  /** Represents an exception thrown by the {@code AsgInstanceDao} class. */
  public class AsgInstanceDaoException extends Exception {
    /** Creates a new instance of the {@code AsgInstanceDaoException} class. */
    public AsgInstanceDaoException(Throwable cause) {
      super(cause);
    }
  }
}
