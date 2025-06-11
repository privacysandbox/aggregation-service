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
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceTerminationReasonProto.InstanceTerminationReason;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao.AsgInstanceDaoException;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp.SpannerAsgInstancesDao.AsgInstancesDbSpannerTtlDays;
import com.google.scp.shared.proto.ProtoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Requests instance termination if the number of instances in the instance group is greater than
 * the autoscaler recommended size.
 */
public class RequestUpdateTask {

  private final GcpInstanceManagementClient instanceManagementClient;
  private final AsgInstancesDao asgInstancesDao;
  private final Clock clock;
  private final int ttlDays;

  private final Logger logger = LoggerFactory.getLogger(RequestUpdateTask.class);

  /** Creates a new instance of the {@code RequestUpdateTask} class. */
  @Inject
  public RequestUpdateTask(
      GcpInstanceManagementClient instanceManagementClient,
      AsgInstancesDao asgInstancesDao,
      Clock clock,
      @AsgInstancesDbSpannerTtlDays int ttlDays) {
    this.instanceManagementClient = instanceManagementClient;
    this.asgInstancesDao = asgInstancesDao;
    this.clock = clock;
    this.ttlDays = ttlDays;
  }

  /**
   * Creates a termination request entry for any instances running an outdated instance template.
   */
  public Map<String, List<GcpComputeInstance>> requestUpdate(
      Map<String, List<GcpComputeInstance>> zoneToRemainingInstances) {
    logger.info("Remaining instances map before updates: " + zoneToRemainingInstances);

    // Separates out instances that will be updated.
    String instanceTemplate = instanceManagementClient.getCurrentInstanceTemplate();
    logger.info("Current instance group template: " + instanceTemplate);
    Map<String, List<GcpComputeInstance>> filteredZoneToRemainingInstances = new HashMap<>();
    List<String> instancesToUpdate = new ArrayList<>();
    zoneToRemainingInstances.forEach(
        (zone, zoneInstances) -> {
          List<GcpComputeInstance> filteredZoneInstances = new ArrayList<>();
          zoneInstances.forEach(
              instance -> {
                if (instance.getInstanceTemplate().equals(instanceTemplate)) {
                  filteredZoneInstances.add(instance);
                } else {
                  instancesToUpdate.add(instance.getInstanceId());
                  logger.info(
                      "UPDATE REQUIRED: "
                          + instance.getInstanceId()
                          + " using template "
                          + instance.getInstanceTemplate()
                          + ". Update required to template "
                          + instanceTemplate
                          + ".");
                }
              });
          if (!filteredZoneInstances.isEmpty()) {
            filteredZoneToRemainingInstances.put(zone, filteredZoneInstances);
          }
        });

    // Create termination requests for instances needing an update.
    for (String instance : instancesToUpdate) {
      try {
        Instant now = Instant.now();
        AsgInstance instanceToTerminate =
            AsgInstance.newBuilder()
                .setInstanceName(instance)
                .setStatus(InstanceStatus.TERMINATING_WAIT)
                .setRequestTime(ProtoUtil.toProtoTimestamp(Instant.now()))
                .setTtl(now.plus(ttlDays, ChronoUnit.DAYS).getEpochSecond())
                .setTerminationReason(InstanceTerminationReason.UPDATE)
                .build();
        logger.info("Adding instance " + instance + " for termination due to an update.");
        asgInstancesDao.upsertAsgInstance(instanceToTerminate);
      } catch (AsgInstanceDaoException e) {
        logger.info("Failed to mark instance for termination: " + instance, e);
      }
    }
    return filteredZoneToRemainingInstances;
  }
}
