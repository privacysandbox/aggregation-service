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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OutputShardFileHelperTest {

  /**
   * setSingleShardFileSize with too small file size would not update
   * the single shard file size.
   */
  @Test
  public void testSetOutputShardFileSizeWithTooSmallOutputShardFileSize() {
    long tooSmallNumberForOutputShardFileSizeBytes =
        OutputShardFileHelper.getOneRecordFileSizeBytes() - 1;
    long outputShardFileSizeBytes = OutputShardFileHelper.getOutputShardFileSizeBytes();

    OutputShardFileHelper.setOutputShardFileSizeBytes(tooSmallNumberForOutputShardFileSizeBytes);

    assertThat(OutputShardFileHelper.getOutputShardFileSizeBytes())
        .isNotEqualTo(OutputShardFileHelper.getOneRecordFileSizeBytes()-1);
    assertThat(OutputShardFileHelper.getOutputShardFileSizeBytes())
        .isEqualTo(outputShardFileSizeBytes);
  }

  /**
   * setSingleShardFileSize with too large file size would not update
   * the single shard file size.
   */
  @Test
  public void testSetOutputShardFileSizeWithTooLargeOutputShardFileSize() {
    long tooLargeNumberForOutputShardFileSizeBytes = 10_000_000_000L; // 10GB
    long outputShardFileSizeBytes = OutputShardFileHelper.getOutputShardFileSizeBytes();

    OutputShardFileHelper.setOutputShardFileSizeBytes(tooLargeNumberForOutputShardFileSizeBytes);

    assertThat(OutputShardFileHelper.getOutputShardFileSizeBytes())
        .isNotEqualTo(tooLargeNumberForOutputShardFileSizeBytes);
    assertThat(OutputShardFileHelper.getOutputShardFileSizeBytes())
        .isEqualTo(outputShardFileSizeBytes);
  }

  /**
   * setSingleShardFileSize with valid file size would update the single shard file size.
   */
  @Test
  public void testSetSingleShardFileSizeWithValidSingleShardFileSize() {
    long oldOutputShardFileSizeBytes = OutputShardFileHelper.getOutputShardFileSizeBytes();

    long newOutputShardFileSizeBytes = 3 * OutputShardFileHelper.getOneRecordFileSizeBytes()
        + OutputShardFileHelper.getAvroMetadataSizeBytes();
    OutputShardFileHelper.setOutputShardFileSizeBytes(newOutputShardFileSizeBytes);
    long resultOutputShardFileSizeBytes = OutputShardFileHelper.getOutputShardFileSizeBytes();

    assertThat(resultOutputShardFileSizeBytes)
        .isNotEqualTo(oldOutputShardFileSizeBytes);
    assertThat(resultOutputShardFileSizeBytes)
        .isEqualTo(newOutputShardFileSizeBytes);
  }

  /**
   * getNumShard with smaller records count than the records count in a single shard
   * size would return 1 (single shard).
   */
  @Test
  public void testGetNumShardsForOneShard() {
    // Configure a single shard to have 5 records.
    OutputShardFileHelper.setOutputShardFileSizeBytes(
        5 * OutputShardFileHelper.getOneRecordFileSizeBytes()
            + OutputShardFileHelper.getAvroMetadataSizeBytes());
    int outputRecordCount = 3;
    int expectedNumShards = 1;

    int numShard = OutputShardFileHelper.getNumShards(outputRecordCount);

    assertThat(numShard).isEqualTo(expectedNumShards);
  }

  /**
   * getNumShard with large records count than the records count in a single shard
   * size would return more than 1 (multiple shards).
   */
  @Test
  public void testGetNumShardsForMultiShard() {
    // Configure a single shard to have 2 records.
    OutputShardFileHelper.setOutputShardFileSizeBytes(
        2 * OutputShardFileHelper.getOneRecordFileSizeBytes()
            + OutputShardFileHelper.getAvroMetadataSizeBytes());
    int outputRecordCount = 100;
    int expectedNumShards = 50;

    int numShards =
        OutputShardFileHelper.getNumShards(outputRecordCount);

    assertThat(numShards).isEqualTo(expectedNumShards);
  }

  /**
   * getNumShards with 0 record count would return 1 because 0 record
   * case would be dealt in a single shard process.
   */
  @Test
  public void testGetNumShardsWithZeroKeyValuePair() {
    int outputRecordCount = 0;
    int expectedNumShards = 1;

    int numShards = OutputShardFileHelper.getNumShards(outputRecordCount);

    assertThat(numShards).isEqualTo(expectedNumShards);
  }

  /**
   * Huge single shard file size is expected to yield 1 shard instead of 0.
   */
  @Test
  public void testGetNumShardsWithLargeSingleShardFileSize() {
    OutputShardFileHelper.setOutputShardFileSizeBytes(10_000_000_000L);

    int numShard = OutputShardFileHelper.getNumShards(1);

    assertThat(numShard).isEqualTo(1);
  }

  /**
   * getLastIndexOfShard returns (Shard ID + 1) * records count per shard
   * when shard Id + 1 < numShards
   */
  @Test
  public void testGetLastIndexOfShardBeforeLastShard() {
    int shardId = 2;
    int numShards = 4;
    int recordsCountPerShard = 5;
    int remainingRecordsCount = 3;
    int expectedResult = shardId * recordsCountPerShard;

    int lastIndex = OutputShardFileHelper.getEndIndexOfShard(
        shardId, numShards, recordsCountPerShard, remainingRecordsCount);

    assertThat(lastIndex).isEqualTo(expectedResult);
  }

  /**
   * getLastIndexOfShard returns (Shard ID + 1) * records count per shard
   * when shard Id == totalShards and remaining records count == 0
   */
  @Test
  public void testGetLastIndexOfShardForLastShardWithoutRemainingRecords() {
    int shardId = 3;
    int totalShards = 4;
    int recordsCountPerShard = 5;
    int remainingRecordsCount = 0;
    int expectedResult = shardId * recordsCountPerShard;

    int lastIndex = OutputShardFileHelper.getEndIndexOfShard(
        shardId, totalShards, recordsCountPerShard, remainingRecordsCount);

    assertThat(lastIndex).isEqualTo(expectedResult);
  }

  /**
   * getLastIndexOfShard returns (total Shard - 1) * records count per shard +
   * remaining records count when shard Id == totalShards and remaining records != 0
   */
  @Test
  public void testGetLastIndexOfShardForLastShardWithRemainingRecords() {
    int shardId = 4;
    int totalShards = 4;
    int recordsCountPerShard = 5;
    int remainingRecordsCount = 2;

    int expectedResult = (totalShards - 1) * recordsCountPerShard + remainingRecordsCount;

    int lastIndex = OutputShardFileHelper.getEndIndexOfShard(
        shardId, totalShards, recordsCountPerShard, remainingRecordsCount);

    assertThat(lastIndex).isEqualTo(expectedResult);
  }

  /**
   * getOutputPrefixWithShardNum returns [Prefix]-[ShardId]-of-[TotalShard] when
   * Prefix doesn't include AVRO extension.
   */
  @Test
  public void testGetOutputPrefixWithShardNumWithoutAvroExt() {
    int shardId = 3;
    int totalShards = 4;
    String prefix = "TestFile";
    String expectedResult = prefix + "-" + shardId + "-of-" + totalShards;

    String resultName =
        OutputShardFileHelper.getOutputFileNameWithShardInfo(prefix, shardId, totalShards);

    assertThat(resultName).isEqualTo(expectedResult);
  }

  /**
   * getOutputPrefixWithShardNum returns [Prefix without the extension]-1-of-1.avro
   * when Prefix includes AVRO extension.
   */
  @Test
  public void testGetOutputPrefixWithShardNumWithAvroExt() {
    final String AVRO_EXT = ".avro";
    int shardId = 1;
    int totalShards = 1;
    String prefix = "TestFile" ;
    String expectedResult = prefix + "-" + shardId + "-of-" + totalShards + AVRO_EXT;

    String resultName = OutputShardFileHelper.getOutputFileNameWithShardInfo(
        prefix + AVRO_EXT, shardId, totalShards);

    assertThat(resultName).isEqualTo(expectedResult);
  }

  /**
   * getOutputPrefixWithShardNum returns shard ID with "0" paddings for multi digit shards.
   */
  @Test
  public void testGetOutputPrefixWithShardNumWithMultiDigitNumShards() {
    int shardId = 15;
    int totalShards = 10000;
    String prefix = "TestFile";
    String expectedResult = prefix + "-000" + shardId + "-of-" + totalShards;

    String resultName =
        OutputShardFileHelper.getOutputFileNameWithShardInfo(prefix, shardId, totalShards);

    assertThat(resultName).isEqualTo(expectedResult);
  }
}
