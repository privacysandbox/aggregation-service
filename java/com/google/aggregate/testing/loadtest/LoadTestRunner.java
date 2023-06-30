/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.testing.loadtest;

import com.beust.jcommander.JCommander;
import com.google.aggregate.privacy.budgeting.bridge.HttpPrivacyBudgetingServiceBridge;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoadTestRunner {

  public static void main(String[] args) throws InterruptedException {
    LoadTestArgs cliArgs = new LoadTestArgs();
    JCommander.newBuilder().allowParameterOverwriting(true).addObject(cliArgs).build().parse(args);

    ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(cliArgs.getParallelTransactionsPerSecond());
    var guiceInjector = initialiseAndGetGuiceInjector(cliArgs);
    List<Callable<Void>> transactionsExecutables =
        IntStream.range(0, cliArgs.getParallelTransactionsPerSecond())
            .mapToObj(
                idx ->
                    new TransactionExecutor(
                        getPrivacyBudgetBridge(guiceInjector),
                        cliArgs.getNumPbsKeysPerTransaction(),
                        cliArgs.getReportingOrigin()))
            .collect(Collectors.toList());
    FixedTaskListExecutor fixedTaskListExecutor =
        new FixedTaskListExecutor(
            transactionsExecutables, cliArgs.getParallelTransactionsPerSecond());
    scheduler.scheduleAtFixedRate(fixedTaskListExecutor, 1, 1, TimeUnit.SECONDS);
    for (int minsElapsed = 0; minsElapsed < cliArgs.getTaskDurationMins(); ) {
      Thread.sleep(Duration.of(60, ChronoUnit.SECONDS).toMillis());
      System.out.println("Minutes elapsed: " + ++minsElapsed);
    }
    System.out.println("Shutting down the transaction execution scheduler");
    scheduler.shutdown();
    scheduler.awaitTermination(60, TimeUnit.SECONDS);
    System.out.println(
        "Transaction execution scheduler shut down successfully. Exiting with success");
    System.exit(0);
  }

  private static Injector initialiseAndGetGuiceInjector(LoadTestArgs cliArgs) {
    LoadTestModule module = new LoadTestModule(cliArgs);
    return Guice.createInjector(module);
  }

  private static HttpPrivacyBudgetingServiceBridge getPrivacyBudgetBridge(Injector injector) {
    return new HttpPrivacyBudgetingServiceBridge(
        injector.getInstance(DistributedPrivacyBudgetClient.class));
  }
}
