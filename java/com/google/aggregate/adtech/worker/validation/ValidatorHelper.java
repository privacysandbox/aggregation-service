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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import java.util.Optional;

/** Helper class for validator module */
public final class ValidatorHelper {

  /**
   * Returns true when provided inputString is not null or empty, else returns false.
   *
   * @param field
   * @return true/false
   */
  public static boolean isFieldNonEmpty(String field) {
    return !isNullOrEmpty(field);
  }

  /**
   * Returns true when provided input Optional String is not null or empty, else returns false.
   *
   * @param field
   * @return true/false
   */
  public static boolean isFieldNonEmpty(Optional<String> field) {
    return field != null && field.isPresent() && !isNullOrEmpty(field.get());
  }

  /**
   * Returns ErrorMessage from given ErrorCounter.
   *
   * @param errorCounter
   * @return
   */
  public static Optional<ErrorMessage> createErrorMessage(ErrorCounter errorCounter) {
    return Optional.of(ErrorMessage.builder().setCategory(errorCounter).build());
  }
}
