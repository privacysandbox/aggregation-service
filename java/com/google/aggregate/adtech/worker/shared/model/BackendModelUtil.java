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

package com.google.aggregate.adtech.worker.shared.model;

import com.google.aggregate.protos.shared.backend.CreateJobRequestProto.CreateJobRequest;
import com.google.aggregate.protos.shared.backend.JobKeyProto;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.scp.shared.proto.ProtoUtil.toJavaInstant;
import static com.google.scp.shared.proto.ProtoUtil.toProtoTimestamp;

/** Utilities for backend models. */
public class BackendModelUtil {

  private BackendModelUtil() {}

  /** String representation of the key. */
  public static String toJobKeyString(JobKeyProto.JobKey jobKey) {
    return jobKey.getJobRequestId();
  }

  /** Performs equality check of JobMetadata ignoring recordVersion and ttl. */
  public static boolean equalsIgnoreDbFields(JobMetadata one, JobMetadata two) {
    JobMetadata oneNoVersion = one.toBuilder().clearRecordVersion().clearTtl().build();
    JobMetadata twoNoVersion = two.toBuilder().clearRecordVersion().clearTtl().build();
    return oneNoVersion.equals(twoNoVersion);
  }

  /**
   * Returns the JobMetadata.createJobRequest value, or null if it is not set. This is used in
   * conversion of model to schemas which accept null instead of Optional.
   */
  @Nullable
  @Deprecated
  public static CreateJobRequest getCreateJobRequestValue(JobMetadata jobMetadata) {
    return jobMetadata.hasCreateJobRequest() ? jobMetadata.getCreateJobRequest() : null;
  }

  /**
   * Sets the JobMetadata.createJobRequest value if the given {@param createJobRequest} is not null.
   * This is used in conversion of model to schemas which accept null instead of Optional.
   */
  @Deprecated
  public static void setCreateJobRequestValue(
      JobMetadata.Builder jobMetadata, @Nullable CreateJobRequest createJobRequest) {
    if (createJobRequest != null) {
      jobMetadata.setCreateJobRequest(createJobRequest);
    }
  }

  /**
   * Returns JobMetadata.requestInfo value, or null if it is not set. This is used in conversion of
   * model to schemas which accept null instead of Optional.
   */
  @Nullable
  public static RequestInfo getRequestInfoValue(JobMetadata jobMetadata) {
    return jobMetadata.hasRequestInfo() ? jobMetadata.getRequestInfo() : null;
  }

  /**
   * Sets the JobMetadata.requestInfo value if the given {@param jobMetadata} is not null. This is
   * used in conversion of model to schemas which accept null instead of Optional.
   */
  public static void setRequestInfoValue(
      JobMetadata.Builder jobMetadata, @Nullable RequestInfo requestInfo) {
    if (requestInfo != null) {
      jobMetadata.setRequestInfo(requestInfo);
    }
  }

  /**
   * Returns JobMetadata.resultInfo value, or null if it is not set. This is used in conversion of
   * model to schemas which accept null instead of Optional.
   */
  @Nullable
  public static ResultInfo getResultInfoValue(JobMetadata jobMetadata) {
    return jobMetadata.hasResultInfo() ? jobMetadata.getResultInfo() : null;
  }

  /**
   * Sets the JobMetadata.resultInfo value if the given {@param jobMetadata} is not null. This is
   * used in conversion of model to schemas which accept null instead of Optional.
   */
  public static void setResultInfoValue(
      JobMetadata.Builder jobMetadata, @Nullable ResultInfo resultInfo) {
    if (resultInfo != null) {
      jobMetadata.setResultInfo(resultInfo);
    }
  }

  /**
   * Returns JobMetadata.requestProcessingStartedAt value, or null if it is not set. This is used in
   * conversion of model to schemas which accept null instead of Optional.
   */
  @Nullable
  public static Instant getRequestProcessingStartedTimeValue(JobMetadata jobMetadata) {
    return jobMetadata.hasRequestProcessingStartedAt()
        ? toJavaInstant(jobMetadata.getRequestProcessingStartedAt())
        : null;
  }

  /**
   * Sets the JobMetadata.requestProcessingStartedAt value if the given {@param jobMetadata} is not
   * null. This is used in conversion of model to schemas which accept null instead of Optional.
   */
  public static void setRequestProcessingStartedTimeValue(
      JobMetadata.Builder jobMetadata, @Nullable Instant requestProcessingStartedAt) {
    if (requestProcessingStartedAt != null) {
      jobMetadata.setRequestProcessingStartedAt(toProtoTimestamp(requestProcessingStartedAt));
    }
  }

  /**
   * Returns AsgInstance.terminationTime value, or null if it is not set. This is used in conversion
   * of model to schemas which accept null instead of Optional.
   */
  @Nullable
  public static Instant getAsgInstanceTerminationTimeValue(AsgInstance asgInstance) {
    return asgInstance.hasTerminationTime()
        ? toJavaInstant(asgInstance.getTerminationTime())
        : null;
  }

  /**
   * Sets the AsgInstance.terminationTime value if the given {@param asgInstance} is not null. This
   * is used in conversion of model to schemas which accept null instead of Optional.
   */
  public static void setAsgInstanceTerminationTimeValue(
      AsgInstance.Builder asgInstance, @Nullable Instant terminationTime) {
    if (terminationTime != null) {
      asgInstance.setTerminationTime(toProtoTimestamp(terminationTime));
    }
  }

  /**
   * Returns AsgInstance.lastHeartbeatTime value, or null if it is not set. This is used in
   * conversion of model to schemas which accept null instead of Optional.
   */
  @Nullable
  public static Instant getAsgInstanceLastHeartbeatTimeValue(AsgInstance asgInstance) {
    return asgInstance.hasLastHeartbeatTime()
        ? toJavaInstant(asgInstance.getLastHeartbeatTime())
        : null;
  }

  /**
   * Sets the AsgInstance.lastHeartbeatTime value if the given {@param asgInstance} is not null.
   * This is used in conversion of model to schemas which accept null instead of Optional.
   */
  public static void setAsgInstanceLastHeartbeatTimeValue(
      AsgInstance.Builder asgInstance, @Nullable Instant lastHeartbeatTime) {
    if (lastHeartbeatTime != null) {
      asgInstance.setLastHeartbeatTime(toProtoTimestamp(lastHeartbeatTime));
    }
  }
}
