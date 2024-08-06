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

import com.google.aggregate.adtech.worker.ResultLogger;
import com.google.aggregate.adtech.worker.exceptions.ResultLogException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.jobclient.model.Job;

/**
 * {@link ResultLogger} implementation to materialized and store aggregation results in memory for
 * testing.
 */
public final class InMemoryResultLogger implements ResultLogger {

  private MaterializedAggregationResults materializedAggregations;
  private MaterializedAggregationResults materializedDebugAggregations;
  private boolean shouldThrow;
  private volatile boolean hasLogged;

  InMemoryResultLogger() {
    materializedAggregations = null;
    shouldThrow = false;
    hasLogged = false;
  }

  public synchronized boolean hasLogged() {
    return hasLogged;
  }

  @Override
  public void logResults(ImmutableList<AggregatedFact> results, Job unused, boolean isDebugRun)
      throws ResultLogException {
    hasLogged = true;

    if (shouldThrow) {
      throw new ResultLogException(new IllegalStateException("Was set to throw"));
    }

    if (isDebugRun) {
      materializedDebugAggregations = MaterializedAggregationResults.of(results.stream());
      System.out.println("Materialized debug results: " + materializedDebugAggregations);
    } else {
      materializedAggregations = MaterializedAggregationResults.of(results.stream());
      System.out.println("Materialized results: " + materializedAggregations);
    }
  }

  /**
   * Gets materialized aggregation results as an ImmutableList of {@link AggregatedFact}
   *
   * @throws ResultLogException if results were not logged prior to calling this method.
   */
  public MaterializedAggregationResults getMaterializedAggregationResults()
      throws ResultLogException {
    if (materializedAggregations == null) {
      throw new ResultLogException(
          new IllegalStateException(
              "MaterializedAggregations is null. Maybe results did not get logged."));
    }

    return materializedAggregations;
  }

  /**
   * Gets materialized debug aggregation results as an ImmutableList of {@link AggregatedFact}
   *
   * @throws ResultLogException if debug results were not logged prior to calling this method.
   */
  public MaterializedAggregationResults getMaterializedDebugAggregationResults()
      throws ResultLogException {
    if (materializedDebugAggregations == null) {
      throw new ResultLogException(
          new IllegalStateException(
              "MaterializedAggregations is null. Maybe results did not get logged."));
    }
    return materializedDebugAggregations;
  }

  public void setShouldThrow(boolean shouldThrow) {
    this.shouldThrow = shouldThrow;
  }
}
