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

package com.google.aggregate.adtech.worker.jobclient;

import com.google.aggregate.adtech.worker.jobclient.JobHandlerModule.JobClientJobMaxNumAttemptsBinding;
import com.google.aggregate.adtech.worker.jobclient.JobHandlerModule.JobClientJobValidatorsBinding;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Guice module for binding the AWS job client functionality */
public final class JobValidatorModule extends AbstractModule {

  /** Provider for a list of job validators. */
  @Provides
  @JobClientJobValidatorsBinding
  public ImmutableList<JobValidator> provideValidators(
      JobValidatorCheckFields validatorCheckFields,
      JobValidatorCheckRetryLimit validatorCheckRetryLimit,
      JobValidatorCheckStatus validatorCheckStatus) {
    return ImmutableList.of(
        // Job validators perform high level checks on the received job
        // to drop any job that does not need to be processed by the worker.
        // Note that order of validators matter.
        validatorCheckFields, validatorCheckStatus, validatorCheckRetryLimit);
  }

  /** Provider for an instance of the {@code JobValidatorCheckRetryLimit} class. */
  @Provides
  public JobValidatorCheckRetryLimit provideJobValidatorCheckRetryLimit(
      @JobClientJobMaxNumAttemptsBinding int maxNumAttempts) {
    return new JobValidatorCheckRetryLimit(maxNumAttempts);
  }
}
