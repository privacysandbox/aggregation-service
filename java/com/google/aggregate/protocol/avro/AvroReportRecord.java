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

package com.google.aggregate.protocol.avro;

import com.google.auto.value.AutoValue;
import com.google.common.io.ByteSource;

@AutoValue
public abstract class AvroReportRecord {

  public static AvroReportRecord create(ByteSource payload, String keyId, String sharedInfo) {
    return new AutoValue_AvroReportRecord(payload, keyId, sharedInfo);
  }

  public abstract ByteSource payload();

  // String identifier for key used for encryption.
  public abstract String keyId();

  /** Associated data that may be used for decryption. */
  public abstract String sharedInfo();
}
