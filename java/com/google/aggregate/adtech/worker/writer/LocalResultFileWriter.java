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

package com.google.aggregate.adtech.worker.writer;

import com.google.aggregate.adtech.worker.model.AggregatedFact;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Writes a stream of AggregatedFacts to a result file on the local filesystem. */
public interface LocalResultFileWriter {

  /** Write the file to the local filesystem */
  void writeLocalFile(Stream<AggregatedFact> results, Path resultFile) throws FileWriteException;

  /** Returns the file extension for the file type written */
  String getFileExtension();

  final class FileWriteException extends Exception {

    public FileWriteException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
