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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.aggregate.adtech.worker.decryption.DecryptionCipherFactory.CipherCreationException;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter;
import com.google.aggregate.adtech.worker.decryption.RecordDecrypter.DecryptionException;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.validation.ReportValidator;
import com.google.common.collect.ImmutableList;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;
import com.google.scp.operator.cpio.jobclient.model.Job;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decrypts, Deserializes, and Validates reports for aggregation. */
public final class ReportDecrypterAndValidator {

  private final RecordDecrypter recordDecrypter;
  private final Set<ReportValidator> reportValidators;

  private static final Logger logger = LoggerFactory.getLogger(ReportDecrypterAndValidator.class);

  /**
   * Dependencies should be injected.
   *
   * <p>Set<ReportValidator> is used so that Guice's Multibinder can be used to inject the
   * ReportValidator classes.
   */
  @Inject
  public ReportDecrypterAndValidator(
      RecordDecrypter recordDecrypter, Set<ReportValidator> reportValidators) {
    this.recordDecrypter = recordDecrypter;
    this.reportValidators = reportValidators;
  }

  /**
   * Decrypts, deserializes, and validates a report.
   *
   * <p>Performs decryption, deserialization, and validation. The result is a
   * DecryptionValidationResult which contains either the decrypted report or errors that came up in
   * decryption/validation which can be summarized and provided to requestors as debug information.
   */
  public DecryptionValidationResult decryptAndValidate(EncryptedReport encryptedReport, Job ctx) {
    try {
      // Decrypt the report
      Report report = recordDecrypter.decryptSingleReport(encryptedReport);

      // Perform validations
      ImmutableList<ErrorMessage> validationErrors =
          reportValidators.stream()
              .map(reportValidator -> reportValidator.validate(report, ctx))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(toImmutableList());

      if (validationErrors.isEmpty()) {
        return DecryptionValidationResult.builder().setReport(report).build();
      }

      return DecryptionValidationResult.builder().addAllErrorMessage(validationErrors).build();
    } catch (DecryptionException e) {
      logger.error("Report Decryption Failure", e);
      String detailedErrorMessage = String.format("Report Decryption Failure, cause: %s", e);
      ErrorMessage.Builder errorMessageBuilder = ErrorMessage.builder();

      // DecryptionKeyService Error
      if (e.getCause() instanceof CipherCreationException) {
        ErrorReason reason = ((CipherCreationException) e.getCause()).reason;
        errorMessageBuilder.setCategory(errorCounterFromCipherCreationException(reason));
      } else {
        errorMessageBuilder.setCategory(ErrorCounter.DECRYPTION_ERROR);
      }

      return DecryptionValidationResult.builder()
          .addErrorMessage(errorMessageBuilder.build())
          .build();
    }
  }

  private static ErrorCounter errorCounterFromCipherCreationException(ErrorReason reason) {
    switch (reason) {
      case KEY_DECRYPTION_ERROR:
        return ErrorCounter.DECRYPTION_KEY_FETCH_ERROR;
      case KEY_NOT_FOUND:
        return ErrorCounter.DECRYPTION_KEY_NOT_FOUND;
      case INTERNAL:
      default:
        return ErrorCounter.INTERNAL_ERROR;
    }
  }
}
