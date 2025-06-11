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

package com.google.aggregate.adtech.worker.frontend.tasks.validation;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;

import com.google.common.base.CharMatcher;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;

/**
 * Validates that the jobRequestId only contains allowed characters. The allowed characters are
 * ascii letters, ascii digits, and ascii punctuation except for the | character.
 */
public final class JobRequestIdCharactersValidator implements RequestInfoValidator {

  // ASCII punctuation characters except for |. | isn't allowed since its the separator in the
  // {@code JobKey}
  private static final String ALLOWED_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{}~";
  private static final CharMatcher PUNCTUATION_MATCHER = anyOf(ALLOWED_PUNCTUATION);
  // ASCII Letters
  private static final CharMatcher LETTER_MATCHER = inRange('a', 'z').or(inRange('A', 'Z'));
  // ASCII Numbers
  private static final CharMatcher NUMBER_MATCHER = inRange('0', '9');
  // Matches characters in (punctuation OR letter OR digit)
  private static final CharMatcher MATCHER =
      PUNCTUATION_MATCHER.or(LETTER_MATCHER).or(NUMBER_MATCHER);

  @Override
  public Optional<String> validate(RequestInfo requestInfo) {
    if (MATCHER.matchesAllOf(requestInfo.getJobRequestId())) {
      return Optional.empty();
    }

    return Optional.of(errorMessage(requestInfo.getJobRequestId()));
  }

  private String errorMessage(String jobRequestId) {
    return String.format(
        "job_request_id contained illegal characters. Can only contain ascii letters, ascii"
            + " digits, or the following punctuation characters: %s. Illegal characters were: %s",
        ALLOWED_PUNCTUATION, MATCHER.removeFrom(jobRequestId));
  }
}
