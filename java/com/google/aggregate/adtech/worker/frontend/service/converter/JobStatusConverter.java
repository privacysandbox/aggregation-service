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

package com.google.aggregate.adtech.worker.frontend.service.converter;

import com.google.common.base.Converter;
import com.google.aggregate.protos.frontend.api.v1.JobStatusProto;

/**
 * Converts between the {@link
 * com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus} and {@link
 * JobStatusProto.JobStatus}. *
 */
public final class JobStatusConverter
    extends Converter<
        com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus,
        JobStatusProto.JobStatus> {

  /** Converts the shared model into the frontend model. */
  @Override
  protected JobStatusProto.JobStatus doForward(
      com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus jobStatus) {
    switch (jobStatus) {
      case UNRECOGNIZED:
        return JobStatusProto.JobStatus.UNRECOGNIZED;
      case JOB_STATUS_UNKNOWN:
        return JobStatusProto.JobStatus.JOB_STATUS_UNKNOWN;
      case RECEIVED:
        return JobStatusProto.JobStatus.RECEIVED;
      case IN_PROGRESS:
        return JobStatusProto.JobStatus.IN_PROGRESS;
      case FINISHED:
        return JobStatusProto.JobStatus.FINISHED;
      default:
        throw new IllegalArgumentException("Unknown JobStatus:" + jobStatus);
    }
  }

  /** Converts the frontend model into the shared model. */
  @Override
  protected com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus doBackward(
      JobStatusProto.JobStatus jobStatus) {
    switch (jobStatus) {
      case UNRECOGNIZED:
        return com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.UNRECOGNIZED;
      case JOB_STATUS_UNKNOWN:
        return com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus
            .JOB_STATUS_UNKNOWN;
      case RECEIVED:
        return com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.RECEIVED;
      case IN_PROGRESS:
        return com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.IN_PROGRESS;
      case FINISHED:
        return com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus.FINISHED;
      default:
        throw new IllegalArgumentException("Unknown JobStatus:" + jobStatus);
    }
  }
}
