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

import com.google.cloud.compute.v1.Autoscaler;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Requests instance termination if the number of instances in the instance group is greater than
 * the autoscaler recommended size.
 */
public class RequestScaleInTask {

  private final GcpInstanceManagementClient instanceManagementClient;
  private final AsgInstancesDao asgInstancesDao;
  private final Clock clock;
  private final int ttlDays;

  private final Logger logger = LoggerFactory.getLogger(RequestScaleInTask.class);

  /** Creates a new instance of the {@code RequestScaleInTask} class. */
  @Inject
  public RequestScaleInTask(
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
   * Creates a termination request entry for the number of instances in the instance group minus the
   * instance group autoscaler recommended size. The instances to terminate will be chosen to
   * balance the number of instances per zone.
   */
  public void requestScaleIn(Map<String, List<GcpComputeInstance>> zoneToRemainingInstances) {
    logger.info("Remaining instances map: " + zoneToRemainingInstances);
    Optional<Autoscaler> autoscaler = instanceManagementClient.getAutoscaler();
    if (autoscaler.isPresent() && autoscaler.get().hasRecommendedSize()) {
      Map<String, Integer> zoneToInstanceCount =
          zoneToRemainingInstances.keySet().stream()
              .collect(
                  Collectors.toMap(key -> key, key -> zoneToRemainingInstances.get(key).size()));
      Integer numInstances = zoneToInstanceCount.values().stream().reduce(0, Integer::sum);
      Integer desiredNumInstances = autoscaler.get().getRecommendedSize();
      Integer numOfInstancesToDelete = Math.max(0, numInstances - desiredNumInstances);
      logger.info(
          String.format(
              "Current instances: %d Recommended size: %d Number of instances to delete: %d",
              numInstances, autoscaler.get().getRecommendedSize(), numOfInstancesToDelete));
      if (numOfInstancesToDelete == 0) {
        return;
      }

      // Create an alternating list of instances based on zone.
      Integer maxNumZoneInstances = Collections.max(zoneToInstanceCount.values());
      List<GcpComputeInstance> orderedZoneInstances = new ArrayList<>();
      for (int i = 0; i < maxNumZoneInstances; i++) {
        Collection<List<GcpComputeInstance>> zoneInstancesList = zoneToRemainingInstances.values();
        for (List<GcpComputeInstance> zoneInstances : zoneInstancesList) {
          if (i < zoneInstances.size()) {
            orderedZoneInstances.add(zoneInstances.get(i));
          }
        }
      }

      // Create termination requests for number of instances over recommended capacity.
      List<GcpComputeInstance> instancesToDelete =
          orderedZoneInstances.subList(
              Math.max(orderedZoneInstances.size() - numOfInstancesToDelete, 0),
              orderedZoneInstances.size());
      for (GcpComputeInstance instance : instancesToDelete) {
        try {
          Instant now = Instant.now();
          AsgInstance instanceToTerminate =
              AsgInstance.newBuilder()
                  .setInstanceName(instance.getInstanceId())
                  .setStatus(InstanceStatus.TERMINATING_WAIT)
                  .setRequestTime(ProtoUtil.toProtoTimestamp(Instant.now()))
                  .setTtl(now.plus(ttlDays, ChronoUnit.DAYS).getEpochSecond())
                  .setTerminationReason(InstanceTerminationReason.SCALE_IN)
                  .build();
          logger.info("Adding instance " + instance + " for termination due to a scale-in.");
          asgInstancesDao.upsertAsgInstance(instanceToTerminate);
        } catch (AsgInstanceDaoException e) {
          logger.info("Failed to mark instance for termination: " + instance, e);
        }
      }
    }
  }
}
