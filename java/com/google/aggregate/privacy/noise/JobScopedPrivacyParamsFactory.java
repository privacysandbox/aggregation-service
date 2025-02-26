/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.privacy.noise;

import static com.google.aggregate.adtech.worker.AggregationWorkerReturnCode.INVALID_JOB;
import static com.google.aggregate.adtech.worker.util.JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON;

import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.util.JobUtils;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.LaplaceDpParams;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.inject.Inject;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builds {@link JobScopedPrivacyParams} by combining default params with a specific job. */
public class JobScopedPrivacyParamsFactory {

  private static final Logger logger = LoggerFactory.getLogger(JobScopedPrivacyParamsFactory.class);

  private final Supplier<PrivacyParameters> defaultParams;

  @Inject
  JobScopedPrivacyParamsFactory(Supplier<PrivacyParameters> defaultParams) {
    this.defaultParams = defaultParams;
  }

  /**
   * Builds {@link JobScopedPrivacyParams} by combining default params with a specific job's request
   * info.
   */
  public JobScopedPrivacyParams fromRequestInfo(RequestInfo info)
      throws AggregationJobProcessException {
    LaplaceDpParams.Builder builder =
        LaplaceDpParams.builder()
            .setL1Sensitivity(defaultParams.get().getL1Sensitivity())
            .setDelta(defaultParams.get().getDelta());

    Optional<Double> debugPrivacyEpsilon = getDebugPrivacyEpsilon(info);
    if (debugPrivacyEpsilon.isPresent()) {
      if (!(debugPrivacyEpsilon.get() > 0d && debugPrivacyEpsilon.get() <= 64d)) {
        throw new AggregationJobProcessException(
            INVALID_JOB,
            String.format(
                "Failed Parsing Job parameters for %s", JobUtils.JOB_PARAM_DEBUG_PRIVACY_EPSILON));
      }
      builder.setEpsilon(debugPrivacyEpsilon.get());
    } else {
      builder.setEpsilon(defaultParams.get().getEpsilon());
    }

    return JobScopedPrivacyParams.ofLaplace(builder.build());
  }

  private static Optional<Double> getDebugPrivacyEpsilon(RequestInfo requestInfo) {
    Optional<Double> epsilonValueFromJobReq = Optional.empty();
    try {
      if (requestInfo.containsJobParameters(JOB_PARAM_DEBUG_PRIVACY_EPSILON)) {
        epsilonValueFromJobReq =
            Optional.of(
                Double.parseDouble(
                    requestInfo.getJobParametersMap().get(JOB_PARAM_DEBUG_PRIVACY_EPSILON)));
      }
    } catch (NumberFormatException e) {
      logger.error(
          String.format("Failed Parsing Job parameters for %s", JOB_PARAM_DEBUG_PRIVACY_EPSILON),
          e);
    }

    return epsilonValueFromJobReq;
  }
}
