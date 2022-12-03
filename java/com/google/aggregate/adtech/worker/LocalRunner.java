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

import com.beust.jcommander.JCommander;
import com.google.aggregate.shared.LicenseUtil;
import com.google.common.util.concurrent.ServiceManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main entry point for the Standalone worker library. Corresponding e2e test is {@code
 * LocalRunnerTest}
 */
public final class LocalRunner {

  public static void main(String[] args) throws IOException {
    internalMain(args);
  }

  public static ServiceManager internalMain(String[] args) throws IOException {
    LocalWorkerArgs localWorkerArgs = new LocalWorkerArgs();
    JCommander jc =
        JCommander.newBuilder().allowParameterOverwriting(true).addObject(localWorkerArgs).build();
    jc.parse(args);

    if (localWorkerArgs.isHelp() || (args == null || args.length == 0)) {
      jc.setProgramName("Aggregation Library");
      jc.usage();
      return null;
    }
    if (localWorkerArgs.isPrintLicenses()) {
      LicenseUtil.printLicenses();
      return null;
    } else {
      localWorkerArgs.validate();
      createDirectories(Path.of(localWorkerArgs.getOutputDirectory()));
      LocalWorkerModule guiceModule = new LocalWorkerModule(localWorkerArgs);
      AggregationWorker worker = AggregationWorker.fromModule(guiceModule);
      return worker.createServiceManager().startAsync();
    }
  }

  private static void createDirectories(Path pathToDirectory) throws IOException {
    Files.createDirectories(pathToDirectory);
  }
}
