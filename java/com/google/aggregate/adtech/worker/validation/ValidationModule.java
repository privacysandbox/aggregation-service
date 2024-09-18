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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice modules that adds validations to perform on {@code Report}.
 */
public final class ValidationModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<ReportValidator> validatorBinder =
        Multibinder.newSetBinder(binder(), ReportValidator.class);
    validatorBinder.addBinding().to(ReportingOriginMatchesRequestValidator.class);
    validatorBinder.addBinding().to(ReportNotTooOldValidator.class);
    validatorBinder.addBinding().to(SupportedOperationValidator.class);
    validatorBinder.addBinding().to(ReportForDebugValidator.class);
    validatorBinder.addBinding().to(SupportedReportApiTypeValidator.class);
    validatorBinder.addBinding().to(ReportPrivacyBudgetKeyValidator.class);
    validatorBinder.addBinding().to(SharedInfoReportIdValidator.class);
    validatorBinder.addBinding().to(ReportVersionValidator.class);
  }
}
