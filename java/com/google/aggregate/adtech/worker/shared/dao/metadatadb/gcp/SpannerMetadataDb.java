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

package com.google.aggregate.adtech.worker.shared.dao.metadatadb.gcp;

import static com.google.aggregate.adtech.worker.shared.model.BackendModelUtil.toJobKeyString;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import com.google.aggregate.protos.shared.backend.JobKeyProto.JobKey;
import com.google.aggregate.protos.shared.backend.JobStatusProto.JobStatus;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import com.google.aggregate.protos.shared.backend.ResultInfoProto.ResultInfo;
import com.google.aggregate.protos.shared.backend.metadatadb.JobMetadataProto.JobMetadata;
import com.google.aggregate.adtech.worker.shared.dao.metadatadb.common.JobMetadataDb;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spanner implementation of the {@code JobMetadataDb} */
public final class SpannerMetadataDb implements JobMetadataDb {

  private static final Logger logger = LoggerFactory.getLogger(SpannerMetadataDb.class);

  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();
  private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();

  /** Table name for the job metadata DB. */
  public static final String TABLE_NAME = "JobMetadata";

  private final DatabaseClient dbClient;
  private final Clock clock;

  /** Creates a new instance of the {@code SpannerMetadataDb} class. */
  @Inject
  SpannerMetadataDb(@JobMetadataDbClient DatabaseClient dbClient, Clock clock) {
    this.dbClient = dbClient;
    this.clock = clock;
  }

