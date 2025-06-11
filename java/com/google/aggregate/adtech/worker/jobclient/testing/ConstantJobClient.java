/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.jobclient.testing;

import com.google.aggregate.adtech.worker.jobclient.JobClient;
import com.google.aggregate.adtech.worker.jobclient.model.ErrorReason;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.model.JobResult;
import com.google.aggregate.adtech.worker.jobclient.model.JobRetryRequest;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;

import java.util.Optional;

/**
 * Puller implementation that returns a constant provided to it, or returns nothing is available to
 * pull, if set so; this puller exhausts after one pull.
 *
 * <p>If nothing is set explicitly, the puller will return an empty optional.
 */
public class ConstantJobClient implements JobClient {

  private boolean returnEmpty = true;
  private Job constantToReturn = null;
  private boolean shouldThrowOnGetJob;
  private boolean shouldThrowOnMarkJobCompleted;
  private boolean shouldThrowOnAppendJobErrorMessage;

  private boolean shouldThrowOnReturnJobForRetry;

  private JobResult lastJobResultCompleted;

  private void reset() {
    returnEmpty = true;
    constantToReturn = null;
    lastJobResultCompleted = null;
    shouldThrowOnGetJob = false;
    shouldThrowOnMarkJobCompleted = false;
    shouldThrowOnAppendJobErrorMessage = false;
    shouldThrowOnReturnJobForRetry = false;
  }

  @Override
  public Optional<Job> getJob() throws JobClientException {
    if (shouldThrowOnGetJob) {
      // Only throw exception once, mimicking the behavior of only returning the Job once
      setShouldThrowOnGetJob(false);
      throw new JobClientException(
          new IllegalStateException("Was set to throw"), ErrorReason.UNSPECIFIED_ERROR);
    }

    if (returnEmpty) {
      return Optional.empty();
    }

    Job toReturn = constantToReturn;
    setReturnEmpty();
    return Optional.of(toReturn);
  }

  @Override
  public void markJobCompleted(JobResult jobResult) throws JobClientException {
    if (shouldThrowOnMarkJobCompleted) {
      throw new JobClientException(
          new IllegalStateException("Was set to throw"), ErrorReason.UNSPECIFIED_ERROR);
    }

    lastJobResultCompleted = jobResult;
  }

  @Override
  public void returnJobForRetry(JobRetryRequest jobRetryRequest) throws JobClientException {
    if (shouldThrowOnReturnJobForRetry) {
      throw new JobClientException(
          new IllegalStateException("Was set to throw"), ErrorReason.UNSPECIFIED_ERROR);
    }
  }

  @Override
  public void appendJobErrorMessage(JobKey jobKey, String error) throws JobClientException {
    if (shouldThrowOnAppendJobErrorMessage) {
      throw new JobClientException(
          new IllegalStateException("Was set to throw"), ErrorReason.UNSPECIFIED_ERROR);
    }
  }

  /** Set true to return an empty {@code Optional<Job>} from the {@code getJob} method. */
  public void setReturnEmpty() {
    returnEmpty = true;
  }

  /** Set to return a constant instance of {@code Job} from the {@code getJob} method. */
  public void setReturnConstant(Job constant) {
    returnEmpty = false;
    constantToReturn = constant;
  }

  /** Returns the last job result that was completed. */
  public JobResult getLastJobResultCompleted() {
    return lastJobResultCompleted;
  }

  /** Set true to throw an exception in the {@code getJob} method. */
  public void setShouldThrowOnGetJob(boolean shouldThrowOnGetJob) {
    this.shouldThrowOnGetJob = shouldThrowOnGetJob;
  }

  /** Set true to throw an exception in the {@code markJobCompleted} method. */
  public void setShouldThrowOnMarkJobCompleted(boolean shouldThrowOnMarkJobCompleted) {
    this.shouldThrowOnMarkJobCompleted = shouldThrowOnMarkJobCompleted;
  }

  public void setShouldThrowOnReturnJobForRetry(boolean shouldThrowOnReturnJobForRetry) {
    this.shouldThrowOnReturnJobForRetry = shouldThrowOnReturnJobForRetry;
  }

  public void setShouldThrowOnAppendJobErrorMessage(boolean shouldThrowOnAppendJobErrorMessage) {
    this.shouldThrowOnAppendJobErrorMessage = shouldThrowOnAppendJobErrorMessage;
  }
}
