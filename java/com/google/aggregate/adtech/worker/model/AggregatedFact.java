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

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Single aggregated result (for one key) */
public class AggregatedFact {

  private final BigInteger bucket;

  private long metric;

  /** Set it optional because it is for debug result use. */
  private Optional<Long> unnoisedMetric;

  /** Set it optional because it is for debug result use only. */
  private Optional<List> debugAnnotations;

  private AggregatedFact(
      BigInteger bucket,
      long metric,
      Optional<Long> unnoisedMetric,
      Optional<List> debugBucketAnnotations) {
    this.bucket = bucket;
    this.metric = metric;
    this.unnoisedMetric = unnoisedMetric;
    this.debugAnnotations = debugBucketAnnotations;
  }

  public static AggregatedFact create(BigInteger bucket, long metric) {
    return new AggregatedFact(bucket, metric, Optional.empty(), Optional.empty());
  }

  public static AggregatedFact create(BigInteger bucket, long metric, Long unnoisedMetric) {
    return new AggregatedFact(bucket, metric, Optional.of(unnoisedMetric), Optional.empty());
  }

  public static AggregatedFact create(
      BigInteger bucket,
      long metric,
      Long unnoisedMetric,
      List<DebugBucketAnnotation> debugAnnotations) {
    return new AggregatedFact(
        bucket, metric, Optional.of(unnoisedMetric), Optional.of(debugAnnotations));
  }

  public BigInteger getBucket() {
    return bucket;
  }

  public long getMetric() {
    return metric;
  }

  public void setMetric(long metric) {
    this.metric = metric;
  }

  public Optional<Long> getUnnoisedMetric() {
    return unnoisedMetric;
  }

  public void setUnnoisedMetric(Optional<Long> unnoisedMetric) {
    this.unnoisedMetric = unnoisedMetric;
  }

  public Optional<List> getDebugAnnotations() {
    return debugAnnotations;
  }

  public void setDebugAnnotations(List<DebugBucketAnnotation> debugBucketAnnotations) {
    this.debugAnnotations = Optional.of(debugBucketAnnotations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.bucket.hashCode(),
        this.metric,
        this.unnoisedMetric.hashCode(),
        this.debugAnnotations.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AggregatedFact)) {
      return false;
    } else {
      AggregatedFact that = (AggregatedFact) o;
      return this.bucket.equals(that.getBucket())
          && this.metric == that.getMetric()
          && this.unnoisedMetric.equals(that.getUnnoisedMetric())
          && this.debugAnnotations.equals(that.getDebugAnnotations());
    }
  }
}
