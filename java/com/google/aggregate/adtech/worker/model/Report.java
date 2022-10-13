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

import com.google.auto.value.AutoValue;

/**
 * Representation of a single report
 *
 * <p>List of aggregation facts, operation to perform, and other data for request handling.
 *
 * <p>Jackson JSON annotations are added for converting to/from CBOR with Jackson. CBOR uses the
 * same annotations as JSON.
 *
 * <p>Fields are intended to match the report format here:
 * https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregate-attribution-reports
 */
@AutoValue
public abstract class Report {

  public static Builder builder() {
    return new AutoValue_Report.Builder();
  }

  // The encrypted data to be aggregated
  public abstract Payload payload();

  // Plaintext information about the report
  public abstract SharedInfo sharedInfo();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPayload(Payload payload);

    public abstract Builder setSharedInfo(SharedInfo sharedInfo);

    public abstract Report build();
  }
}
