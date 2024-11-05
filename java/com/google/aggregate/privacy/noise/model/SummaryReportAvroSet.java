/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.privacy.noise.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** SummaryReport set (for one job) */
@AutoValue
public abstract class SummaryReportAvroSet {

  public abstract ImmutableList<SummaryReportAvro> summaryReports();

  public abstract Optional<ImmutableList<SummaryReportAvro>> debugSummaryReport();

  public static SummaryReportAvroSet create(
      ImmutableList<SummaryReportAvro> summaryReportAvros,
      Optional<ImmutableList<SummaryReportAvro>> debugSummaryReportAvros) {
    return new AutoValue_SummaryReportAvroSet(summaryReportAvros, debugSummaryReportAvros);
  }
}
