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

import com.google.aggregate.adtech.worker.Annotations.DebugWriter;
import com.google.aggregate.adtech.worker.Annotations.ResultWriter;
import com.google.aggregate.adtech.worker.writer.LocalResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroDebugResultFileWriter;
import com.google.aggregate.adtech.worker.writer.avro.LocalAvroResultFileWriter;

/**
 * Module that uses the {@code LocalFileToCloudStorageLogger}, {@code LocalAvroResultFileWriter},
 * and {@code SinglePartAwsS3Writer}
 */
public final class LocalAvroToS3LoggerModule extends ResultLoggerModule {

  @Override
  public Class<? extends ResultLogger> getResultLoggerImplementation() {
    return LocalFileToCloudStorageLogger.class;
  }

  @Override
  public void configureModule() {
    bind(LocalResultFileWriter.class)
        .annotatedWith(ResultWriter.class)
        .to(LocalAvroResultFileWriter.class);

    bind(LocalResultFileWriter.class)
        .annotatedWith(DebugWriter.class)
        .to(LocalAvroDebugResultFileWriter.class);
  }
}
