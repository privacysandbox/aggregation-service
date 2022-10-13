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

import com.google.aggregate.adtech.worker.model.DebugBucketAnnotation;
import com.google.auto.value.AutoValue;
import java.math.BigInteger;
import java.util.List;

/** Representations of a single Avro debug results record. */
@AutoValue
public abstract class AvroDebugResultsRecord {

  /** Returns debug results record with bucket, metric and unnoisedMetric in it. */
  public static AvroDebugResultsRecord create(
      BigInteger bucket,
      long metric,
      long unnoisedMetric,
      List<DebugBucketAnnotation> debugAnnotations) {
    return new AutoValue_AvroDebugResultsRecord(bucket, metric, unnoisedMetric, debugAnnotations);
  }

  public abstract BigInteger bucket();

  /** metric associated with the bucket */
  public abstract long metric();

  /** unnoised metric associated with the bucket */
  public abstract long unnoisedMetric();

  /** debug annotations associated with the bucket */
  public abstract List<DebugBucketAnnotation> debugAnnotations();
}
