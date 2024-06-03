/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.ErrorCounter.DEBUG_NOT_ENABLED;
import static com.google.aggregate.adtech.worker.validation.ValidatorHelper.createErrorMessage;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.util.DebugSupportHelper;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;

/** Validates that the report's debugMode is enabled. */
public final class ReportForDebugValidator implements ReportValidator {

  public static final String JOB_PARAM_DEBUG_RUN = "debug_run";

  /**
   * The report is valid when: 1. this is not a debug job or 2. this is a debug job and the
   * debugMode in sharedInfo is `enabled`
   */
  @Override
  public Optional<ErrorMessage> validate(Report report, Job job) {
    boolean debugRun = DebugSupportHelper.isDebugRun(job);
    boolean reportDebugMode = report.sharedInfo().getReportDebugMode();

    if (!debugRun || reportDebugMode) {
      return Optional.empty();
    }

    return createErrorMessage(DEBUG_NOT_ENABLED);
  }
}
