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

package com.google.aggregate.adtech.worker.model;

import com.google.scp.operator.protos.shared.backend.ErrorSummaryProto.ErrorSummary;

/**
 * Describes a category of error message, used to distinguish between processing or validation
 * failures reported by the {@link ErrorSummary} class.
 */
public enum ErrorCounter {
  ATTRIBUTION_REPORT_TO_MISMATCH,
  ATTRIBUTION_REPORT_TO_MALFORMED,
  ORIGINAL_REPORT_TIME_MISMATCH,
  ORIGINAL_REPORT_TIME_TOO_OLD,
  NUM_REPORTS_WITH_ERRORS,
  NUM_REPORTS_DEBUG_NOT_ENABLED
}
