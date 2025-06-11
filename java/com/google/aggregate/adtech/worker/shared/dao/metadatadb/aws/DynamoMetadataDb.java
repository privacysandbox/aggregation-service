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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.aws.model.DynamoMetadataTable;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import com.google.scp.shared.proto.ProtoUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/** DynamoDB implementation of the {@code JobMetadataDb} interface. */
public final class DynamoMetadataDb implements JobMetadataDb {

  private static final String JOB_KEY_DOES_NOT_EXIST_EXPRESSION = "attribute_not_exists(JobKey)";
  private static final String JOB_KEY_EXISTS_EXPRESSION = "attribute_exists(JobKey)";

  private final DynamoDbEnhancedClient ddb;
  private final Provider<String> tableName;
  // Supplier for job metadata table name. Defined as supplier to delay
  // fetching table name from the parameter store to after Guice initialization.
  private final Supplier<DynamoDbTable<JobMetadata>> jobMetadataTable;
  private final Clock clock;
  private final int jobMetadataTtl;
  private final Logger logger = Logger.getLogger("DynamoMetadataDb");

  /** Creates a new instance of the {@code DynamoMetadataDb} class. */
  @Inject
  DynamoMetadataDb(
      @MetadataDbDynamoClient DynamoDbEnhancedClient ddb,
      @MetadataDbDynamoTableName Provider<String> tableName,
      Clock clock,
      @MetadataDbDynamoTtlDays int jobMetadataTtl) {
    this.ddb = ddb;
    this.tableName = tableName;
    this.clock = clock;
    this.jobMetadataTtl = jobMetadataTtl;
    jobMetadataTable =
        Suppliers.memoize(
            () -> {
              Logger.getLogger(DynamoMetadataDb.class.getName())
                  .info("Creating dynamo metadata for table:" + tableName.get());
              return ddb.table(tableName.get(), DynamoMetadataTable.getDynamoDbTableSchema());
            });
  }

  @Override
  public Optional<JobMetadata> getJobMetadata(String jobKeyString) throws JobMetadataDbException {
    Key key = Key.builder().partitionValue(jobKeyString).build();
    try {
      JobMetadata jobMetadata = jobMetadataTable.get().getItem(key);
      if (Objects.nonNull(jobMetadata)) {
        return Optional.of(jobMetadata);
      } else {
        return Optional.empty();
      }
    } catch (SdkException e) {
      logger.log(Level.INFO, "SDK exception getting from JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    } catch (Exception e) {
      logger.log(Level.INFO, "Unknown exception getting from JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    }
  }

  @Override
  public void insertJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobKeyExistsException {
    if (jobMetadata.getRecordVersion() != 0) {
      throw new IllegalArgumentException(
          "JobMetadata.recordVersion should not be set when inserting metadata entries");
    }

    Instant ttl = clock.instant().plus(jobMetadataTtl, ChronoUnit.DAYS);
    logger.log(Level.INFO, "Set Ttl to " + ttl);
    jobMetadata = jobMetadata.toBuilder().setTtl(ttl.getEpochSecond()).build();
    logger.log(Level.INFO, "Persisting Ttl as " + jobMetadata.getTtl());
    PutItemEnhancedRequest<JobMetadata> putItemEnhancedRequest =
        PutItemEnhancedRequest.builder(JobMetadata.class)
            .item(jobMetadata)
            .conditionExpression(
                Expression.builder().expression(JOB_KEY_DOES_NOT_EXIST_EXPRESSION).build())
            .build();
    logger.log(Level.INFO, "Inserting '" + jobMetadata.getJobKey() + "' into JobMetadata table");
    try {
      jobMetadataTable.get().putItem(putItemEnhancedRequest);
      logger.log(Level.INFO, "Inserted '" + jobMetadata.getJobKey() + "' into JobMetadata table");
    } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
      throw new JobKeyExistsException(conditionalCheckFailedException);
    } catch (SdkException e) {
      logger.log(Level.INFO, "SDK exception inserting into JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    } catch (Exception e) {
      logger.log(Level.INFO, "Unknown exception inserting into JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    }
  }

  @Override
  public void updateJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobMetadataConflictException {
    try {
      jobMetadata =
          jobMetadata.toBuilder()
              .setRequestUpdatedAt(ProtoUtil.toProtoTimestamp(Instant.now(clock)))
              .build();
      PutItemEnhancedRequest<JobMetadata> putItemEnhancedRequest =
          PutItemEnhancedRequest.builder(JobMetadata.class)
              .item(jobMetadata)
              .conditionExpression(
                  Expression.builder().expression(JOB_KEY_EXISTS_EXPRESSION).build())
              .build();
      jobMetadataTable.get().putItem(putItemEnhancedRequest);
    } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
      throw new JobMetadataConflictException(conditionalCheckFailedException);
    } catch (SdkException e) {
      logger.log(Level.INFO, "SDK exception updating JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    } catch (Exception e) {
      logger.log(Level.INFO, "Unknown exception updating JobMetadata table: ", e);
      throw new JobMetadataDbException(e);
    }
  }

  /** Annotation for the {@code DynamoDbEnhancedClient} to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface MetadataDbDynamoClient {}

  /** Annotation for the String of the table name to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface MetadataDbDynamoTableName {}

  /** Annotation for the int of the TTL days to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface MetadataDbDynamoTtlDays {}
}
