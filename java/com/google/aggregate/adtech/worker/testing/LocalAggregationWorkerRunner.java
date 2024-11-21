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

package com.google.aggregate.adtech.worker.testing;

import com.beust.jcommander.JCommander;
import com.google.aggregate.adtech.worker.AggregationWorker;
import com.google.aggregate.adtech.worker.AggregationWorkerArgs;
import com.google.aggregate.adtech.worker.AggregationWorkerModule;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

/**
 * LocalAggregationWorker is a wrapper on AggregationWorker to provide necessary helper methods used
 * in tests and on demand dev tools.
 */
public final class LocalAggregationWorkerRunner {
  // TODO(b/203693602): Refactor the Hermetic test with this runner.

  private AggregationWorker worker;
  private ServiceManager serviceManager;

  private LocalAggregationWorkerRunner(AggregationWorkerArgs args) {
    createRunner(args);
  }

  /**
   * Creates an aggregation worker runner from a path and args.
   *
   * @param rootDir Path that stores the worker results.
   * @param newArgs Args to create the runner.
   */
  public static LocalAggregationWorkerRunner create(Path rootDir, String[] newArgs) {
    String[] args =
        new String[] {
          "--job_client",
          "LOCAL_FILE",
          "--blob_storage_client",
          "LOCAL_FS_CLIENT",
          "--record_reader",
          "LOCAL_NIO_AVRO",
          "--decryption",
          "HYBRID",
          "--result_logger",
          "IN_MEMORY",
          "--decryption_key_service",
          "LOCAL_FILE_DECRYPTION_KEY_SERVICE",
          "--adtech_region_override",
          "us-east-1",
          "--noising",
          "CONSTANT_NOISING",
          "--timer_exporter",
          "PLAIN_FILE",
          "--local_file_decryption_key_path",
          rootDir.resolve("hybrid.key").toAbsolutePath().toString(),
          "--result_working_directory_path",
          rootDir.toAbsolutePath().toString(),
          "--local_file_single_puller_path",
          rootDir.resolve("reports.avro").toAbsolutePath().toString(),
          "--local_file_job_info_path",
          rootDir.resolve("results.json").toAbsolutePath().toString(),
          "--timer_exporter_file_path",
          rootDir.resolve("stopwatches.txt").toAbsolutePath().toString(),
          "--local_output_domain_path",
          rootDir.resolve("domain.txt").toAbsolutePath().toString(),
          "--simulation_inputs",
          "--parallel_summary_upload_enabled",
          "--streaming_output_domain_processing_enabled",
          "--attribution_reporting_debug_api_enabled",
        };
    AggregationWorkerArgs cliArgs = new AggregationWorkerArgs();
    JCommander jCommander =
        JCommander.newBuilder().allowParameterOverwriting(true).addObject(cliArgs).build();
    jCommander.parse(args);
    jCommander.parse(newArgs);
    return new LocalAggregationWorkerRunner(cliArgs);
  }

  /** Create LocalAggregationWorkerRunner from AggregationWorkerArgs. */
  private LocalAggregationWorkerRunner createRunner(AggregationWorkerArgs args) {
    AggregationWorkerModule guiceModule = new AggregationWorkerModule(args);
    worker = AggregationWorker.fromModule(guiceModule);
    serviceManager = worker.createServiceManager();
    return this;
  }

  /** Service manager starts the service and the job would start processing. */
  public void run() {
    serviceManager.startAsync();
    serviceManager.awaitStopped();
  }

  /** Wait for the aggregation job complete and return the aggregated results. */
  public ImmutableList<AggregatedFact> waitForAggregation()
      throws ResultLogException, TimeoutException {
    InMemoryResultLogger logger = worker.getInjector().getInstance(InMemoryResultLogger.class);
    MaterializedAggregationResults results = null;
    boolean loggerTriggered = false;

    if (logger.hasLogged()) {
      loggerTriggered = true;
      results = logger.getMaterializedAggregationResults();
    }

    if (results == null) {
      // Worker hasn't completed after polling.
      if (loggerTriggered) {
        throw new ResultLogException(
            new IllegalStateException(
                "MaterializedAggregations is null. Maybe results did not get logged."));
      }
      throw new TimeoutException("logResults is never called. Worker timed out.");
    }
    return results.getMaterializedAggregations();
  }
}
