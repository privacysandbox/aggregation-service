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

package com.google.aggregate.adtech.worker.util;

import com.google.scp.operator.cpio.jobclient.model.Job;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Utilities for debug support */
public final class DebugSupportHelper {

  public static final String JOB_PARAM_DEBUG_RUN = "debug_run";

  public static String getDebugFilePrefix(String outputKey) {
    if (outputKey.isEmpty()) {
      throw new IllegalArgumentException("Unable to parse empty file path name.");
    }

    String summaryFolder;
    Path outputKeyPath = Paths.get(outputKey);
    try {
      summaryFolder = outputKeyPath.getParent().toString();
    } catch (NullPointerException e) {
      summaryFolder = "";
    }
    String summaryName = outputKeyPath.getFileName().toString();
    Path debugKeyPath = Paths.get(summaryFolder, "debug", summaryName);
    return debugKeyPath.toString();
  }

  /** Retrieve JOB_PARAM_DEBUG_RUN in JOB PARAM and return whether this is a debug job */
  public static Boolean isDebugRun(Job job) {
    if (job.requestInfo().getJobParameters().containsKey(JOB_PARAM_DEBUG_RUN)) {
      return Boolean.parseBoolean(job.requestInfo().getJobParameters().get(JOB_PARAM_DEBUG_RUN));
    } else {
      return false;
    }
  }
}
