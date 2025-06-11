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

package com.google.aggregate.adtech.worker;

import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.privacy.noise.model.SummaryReportAvro;
import com.google.common.collect.ImmutableList;
import com.google.aggregate.adtech.worker.jobclient.model.Job;

/** Interface for storing the results of the aggregation worker */
public interface ResultLogger {

  /** Takes the aggregation results and logs them to results. */
  void logResults(ImmutableList<AggregatedFact> results, Job ctx, boolean isDebugRun)
      throws ResultLogException;

  void logResultsAvros(
      ImmutableList<SummaryReportAvro> summaryReportAvros, Job ctx, boolean isDebugRun);

  /**
   * Writes PrivacyBudgetExhaustedInfo to persistent storage.
   *
   * @param privacyBudgetExhaustedDebuggingInfo the information to be written
   * @param ctx the job context
   * @param fileName the name of the file to write privacy budget debugging information to.
   * @return String representing the complete path of the privacy budget debugging information file,
   *     including bucket and prefix both.
   */
  String writePrivacyBudgetExhaustedDebuggingInformation(
      PrivacyBudgetExhaustedInfo privacyBudgetExhaustedDebuggingInfo, Job ctx, String fileName);
}