  @Override
  public Optional<JobMetadata> getJobMetadata(String jobKeyString) throws JobMetadataDbException {
    Optional<JobMetadata> jobMetadata = Optional.empty();

    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM "
                    + TABLE_NAME
                    + " WHERE "
                    + SpannerJobMetadataTableColumn.JOB_KEY_COLUMN.label
                    + " = @jobKeyString")
            .bind("jobKeyString")
            .to(jobKeyString)
            .build();
    logger.debug("executing spanner statement: " + statement);

    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      // Expecting only one row to be returned since searching on primary key
      if (resultSet.next()) {
        jobMetadata = Optional.of(convertResultSetToJobMetadata(resultSet));
      }
    } catch (SpannerException | InvalidProtocolBufferException e) {
      throw new JobMetadataDbException(e);
    }
    return jobMetadata;
  }

  @Override
  public void insertJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobKeyExistsException {
    if (jobMetadata.getRecordVersion() != 0) {
      throw new IllegalArgumentException(
          "JobMetadata.recordVersion should not be set when inserting metadata entries");
    }

    com.google.cloud.Timestamp ttl =
        com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
            TimeUnit.SECONDS.convert(jobMetadata.getTtl(), TimeUnit.SECONDS), 0);

    try {
      WriteBuilder insertBuilder =
          Mutation.newInsertBuilder(TABLE_NAME)
              .set(SpannerJobMetadataTableColumn.JOB_KEY_COLUMN.label)
              .to(jobMetadata.getJobKey().getJobRequestId())
              .set(SpannerJobMetadataTableColumn.JOB_STATUS_COLUMN.label)
              .to(jobMetadata.getJobStatus().toString())
              .set(SpannerJobMetadataTableColumn.SERVER_JOB_ID_COLUMN.label)
              .to(jobMetadata.getServerJobId())
              .set(SpannerJobMetadataTableColumn.REQUEST_INFO_COLUMN.label)
              .to(Value.json(JSON_PRINTER.print(jobMetadata.getRequestInfo())))
              .set(SpannerJobMetadataTableColumn.REQUEST_RECEIVED_AT_COLUMN.label)
              .to(com.google.cloud.Timestamp.fromProto(jobMetadata.getRequestReceivedAt()))
              .set(SpannerJobMetadataTableColumn.REQUEST_UPDATED_AT_COLUMN.label)
              .to(com.google.cloud.Timestamp.fromProto(jobMetadata.getRequestUpdatedAt()))
              .set(SpannerJobMetadataTableColumn.NUM_ATTEMPTS_COLUMN.label)
              .to(jobMetadata.getNumAttempts())
              .set(SpannerJobMetadataTableColumn.TTL.label)
              .to(ttl);

      if (jobMetadata.hasResultInfo()) {
        insertBuilder
            .set(SpannerJobMetadataTableColumn.RESULT_INFO_COLUMN.label)
            .to(Value.json(JSON_PRINTER.print(jobMetadata.getResultInfo())));
      }

      if (jobMetadata.hasRequestProcessingStartedAt()) {
        insertBuilder
            .set(SpannerJobMetadataTableColumn.REQUEST_PROCESSING_STARTED_AT.label)
            .to(com.google.cloud.Timestamp.fromProto(jobMetadata.getRequestProcessingStartedAt()));
      }

      ImmutableList<Mutation> inserts = ImmutableList.of(insertBuilder.build());
      logger.debug("executing spanner inserts: " + inserts);
      dbClient.write(inserts);
      logger.info(
          String.format("Wrote job '%s' to spanner job metadata db.", jobMetadata.getJobKey()));
    } catch (SpannerException e) {
      if (e.getErrorCode() == ErrorCode.ALREADY_EXISTS) {
        throw new JobKeyExistsException(e);
      } else {
        throw new JobMetadataDbException(e);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new JobMetadataDbException(e);
    }
  }

  @Override
  public void updateJobMetadata(JobMetadata jobMetadata)
      throws JobMetadataDbException, JobMetadataConflictException {
    try {
      com.google.cloud.Timestamp ttl =
          com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
              TimeUnit.SECONDS.convert(jobMetadata.getTtl(), TimeUnit.SECONDS), 0);

      dbClient
          .readWriteTransaction()
          .run(
              transaction -> {
                Timestamp lastUpdateTime =
                    transaction
                        .readRow(
                            TABLE_NAME,
                            Key.of(toJobKeyString(jobMetadata.getJobKey())),
                            List.of(SpannerJobMetadataTableColumn.REQUEST_UPDATED_AT_COLUMN.label))
                        .getTimestamp(SpannerJobMetadataTableColumn.REQUEST_UPDATED_AT_COLUMN.label)
                        .toProto();

                if (Timestamps.compare(jobMetadata.getRequestUpdatedAt(), lastUpdateTime) == 0) {

                  Instant now = Instant.now(clock);
                  WriteBuilder updateBuilder =
                      Mutation.newUpdateBuilder(TABLE_NAME)
                          .set(SpannerJobMetadataTableColumn.JOB_KEY_COLUMN.label)
                          .to(jobMetadata.getJobKey().getJobRequestId())
                          .set(SpannerJobMetadataTableColumn.JOB_STATUS_COLUMN.label)
                          .to(jobMetadata.getJobStatus().toString())
                          .set(SpannerJobMetadataTableColumn.SERVER_JOB_ID_COLUMN.label)
                          .to(jobMetadata.getServerJobId())
                          .set(SpannerJobMetadataTableColumn.REQUEST_INFO_COLUMN.label)
                          .to(Value.json(JSON_PRINTER.print(jobMetadata.getRequestInfo())))
                          .set(SpannerJobMetadataTableColumn.REQUEST_RECEIVED_AT_COLUMN.label)
                          .to(
                              com.google.cloud.Timestamp.fromProto(
                                  jobMetadata.getRequestReceivedAt()))
                          .set(SpannerJobMetadataTableColumn.REQUEST_UPDATED_AT_COLUMN.label)
                          .to(
                              com.google.cloud.Timestamp.fromProto(
                                  Timestamp.newBuilder()
                                      .setSeconds(now.getEpochSecond())
                                      .setNanos(now.getNano())
                                      .build()))
                          .set(SpannerJobMetadataTableColumn.NUM_ATTEMPTS_COLUMN.label)
                          .to(jobMetadata.getNumAttempts())
                          .set(SpannerJobMetadataTableColumn.TTL.label)
                          .to(ttl);

                  if (jobMetadata.hasResultInfo()) {
                    updateBuilder
                        .set(SpannerJobMetadataTableColumn.RESULT_INFO_COLUMN.label)
                        .to(Value.json(JSON_PRINTER.print(jobMetadata.getResultInfo())));
                  }

                  if (jobMetadata.hasRequestProcessingStartedAt()) {
                    updateBuilder
                        .set(SpannerJobMetadataTableColumn.REQUEST_PROCESSING_STARTED_AT.label)
                        .to(
                            com.google.cloud.Timestamp.fromProto(
                                jobMetadata.getRequestProcessingStartedAt()));
                  }

                  ImmutableList<Mutation> updates = ImmutableList.of(updateBuilder.build());
                  logger.debug("Buffering spanner updates: " + updates);
                  transaction.buffer(updates);
                  logger.info(
                      String.format(
                          "Updated job '%s' in spanner job metadata db.", jobMetadata.getJobKey()));
                  return null;
                } else {
                  throw new JobMetadataConflictException(
                      "Update time is not equal to last recorded DB update time");
                }
              });
    } catch (SpannerException e) {
      if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
        throw new JobMetadataConflictException(e);
      } else {
        throw new JobMetadataDbException(e);
      }
    }
  }

  private JobMetadata convertResultSetToJobMetadata(ResultSet resultSet)
      throws InvalidProtocolBufferException {
    JobKey jobKey =
        JobKey.newBuilder().setJobRequestId(resultSet.getString(SpannerJobMetadataTableColumn.JOB_KEY_COLUMN.label)).build();
    JobStatus jobStatus = JobStatus.valueOf(resultSet.getString(SpannerJobMetadataTableColumn.JOB_STATUS_COLUMN.label));
    String serverJobId = resultSet.getString(SpannerJobMetadataTableColumn.SERVER_JOB_ID_COLUMN.label);
    Timestamp requestReceivedAt =
        resultSet.getTimestamp(SpannerJobMetadataTableColumn.REQUEST_RECEIVED_AT_COLUMN.label).toProto();
    Timestamp requestUpdatedAt = resultSet.getTimestamp(SpannerJobMetadataTableColumn.REQUEST_UPDATED_AT_COLUMN.label).toProto();
    int numAttempts = (int) resultSet.getLong(SpannerJobMetadataTableColumn.NUM_ATTEMPTS_COLUMN.label);
    long ttl = resultSet.getTimestamp(SpannerJobMetadataTableColumn.TTL.label).getSeconds();

    JobMetadata.Builder jobMetadataBuilder =
        JobMetadata.newBuilder()
            .setJobKey(jobKey)
            .setServerJobId(serverJobId)
            .setRequestReceivedAt(requestReceivedAt)
            .setRequestUpdatedAt(requestUpdatedAt)
            .setNumAttempts(numAttempts)
            .setJobStatus(jobStatus)
            .setTtl(ttl);

    if (!resultSet.isNull(SpannerJobMetadataTableColumn.REQUEST_INFO_COLUMN.label)) { // REQUEST_INFO_COLUMN is nullable
      String serializedRequestInfo = resultSet.getJson(SpannerJobMetadataTableColumn.REQUEST_INFO_COLUMN.label);
      RequestInfo.Builder requestInfo = RequestInfo.newBuilder();
      JSON_PARSER.merge(serializedRequestInfo, requestInfo);
      jobMetadataBuilder.setRequestInfo(requestInfo);
    }

    if (!resultSet.isNull(SpannerJobMetadataTableColumn.RESULT_INFO_COLUMN.label)) { // RESULT_INFO_COLUMN is nullable
      String serializedResultInfo = resultSet.getJson(SpannerJobMetadataTableColumn.RESULT_INFO_COLUMN.label);
      ResultInfo.Builder resultInfo = ResultInfo.newBuilder();
      JSON_PARSER.merge(serializedResultInfo, resultInfo);
      jobMetadataBuilder.setResultInfo(resultInfo);
    }

    if (!resultSet.isNull(SpannerJobMetadataTableColumn.REQUEST_PROCESSING_STARTED_AT.label)) {
      Timestamp requestProcessingStartedAt =
          resultSet.getTimestamp(SpannerJobMetadataTableColumn.REQUEST_PROCESSING_STARTED_AT.label).toProto();
      jobMetadataBuilder.setRequestProcessingStartedAt(requestProcessingStartedAt);
    }

    return jobMetadataBuilder.build();
  }

  /** Column names for the Spanner job metadata table. */
  enum SpannerJobMetadataTableColumn {
    JOB_KEY_COLUMN("JobKey"),
    JOB_STATUS_COLUMN("JobStatus"),
    SERVER_JOB_ID_COLUMN("ServerJobId"),
    NUM_ATTEMPTS_COLUMN("NumAttempts"),
    REQUEST_INFO_COLUMN("RequestInfo"),
    RESULT_INFO_COLUMN("ResultInfo"),
    REQUEST_RECEIVED_AT_COLUMN("RequestReceivedAt"),
    REQUEST_UPDATED_AT_COLUMN("RequestUpdatedAt"),
    TTL("Ttl"),
    REQUEST_PROCESSING_STARTED_AT("RequestProcessingStartedAt");

    /** Value of a {@code SpannerJobMetadataTableColumn} constant. */
    public final String label;

    /** Constructor for setting {@code SpannerJobMetadataTableColumn} enum constants. */
    SpannerJobMetadataTableColumn(String label) {
      this.label = label;
    }
  }

  /** Annotation for the int of the TTL days to use for the metadata DB. */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface MetadataDbSpannerTtlDays {}
}
