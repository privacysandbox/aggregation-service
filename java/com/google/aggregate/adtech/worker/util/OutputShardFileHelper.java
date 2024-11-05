/*
 * Copyright 2023 Google LLC
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

package com.google.aggregate.adtech.worker.util;

/**
 * OutputShardFileHelper provides a set of utility functions to handle output shard writing.
 */
public final class OutputShardFileHelper {

  private static final String AVRO_EXTENSION = ".avro";

  // Avro file has some amount for metadata.
  private static final long AVRO_METADATA_SIZE_BYTES = 335L;
  private static final long RECORD_FILE_SIZE_BYTES = 20L;
  private static final long DEFAULT_OUTPUT_SHARD_FILE_SIZE_BYTES = 100_000_000L; // 100MB
  // Max single shard file size is 5GB based on AWS single PUT operation limit and GCS
  // maximum size of an individual part in a multipart upload.
  // AWS: https://docs.aws.amazon.com/AmazonS3/latest/userguide/upload-objects.html
  // GCS: https://cloud.google.com/storage/quotas
  private static final long MAX_SINGLE_SHARD_FILE_SIZE_BYTES = 5_000_000_000L; // 5GB
  private static long outputShardFileSizeBytes = DEFAULT_OUTPUT_SHARD_FILE_SIZE_BYTES;

  public static long getAvroMetadataSizeBytes() {
    return AVRO_METADATA_SIZE_BYTES;
  }

  public static long getOneRecordFileSizeBytes() {
    return RECORD_FILE_SIZE_BYTES;
  }

  /**
   * Set the file size for a shard in bytes.
   *
   * @param fileSize Output shard file size in bytes. It must be larger than one record size + AVRO
   *     metadata size and smaller than the maximum size(5GB) of a single shard file.
   */
  public static void setOutputShardFileSizeBytes(long fileSize) {
    if (fileSize >= RECORD_FILE_SIZE_BYTES + AVRO_METADATA_SIZE_BYTES
        && fileSize < MAX_SINGLE_SHARD_FILE_SIZE_BYTES) {
      outputShardFileSizeBytes = fileSize;
    }
  }

  public static long getOutputShardFileSizeBytes() {
    return outputShardFileSizeBytes;
  }

  /**
   * Returns the number of records per summary report shard to be written.
   *
   * @return The number of records per shard.
   */
  public static int getMaxRecordsPerShard() {
    return Double.valueOf(
            Math.max(
                Math.ceil(
                    ((outputShardFileSizeBytes - AVRO_METADATA_SIZE_BYTES)
                        / RECORD_FILE_SIZE_BYTES)),
                1))
        .intValue();
  }

  public static int getNumShards(long outputRecordCount) {
    return Double.valueOf(
            Math.max(
                Math.ceil(
                    outputRecordCount
                        * RECORD_FILE_SIZE_BYTES
                        / (outputShardFileSizeBytes - AVRO_METADATA_SIZE_BYTES)),
                1))
        .intValue();
  }

  /**
   * Return the exclusive end index of a shard.
   *
   * @param shardId 1-based index Shard ID
   * @param totalShards The total number of shards
   * @param recordsPerShard The number of records per shard
   * @param remainingRecordAtTheEnd The number of remaining records after distributing records
   *     evenly to each shard
   * @return The exclusive end index for the shard. In most cases, the end index would be Shard ID *
   *     the number of records per shard. When it's the last shard and there are remaining records
   *     after distributing records evenly to all shards except the last shard, the end index for
   *     the last shard would be the number of distributed records to all shards except the last
   *     shard + remaining records.
   */
  public static int getEndIndexOfShard(
      int shardId, long totalShards, long recordsPerShard, long remainingRecordAtTheEnd) {
    if (shardId < totalShards || remainingRecordAtTheEnd == 0) {
      return Long.valueOf(shardId * recordsPerShard).intValue();
    } else {
      return Long.valueOf(
          (totalShards - 1) * recordsPerShard + remainingRecordAtTheEnd).intValue();
    }
  }

  /**
   * Returns the output file name with a shard information suffix.
   *
   * @param prefix File prefix in cloud provided through a job parameter
   * @param shardId 1-based index Shard ID
   * @param totalShards The total number of shards
   * @return The output file name with the format [Prefix]-[ShardID]-of-[TotalShards]
   *
   * Note: The number of digits of [ShardId] is same as that in TotalShards.
   *       ex> ShardID: 15, TotalShards: 1000 -> Filename: Prefix-0015-of-1000
   */
  public static String getOutputFileNameWithShardInfo(String prefix, int shardId, int totalShards) {
    String shardIdFormat = "%0" + String.valueOf(totalShards).length() + "d";
    String shardSuffix = "-" + String.format(shardIdFormat,shardId) + "-of-" + totalShards;
    return prefix.endsWith(AVRO_EXTENSION)
        ? prefix.substring(
            0, prefix.length() - AVRO_EXTENSION.length()) + shardSuffix + AVRO_EXTENSION
        : prefix + shardSuffix;
  }
}
