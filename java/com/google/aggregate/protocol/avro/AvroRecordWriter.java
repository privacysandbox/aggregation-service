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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;

/**
 * Writer that writes reports to an Avro file following the defined schema.
 *
 * <p>The schema is provided by the schema supplier. For convenience, the writer object can be
 * created through the factory, which allows the supplier to be bound with dependency injection,
 * thus requiring only the input stream to be passed.
 */
public abstract class AvroRecordWriter<Record> implements AutoCloseable {

  private static final int RECORDS_PER_FLUSH = 10000;
  private static final int BLOCKING_QUEUE_CAPACITY = 100000;

  private final DataFileWriter<GenericRecord> avroWriter;
  private final OutputStream outStream;
  private final AvroSchemaSupplier schemaSupplier;

  /**
   * Creates a writer based on the given Avro writer and schema supplier (where Avro writer should
   * *NOT* be open, just initialized; check the Avro docs for details)
   */
  AvroRecordWriter(
      DataFileWriter<GenericRecord> avroWriter,
      OutputStream outStream,
      AvroSchemaSupplier schemaSupplier) {
    this.avroWriter = avroWriter;
    this.outStream = outStream;
    this.schemaSupplier = schemaSupplier;
  }

  /** Logic to construct an Avro {@code GenericRecord} from generic Java type {@code Record}. */
  public abstract GenericRecord serializeRecordToGeneric(Record record, Schema avroSchema)
      throws IOException;

  /** Writes out records with the given {@link AvroReportRecord} */
  public void writeRecords(ImmutableList<MetadataElement> metadata, ImmutableList<Record> records)
      throws IOException {
    Schema schema = schemaSupplier.get();

    metadata.forEach(meta -> avroWriter.setMeta(meta.key(), meta.value()));

    try (DataFileWriter<GenericRecord> streamWriter = avroWriter.create(schema, outStream)) {
      for (Record record : records) {
        GenericRecord genericAvroRecord = serializeRecordToGeneric(record, schema);
        streamWriter.append(genericAvroRecord);
      }

      streamWriter.flush();
      outStream.flush();
    }
  }

