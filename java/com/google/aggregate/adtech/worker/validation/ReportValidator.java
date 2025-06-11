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

package com.google.aggregate.adtech.worker.validation;

import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import java.util.Optional;

/** Responsible for performing a single validation operation on a single report */
public interface ReportValidator {

  /**
   * Performs a single validation operation on a single report. The Optional<ErrorMessage> will be
   * present when a validation fails, if validation passes the Optional will be absent.
   */
  Optional<ErrorMessage> validate(Report report, Job ctx);
}
