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

package com.google.aggregate.adtech.worker.frontend.service.aws;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.aws.model.DdbStreamBatchInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.converter.AttributeValueMapToJobMetadataConverter;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsRequest;
import software.amazon.awssdk.services.dynamodb.model.GetRecordsResponse;
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.dynamodb.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.dynamodb.model.ShardIteratorType;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;

/** Retrieves JobMetadata entries from a DynamoDb Stream. */
public final class DdbStreamJobMetadataLookup {

  private final DynamoDbStreamsClient dynamoDbStreamsClient;
  private final AttributeValueMapToJobMetadataConverter attributeValueMapConverter;

  /** Creates a new instance of the {@code DdbStreamJobMetadataLookup} class. */
  @Inject
  DdbStreamJobMetadataLookup(
      DynamoDbStreamsClient dynamoDbStreamsClient,
      AttributeValueMapToJobMetadataConverter attributeValueMapConverter) {
    this.dynamoDbStreamsClient = dynamoDbStreamsClient;
    this.attributeValueMapConverter = attributeValueMapConverter;
  }

  /**
   * Retrieves records from the stream identified by the {@link DdbStreamBatchInfo} provided.
   *
   * <p>Unchecked exceptions are thrown if the AWS API call fails. See the docs for {@link
   * DynamoDbStreamsClient#getShardIterator} and {@link DynamoDbStreamsClient#getRecords} for
   * detailed explanations of exceptions.
   */
  public ImmutableList<JobMetadata> lookupInStream(DdbStreamBatchInfo ddbStreamBatchInfo) {
    GetShardIteratorResponse getShardIteratorResponse =
        dynamoDbStreamsClient.getShardIterator(
            GetShardIteratorRequest.builder()
                .shardId(ddbStreamBatchInfo.shardId())
                .streamArn(ddbStreamBatchInfo.streamArn())
                .shardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
                .sequenceNumber(ddbStreamBatchInfo.startSequenceNumber())
                .build());

    GetRecordsResponse getRecordsResponse =
        dynamoDbStreamsClient.getRecords(
            GetRecordsRequest.builder()
                .shardIterator(getShardIteratorResponse.shardIterator())
                .limit(ddbStreamBatchInfo.batchSize())
                .build());

    return getRecordsResponse.records().stream()
        .map(record -> record.dynamodb().newImage())
        .map(attributeValueMapConverter)
        .collect(toImmutableList());
  }
}