  public void writeRecordsToShards(
      ImmutableList<MetadataElement> metadata,
      Stream<Record> records,
      Path recordPath,
      int numRecords,
      int numShards,
      String shardFilePrefix)
      throws IOException, InterruptedException, ExecutionException {

    Schema schema = schemaSupplier.get();
    int coreCount = Runtime.getRuntime().availableProcessors();
    records = records.parallel().unordered();
    Stream<GenericRecord> genericRecords =
        records.map(
            record -> {
              try {
                return serializeRecordToGeneric(record, schema);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

    final LinkedBlockingQueue<GenericRecord> recordQueue =
        new LinkedBlockingQueue(BLOCKING_QUEUE_CAPACITY);
    Callable<Void> processRecords = getRecordEnqueueProcessor(genericRecords, recordQueue);

    ListeningExecutorService recordProcessorService =
        MoreExecutors.listeningDecorator(new ForkJoinPool(coreCount));
    ListenableFuture<Void> recordProcessorFuture = recordProcessorService.submit(processRecords);

    ListeningExecutorService writerService =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(coreCount));
    List<ListenableFuture> writerFutures = new ArrayList<>();
    int minRecordsPerShard = numRecords / numShards;
    int numShardsWithSpillover = numRecords % numShards;
    try {
      for (int currentShard = 0; currentShard < numShards; currentShard++) {
        int spilloverRecordsInCurrentShard = currentShard < numShardsWithSpillover ? 1 : 0;
        int recordsToWrite = minRecordsPerShard + spilloverRecordsInCurrentShard;
        Path shardPath =
            numShards > 1
                ? recordPath.resolve(shardFilePrefix + "_shard_" + currentShard + ".avro")
                : recordPath;

        Runnable shardWriter =
            createShardWriter(schema, metadata, shardPath, recordQueue, recordsToWrite);

        writerFutures.add(writerService.submit(shardWriter));
      }

      Futures.addCallback(
          recordProcessorFuture,
          new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {}

            @Override
            public void onFailure(Throwable throwable) {
              writerFutures.forEach(future -> future.cancel(/* mayInterruptIfRunning */ true));
            }
          },
          recordProcessorService);

    } finally {
      recordProcessorFuture.get();
      for (ListenableFuture future : writerFutures) {
        future.get();
      }
      recordProcessorService.shutdown();
      writerService.shutdown();
    }
  }

  /** Writes out records with the given {@link AvroReportRecord} */
  public void writeRecordsFromStream(
      ImmutableList<MetadataElement> metadata, Stream<Record> records) throws IOException {
    Schema schema = schemaSupplier.get();

    metadata.forEach(meta -> avroWriter.setMeta(meta.key(), meta.value()));

    try (DataFileWriter<GenericRecord> streamWriter = avroWriter.create(schema, outStream)) {
      AtomicInteger flushCounter = new AtomicInteger(0);
      records.forEach(
          record -> {
            try {
              GenericRecord genericAvroRecord = serializeRecordToGeneric(record, schema);
              streamWriter.append(genericAvroRecord);
              if (flushCounter.incrementAndGet() >= RECORDS_PER_FLUSH) {
                flushCounter.set(0);
                streamWriter.flush();
                outStream.flush();
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  /** Writes out @numRecords records with the given {@link AvroReportRecord} */
  public void writeRecordsFromSpliterator(
      ImmutableList<MetadataElement> metadata,
      Spliterator<Record> recordSpliterator,
      int numRecords)
      throws IOException {
    Schema schema = schemaSupplier.get();

    metadata.forEach(meta -> avroWriter.setMeta(meta.key(), meta.value()));

    try (DataFileWriter<GenericRecord> streamWriter = avroWriter.create(schema, outStream)) {
      for (int i = 0; i < numRecords; i++) {
        recordSpliterator.tryAdvance(
            record -> {
              GenericRecord genericAvroRecord = null;
              try {
                genericAvroRecord = serializeRecordToGeneric(record, schema);
                streamWriter.append(genericAvroRecord);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
      }

      streamWriter.flush();
      outStream.flush();
    }
  }

  private static Callable<Void> getRecordEnqueueProcessor(
      Stream<GenericRecord> genericRecords, LinkedBlockingQueue<GenericRecord> recordQueue) {
    return new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        genericRecords.forEach(
            record -> {
              try {
                recordQueue.put(record);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
        return null;
      }
    };
  }

  private static Runnable createShardWriter(
      Schema schema,
      ImmutableList<MetadataElement> metadata,
      Path shardPath,
      LinkedBlockingQueue<GenericRecord> recordQueue,
      int recordsToWrite) {
    return () -> {
      DataFileWriter<GenericRecord> avroWriter =
          new DataFileWriter<>(new GenericDatumWriter<>(schema));
      metadata.forEach(meta -> avroWriter.setMeta(meta.key(), meta.value()));

      try (OutputStream outputAvroStream =
              Files.newOutputStream(shardPath, CREATE, TRUNCATE_EXISTING);
          DataFileWriter<GenericRecord> streamWriter =
              avroWriter.create(schema, outputAvroStream)) {
        for (int record = 0; record < recordsToWrite; record++) {
          streamWriter.append(recordQueue.take());
          if (record % RECORDS_PER_FLUSH == 0) {
            streamWriter.flush();
          }
        }
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Override
  public void close() throws IOException {
    avroWriter.close();
  }

  /** One metadata element (key/value string pair) */
  @AutoValue
  public abstract static class MetadataElement {

    public static MetadataElement create(String key, String value) {
      return new AutoValue_AvroRecordWriter_MetadataElement(key, value);
    }

    public abstract String key();

    public abstract String value();
  }
}
