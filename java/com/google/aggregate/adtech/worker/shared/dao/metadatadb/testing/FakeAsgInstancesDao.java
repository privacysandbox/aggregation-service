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

import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake implementation of the {@link AsgInstancesDao} for use in tests. */
public class FakeAsgInstancesDao implements AsgInstancesDao {

  private Optional<AsgInstance> asgInstanceToReturn;
  private String lastInstanceNameLookedUp;
  private AsgInstance lastInstanceInserted;
  private AsgInstance lastInstanceUpdated;
  private Map<String, Integer> zoneToInstanceCountMap;

  private boolean shouldThrowAsgInstancesDaoException;

  public FakeAsgInstancesDao() {
    reset();
  }

  @Override
  public Optional<AsgInstance> getAsgInstance(String instanceName) throws AsgInstanceDaoException {
    if (shouldThrowAsgInstancesDaoException) {
      throw new AsgInstanceDaoException(
          new IllegalStateException("Was set to throw (shouldThrowAsgInstancesDaoException)"));
    }
    lastInstanceNameLookedUp = instanceName;
    return asgInstanceToReturn;
  }

  @Override
  public List<AsgInstance> getAsgInstancesByStatus(String status) throws AsgInstanceDaoException {
    if (shouldThrowAsgInstancesDaoException) {
      throw new AsgInstanceDaoException(
          new IllegalStateException("Was set to throw (shouldThrowAsgInstancesDaoException)"));
    }

    List<AsgInstance> asgInstanceList = new ArrayList<>();
    if (asgInstanceToReturn.isPresent()) {
      asgInstanceList.add(asgInstanceToReturn.get());
    }

    return asgInstanceList;
  }

  @Override
  public void upsertAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException {
    if (shouldThrowAsgInstancesDaoException) {
      throw new AsgInstanceDaoException(
          new IllegalStateException("Was set to throw (shouldThrowAsgInstancesDaoException)"));
    }

    String instanceUrl = asgInstance.getInstanceName();
    Integer zoneStartIndex = instanceUrl.indexOf("zones/") + 6;
    String zone = instanceUrl.substring(zoneStartIndex, instanceUrl.indexOf('/', zoneStartIndex));
    lastInstanceInserted = asgInstance;
    zoneToInstanceCountMap.merge(zone, 1, Integer::sum);
  }

  @Override
  public void updateAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException {
    if (shouldThrowAsgInstancesDaoException) {
      throw new AsgInstanceDaoException(
          new IllegalStateException("Was set to throw (shouldThrowAsgInstancesDaoException)"));
    }

    lastInstanceUpdated = asgInstance;
  }

  /** Set the asg instance to be returned from the {@code getAsgInstances} method. */
  public void setAsgInstanceToReturn(Optional<AsgInstance> asgInstanceToReturn) {
    this.asgInstanceToReturn = asgInstanceToReturn;
  }

  /** Get the most recent asg instance that was inserted. */
  public AsgInstance getLastInstanceInserted() {
    return lastInstanceInserted;
  }

  /** Get the most recent asg instance that was updated. */
  public AsgInstance getLastInstanceUpdated() {
    return lastInstanceUpdated;
  }

  /** Get the zone to instance count map. */
  public Map<String, Integer> getZoneToInstanceCountMap() {
    return zoneToInstanceCountMap;
  }

  /**
   * Set if the {@code getAsgInstance}, {@code upsertAsgInstance}, and {@code updateAsgInstance}
   * methods should throw the {@code asgInstancesDaoException}.
   */
  public void setShouldThrowAsgInstancesDaoException(boolean shouldThrowAsgInstancesDaoException) {
    this.shouldThrowAsgInstancesDaoException = shouldThrowAsgInstancesDaoException;
  }

  /** Sets all internal fields to their default values. */
  public void reset() {
    asgInstanceToReturn = Optional.empty();
    lastInstanceNameLookedUp = null;
    lastInstanceInserted = null;
    lastInstanceUpdated = null;
    shouldThrowAsgInstancesDaoException = false;
    zoneToInstanceCountMap = new HashMap<>();
  }
}
