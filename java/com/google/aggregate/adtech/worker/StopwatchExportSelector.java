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

import com.google.aggregate.perf.StopwatchExporter;
import com.google.aggregate.perf.export.AwsStopwatchExporter;
import com.google.aggregate.perf.export.NoOpStopwatchExporter;
import com.google.aggregate.perf.export.PlainFileStopwatchExporter;

/** CLI enum to select which stopwatch exporter to use in the binary */
public enum StopwatchExportSelector {
  NO_OP(NoOpStopwatchExporter.class),
  PLAIN_FILE(PlainFileStopwatchExporter.class),
  AWS(AwsStopwatchExporter.class);

  private final Class<? extends StopwatchExporter> exporterClass;

  StopwatchExportSelector(Class<? extends StopwatchExporter> exporterClass) {
    this.exporterClass = exporterClass;
  }

  Class<? extends StopwatchExporter> getExporterClass() {
    return exporterClass;
  }
}
