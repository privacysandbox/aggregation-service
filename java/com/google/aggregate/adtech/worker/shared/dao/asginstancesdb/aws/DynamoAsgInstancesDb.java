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

package com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.aggregate.protos.shared.backend.asginstance.AsgInstanceProto.AsgInstance;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.aws.model.DynamoAsgInstancesTable;
import com.google.aggregate.adtech.worker.shared.dao.asginstancesdb.common.AsgInstancesDao;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

/** DynamoDB implementation of the {@code AsgInstancesDao} interface. */
public class DynamoAsgInstancesDb implements AsgInstancesDao {

  private static final Logger logger = LoggerFactory.getLogger(DynamoAsgInstancesDb.class);

  private final DynamoDbEnhancedClient ddb;
  private final Provider<String> tableName;
  // Supplier for job metadata table name. Defined as supplier to delay
  // fetching table name from the parameter store to after Guice initialization.
  private final Supplier<DynamoDbTable<AsgInstance>> asgInstancesTable;
  private final Clock clock;
  private final int asgInstanceTtl;

  @Inject
  DynamoAsgInstancesDb(
      @AsgInstancesDbDynamoClient DynamoDbEnhancedClient ddb,
      @AsgInstancesDbDynamoTableName Provider<String> tableName,
      Clock clock,
      @AsgInstancesDbDynamoTtlDays int asgInstanceTtl) {
    this.ddb = ddb;
    this.tableName = tableName;
    this.clock = clock;
    this.asgInstanceTtl = asgInstanceTtl;
    asgInstancesTable =
        Suppliers.memoize(
            () -> {
              logger.info("Creating dynamo metadata for table:" + tableName.get());
              return ddb.table(tableName.get(), DynamoAsgInstancesTable.getDynamoDbTableSchema());
            });
  }

  @Override
  public Optional<AsgInstance> getAsgInstance(String instanceName) throws AsgInstanceDaoException {
    try {
      Key key = Key.builder().partitionValue(instanceName).build();
      AsgInstance asgInstance = asgInstancesTable.get().getItem(key);
      if (Objects.nonNull(asgInstance)) {
        return Optional.of(asgInstance);
      } else {
        return Optional.empty();
      }
    } catch (SdkException e) {
      logger.info("SDK exception getting from the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    } catch (Exception e) {
      logger.info("Unknown exception getting from the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    }
  }

  /** No-op not needed in AWS autoscaling implementation. */
  @Override
  public List<AsgInstance> getAsgInstancesByStatus(String status) {
    throw new UnsupportedOperationException("Getting AsgInstances by status is not supported.");
  }

  @Override
  public void upsertAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException {
    Instant ttl = clock.instant().plus(asgInstanceTtl, ChronoUnit.DAYS);
    asgInstance = asgInstance.toBuilder().setTtl(ttl.getEpochSecond()).build();

    try {
      asgInstancesTable.get().putItem(asgInstance);
    } catch (SdkException e) {
      logger.info("SDK exception upserting to the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    } catch (Exception e) {
      logger.info("Unknown exception upserting to the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    }
  }

  @Override
  public void updateAsgInstance(AsgInstance asgInstance) throws AsgInstanceDaoException {
    try {
      asgInstancesTable.get().putItem(asgInstance);
    } catch (SdkException e) {
      logger.info("SDK exception updating the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    } catch (Exception e) {
      logger.info("Unknown exception updating the AsgInstances table: ", e);
      throw new AsgInstanceDaoException(e);
    }
  }

  /** Annotation for the String of the table name to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface AsgInstancesDbDynamoClient {}

  /** Annotation for the String of the table name to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface AsgInstancesDbDynamoTableName {}

  /** Annotation for the int of the TTL days to use for the AsgInstances DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface AsgInstancesDbDynamoTtlDays {}
}
