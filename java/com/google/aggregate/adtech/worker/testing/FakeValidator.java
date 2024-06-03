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

import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/** Simple fake ReportValidator used to test that failed validations are handled correctly */
public final class FakeValidator implements ReportValidator {

  private Optional<Set<String>> reportIdShouldReturnError = Optional.empty();
  private Optional<Iterator<Boolean>> nextShouldReturnError = Optional.empty();

  public void setNextShouldReturnError(Iterator<Boolean> nextShouldReturnError) {
    this.nextShouldReturnError = Optional.of(nextShouldReturnError);
  }

  public void setReportIdShouldReturnError(Set<String> reportIdShouldReturnError) {
    this.reportIdShouldReturnError = Optional.of(reportIdShouldReturnError);
  }

  @Override
  public Optional<ErrorMessage> validate(Report report, Job unused) {

    if (this.reportIdShouldReturnError.isPresent() && report.sharedInfo().reportId().isPresent()) {
      if (this.reportIdShouldReturnError.get().contains(report.sharedInfo().reportId().get())) {
        return Optional.of(
            ErrorMessage.builder().setCategory(ErrorCounter.DECRYPTION_ERROR).build());
      }
    }
    if (this.nextShouldReturnError.isPresent()) {
      if (this.nextShouldReturnError.get().next()) {
        return Optional.of(
            ErrorMessage.builder().setCategory(ErrorCounter.DECRYPTION_ERROR).build());
      }
    }
    return Optional.empty();
  }
}
