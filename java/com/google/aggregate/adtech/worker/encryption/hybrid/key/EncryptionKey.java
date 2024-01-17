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

package com.google.aggregate.adtech.worker.encryption.hybrid.key;

import com.google.auto.value.AutoValue;
import com.google.crypto.tink.KeysetHandle;

/** AutoValue to hold the encryption key and id. */
@AutoValue
public abstract class EncryptionKey {
  public static Builder builder() {
    return new AutoValue_EncryptionKey.Builder();
  }

  /** Public {@link KeysetHandle} used for encryption */
  public abstract KeysetHandle key();

  /** Id to identify the encryption key. */
  public abstract String id();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setKey(KeysetHandle key);

    public abstract Builder setId(String id);

    public abstract EncryptionKey build();
  }
}
