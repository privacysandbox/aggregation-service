/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.adtech.worker.encryption.publickeyuri;

import com.google.auto.value.AutoValue;

/** Class for configuring encryption key config. */
@AutoValue
public abstract class CloudEncryptionKeyConfig {
  public static Builder builder() {
    return new AutoValue_CloudEncryptionKeyConfig.Builder();
  }

  public static final int NUM_ENCRYPTION_KEYS = 5;

  public abstract String keyVendingServiceUri();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setKeyVendingServiceUri(String value);

    public abstract CloudEncryptionKeyConfig build();
  }
}
