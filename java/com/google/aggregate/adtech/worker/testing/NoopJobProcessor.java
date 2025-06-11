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

import com.google.aggregate.adtech.worker.AggregationWorkerReturnCode;
import com.google.aggregate.adtech.worker.JobProcessor;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Work processor that does nothing except capture the last job provided to it */
public final class NoopJobProcessor implements JobProcessor {

  public enum ExceptionToThrow {
    AggregationJobProcess,
    IllegalState,
    Interrupted,
    None
  }

  private Optional<Job> lastProcessed = Optional.empty();
  private JobResult jobResultToReturn;
  private ExceptionToThrow toThrow;

  NoopJobProcessor() {
    jobResultToReturn = null;
    toThrow = ExceptionToThrow.None;
  }

  @Override
  public JobResult process(Job Job)
      throws ExecutionException,
          InterruptedException,
          AggregationJobProcessException,
          IllegalStateException {
    switch (toThrow) {
      case AggregationJobProcess:
        throw new AggregationJobProcessException(
            AggregationWorkerReturnCode.INVALID_JOB, "Was set to throw");
      case Interrupted:
        throw new InterruptedException("Was set to throw");
      case IllegalState:
        throw new IllegalStateException("Was set to throw");
      case None:
        // Job processing will continue as normal
    }
    lastProcessed = Optional.of(Job);
    return jobResultToReturn;
  }

  /** Gets the previous Job that was passed into process where an exception was not thrown */
  public Optional<Job> getLastProcessed() {
    return lastProcessed;
  }

  /** Sets the JobResult to be returned by process() if an exception is not thrown */
  public void setJobResultToReturn(JobResult jobResultToReturn) {
    this.jobResultToReturn = jobResultToReturn;
  }

  /** Sets to throw an exception whose type is based on the parameter */
  public void setShouldThrowException(ExceptionToThrow toThrow) {
    this.toThrow = toThrow;
  }
}
