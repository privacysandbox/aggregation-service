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

package com.google.aggregate.adtech.worker.lifecycleclient.gcp;

import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.lifecycleclient.LifecycleClient;
import com.google.aggregate.adtech.worker.lifecycleclient.gcp.Annotations.GcpInstanceUrl;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus.TERMINATING_WAIT;

/** GCP implementation of {@code LifecycleClient}. */
public final class GcpLifecycleClient implements LifecycleClient {
  private static final Logger logger = LoggerFactory.getLogger(GcpLifecycleClient.class);

  private AsgInstancesDao asgInstancesDao;
  private GcpInstanceGroupClient instanceGroupClient;
  private String instanceUrl;

  @Inject
  GcpLifecycleClient(
      AsgInstancesDao asgInstancesDao,
      GcpInstanceGroupClient instanceGroupClient,
      @GcpInstanceUrl String instanceUrl) {
    this.asgInstancesDao = asgInstancesDao;
    this.instanceGroupClient = instanceGroupClient;
    this.instanceUrl = instanceUrl;
  }

  /**
   * Returns the lifecycle status of the current instance from the AsgInstances table. If no record
   * is found, then an empty Optional is returned.
   */
  @Override
  public Optional<String> getLifecycleState() throws LifecycleClientException {
    Optional<AsgInstance> asgInstance;
    try {
      asgInstance = asgInstancesDao.getAsgInstance(instanceUrl);
    } catch (AsgInstanceDaoException e) {
      throw new LifecycleClientException(e);
    }
    return asgInstance.isPresent()
        ? Optional.of(asgInstance.get().getStatus().toString())
        : Optional.empty();
  }

  /**
   * Checks the AsgInstances table to see if the current instance is a candidate for termination. If
   * the instance is a candidate for termination, then the delete instance API is called. Otherwise
   * return false.
   */
  @Override
  public boolean handleScaleInLifecycleAction() throws LifecycleClientException {
    try {
      Optional<AsgInstance> asgInstance = asgInstancesDao.getAsgInstance(instanceUrl);
      if (asgInstance.isPresent() && TERMINATING_WAIT.equals(asgInstance.get().getStatus())) {
        logger.info("Deleting instance: " + asgInstance);
        instanceGroupClient.deleteInstance();
        return true;
      }
      return false;
    } catch (InterruptedException | ExecutionException | AsgInstanceDaoException e) {
      throw new LifecycleClientException(e);
    }
  }
}
