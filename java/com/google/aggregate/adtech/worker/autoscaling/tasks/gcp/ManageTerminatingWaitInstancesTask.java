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

package com.google.aggregate.adtech.worker.autoscaling.tasks.gcp;

import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.autoscaling.tasks.gcp.Annotations.TerminationWaitTimeout;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import com.google.scp.shared.api.exception.ServiceException;
import com.google.scp.shared.api.model.Code;
import com.google.scp.shared.proto.ProtoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Manages the TERMINATING_WAIT state instances. The TERMINATING_WAIT state instances that are
 * overdue will be deleted and TERMINATING_WAIT state instances that are not in the instance group
 * anymore are marked as TERMINATED.
 */
public class ManageTerminatingWaitInstancesTask {

  private final GcpInstanceManagementClient instanceManagementClient;
  private final AsgInstancesDao asgInstancesDao;
  private final Integer terminationWaitTimeout;

  private final Logger logger = LoggerFactory.getLogger(ManageTerminatingWaitInstancesTask.class);

  /** Creates a new instance of the {@code ManageTerminatingWaitInstancesTask} class. */
  @Inject
  public ManageTerminatingWaitInstancesTask(
      AsgInstancesDao asgInstancesDao,
      GcpInstanceManagementClient instanceManagementClient,
      @TerminationWaitTimeout Integer terminationWaitTimeout) {
    this.asgInstancesDao = asgInstancesDao;
    this.instanceManagementClient = instanceManagementClient;
    this.terminationWaitTimeout = terminationWaitTimeout;
  }

  /**
   * Delete TERMINATING_WAIT instances that are over the termination wait timeout. Complete the
   * termination request if instance was deleted or does not exist in the instance group.
   *
   * @return the remaining instances in the instance group mapped by zone
   * @throws ServiceException
   */
  public Map<String, List<GcpComputeInstance>> manageInstances() throws ServiceException {

    try {
      // Get all compute instances in TERMINATING_WAIT STATE
      List<AsgInstance> terminatingWaitInstances =
          asgInstancesDao.getAsgInstancesByStatus(InstanceStatus.TERMINATING_WAIT.toString());
      logger.info(
          "Number of instances in TERMINATING_WAIT state: " + terminatingWaitInstances.size());

      List<GcpComputeInstance> activeInstances =
          instanceManagementClient.listActiveInstanceGroupInstances();

      // Terminate overdue instances
      Set<String> overdueInstanceTerminations =
          terminatingWaitInstances.stream()
              .filter(
                  instance ->
                      ProtoUtil.toJavaInstant(instance.getRequestTime())
                          .isBefore(
                              Instant.now().minus(terminationWaitTimeout, ChronoUnit.SECONDS)))
              .map(AsgInstance::getInstanceName)
              .collect(Collectors.toSet());
      Set<String> activeInstanceNames =
          activeInstances.stream()
              .map(GcpComputeInstance::getInstanceId)
              .collect(Collectors.toSet());
      overdueInstanceTerminations.retainAll(activeInstanceNames);

      logger.info("Deleting instances: " + overdueInstanceTerminations);
      if (!overdueInstanceTerminations.isEmpty()) {
        instanceManagementClient.deleteInstances(overdueInstanceTerminations);
      }

      // Complete instance termination requests
      List<AsgInstance> asgInstancesThatCompleted =
          terminatingWaitInstances.stream()
              .filter(
                  instance ->
                      overdueInstanceTerminations.contains(instance.getInstanceName())
                          || !activeInstanceNames.contains(instance.getInstanceName()))
              .collect(Collectors.toList());
      logger.info("Completing termination requests in db: " + asgInstancesThatCompleted);
      for (AsgInstance asgWaitInstance : asgInstancesThatCompleted) {
        AsgInstance updatedTerminatedInstance =
            asgWaitInstance.toBuilder()
                .setStatus(InstanceStatus.TERMINATED)
                .setTerminationTime(ProtoUtil.toProtoTimestamp(Instant.now()))
                .build();
        asgInstancesDao.updateAsgInstance(updatedTerminatedInstance);
      }
      logger.info("Done completing termination request in db");

      // Filter and return remaining active instances
      Set<String> terminatingWaitInstanceIds =
          terminatingWaitInstances.stream()
              .map(AsgInstance::getInstanceName)
              .collect(Collectors.toSet());
      return activeInstances.stream()
          .filter(instance -> !terminatingWaitInstanceIds.contains(instance.getInstanceId()))
          .collect(
              Collectors.groupingBy(
                  instance -> getZone(instance.getInstanceId()),
                  Collectors.mapping(instance -> instance, Collectors.toList())));
    } catch (AsgInstanceDaoException | ExecutionException | InterruptedException e) {
      throw new ServiceException(Code.INTERNAL, "InstanceTerminationFailure", e);
    }
  }

  private String getZone(String instanceUrl) {
    Integer zoneStartIndex = instanceUrl.indexOf("zones/") + 6;
    return instanceUrl.substring(zoneStartIndex, instanceUrl.indexOf('/', zoneStartIndex));
  }
}
