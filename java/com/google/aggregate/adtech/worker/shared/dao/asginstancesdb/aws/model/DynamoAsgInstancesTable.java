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

package com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.model;

import static com.google.scp.shared.proto.ProtoUtil.toJavaInstant;
import static com.google.scp.shared.proto.ProtoUtil.toProtoTimestamp;

import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceStatusProto.InstanceStatus;
import com.google.aggregate.protos.shared.backend.asginstance.InstanceTerminationReasonProto.InstanceTerminationReason;
import com.google.aggregate.adtech.worker.shared.model.BackendModelUtil;
import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;

/** DynamoDB table schema for AsgInstances table. */
public final class DynamoAsgInstancesTable {

  // AsgInstances Table Attributes
  private static final String INSTANCE_NAME = "InstanceName";
  private static final String STATUS = "Status";
  private static final String REQUEST_TIME = "RequestTime";
  private static final String TERMINATION_TIME = "TerminationTime";
  private static final String TTL = "Ttl";
  private static final String LAST_HEARTBEAT_TIME = "LastHeartbeatTime";
  private static final String TERMINATION_REASON = "TerminationReason";

  /** Returns the table schema for the DynamoDB representation of {@code AsgInstance}. */
  public static TableSchema<AsgInstance> getDynamoDbTableSchema() {
    return StaticImmutableTableSchema.builder(AsgInstance.class, AsgInstance.Builder.class)
        .newItemBuilder(AsgInstance::newBuilder, AsgInstance.Builder::build)
        .addAttribute(
            String.class,
            attribute ->
                attribute
                    .name(INSTANCE_NAME)
                    .getter(AsgInstance::getInstanceName)
                    .setter(AsgInstance.Builder::setInstanceName)
                    .tags(StaticAttributeTags.primaryPartitionKey()))
        .addAttribute(
            InstanceStatus.class,
            attribute ->
                attribute
                    .name(STATUS)
                    .getter(AsgInstance::getStatus)
                    .setter(AsgInstance.Builder::setStatus))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(REQUEST_TIME)
                    .getter(attributeValue -> toJavaInstant(attributeValue.getRequestTime()))
                    .setter(
                        (builder, instant) -> builder.setRequestTime(toProtoTimestamp(instant))))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(TERMINATION_TIME)
                    .getter(BackendModelUtil::getAsgInstanceTerminationTimeValue)
                    .setter(BackendModelUtil::setAsgInstanceTerminationTimeValue))
        .addAttribute(
            Long.class,
            attribute ->
                attribute.name(TTL).getter(AsgInstance::getTtl).setter(AsgInstance.Builder::setTtl))
        .addAttribute(
            Instant.class,
            attribute ->
                attribute
                    .name(LAST_HEARTBEAT_TIME)
                    .getter(BackendModelUtil::getAsgInstanceLastHeartbeatTimeValue)
                    .setter(BackendModelUtil::setAsgInstanceLastHeartbeatTimeValue))
        .addAttribute(
            InstanceTerminationReason.class,
            attribute ->
                attribute
                    .name(TERMINATION_REASON)
                    .getter(AsgInstance::getTerminationReason)
                    .setter(AsgInstance.Builder::setTerminationReason))
        .build();
  }
}
