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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple task list executor that executes a fixed list of {@link Callable} tasks during
 * every invocation of its run() method. The executor executes the task list with a parallelization
 * factor equal to num of threads requested during object creation. This class can be used a given
 * set of tasks needs to be performed multiple times without any changes to the task data.
 */
public final class FixedTaskListExecutor implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(FixedTaskListExecutor.class);
  private final List<Callable<Void>> tasks;
  private final ExecutorService execService;

  public FixedTaskListExecutor(List<Callable<Void>> tasks, int numThreads) {
    this.tasks = tasks;
    execService = Executors.newFixedThreadPool(numThreads);
  }

  @Override
  public void run() {
    try {
      execService.invokeAll(tasks);
    } catch (Exception e) {
      logger.warn("exception in recurring task executor: " + e);
    }
  }
}
