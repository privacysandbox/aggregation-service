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

package com.google.aggregate.perf;

/** Interface for exporting stopwatches to some storage determined by the implementation */
public interface StopwatchExporter {

  /** Exports the provided stopwatch registry to a file */
  void export(StopwatchRegistry stopwatches) throws StopwatchExportException;

  /** Exception for errors related to stopwatch export */
  final class StopwatchExportException extends Exception {

    public StopwatchExportException(Throwable cause) {
      super(cause);
    }
  }
}
