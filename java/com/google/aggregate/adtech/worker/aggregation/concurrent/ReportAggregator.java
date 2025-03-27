/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.adtech.worker.aggregation.concurrent;

import static com.google.scp.operator.shared.model.BackendModelUtil.toJobKeyString;

import com.google.aggregate.adtech.worker.Annotations;
import com.google.aggregate.adtech.worker.ErrorSummaryAggregator;
import com.google.aggregate.adtech.worker.ReportDecrypterAndValidator;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.exceptions.AggregationJobProcessException;
import com.google.aggregate.adtech.worker.model.AvroRecordEncryptedReportConverter;
import com.google.aggregate.adtech.worker.model.DecryptionValidationResult;
import com.google.aggregate.adtech.worker.model.EncryptedReport;
import com.google.aggregate.protocol.avro.AvroReportsReaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.privacysandbox.otel.OTelConfiguration;
import com.google.privacysandbox.otel.Timer;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.jobclient.model.Job;
import com.google.scp.operator.protos.shared.backend.RequestInfoProto.RequestInfo;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for Aggregation Report Processing. */
final class ReportAggregator {

  private static final Logger logger = LoggerFactory.getLogger(ReportAggregator.class);
  private static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();
  private static final int NUM_READ_THREADS = NUM_CPUS;
  // Decryption is a CPU-bound operation so put more CPU resources here.
  private static final int NUM_PROCESS_THREADS = NUM_CPUS;
  // Buffer size for reading data on the same thread
  private final int MAX_REPORTS_READ_BUFFER_SIZE = 1000;
  // Buffer size for decrypting and aggregating data on the same thread
  private final int MAX_REPORTS_PROCESS_BUFFER_SIZE = 1000;

  private final Provider<ReportDecrypterAndValidator> reportDecrypterAndValidatorProvider;
  private final ListeningExecutorService blockingThreadPool;
  private final ListeningExecutorService nonBlockingThreadPool;
  private final AvroReportsReaderFactory readerFactory;
  private final AvroRecordEncryptedReportConverter encryptedReportConverter;
  private final OTelConfiguration oTelConfiguration;
  private final ReportReader reportReader;

  @Inject
  ReportAggregator(
      Provider<ReportDecrypterAndValidator> reportDecrypterAndValidatorProvider,
      @Annotations.BlockingThreadPool ListeningExecutorService blockingThreadPool,
      @Annotations.NonBlockingThreadPool ListeningExecutorService nonBlockingThreadPool,
      AvroReportsReaderFactory readerFactory,
      AvroRecordEncryptedReportConverter encryptedReportConverter,
      OTelConfiguration oTelConfiguration,
      ReportReader reportReader) {
    this.reportDecrypterAndValidatorProvider = reportDecrypterAndValidatorProvider;
    this.blockingThreadPool = blockingThreadPool;
    this.nonBlockingThreadPool = nonBlockingThreadPool;
    this.readerFactory = readerFactory;
    this.encryptedReportConverter = encryptedReportConverter;
    this.oTelConfiguration = oTelConfiguration;
    this.reportReader = reportReader;
  }

  /**
   * Processes reports, filtering and aggregating their contributions and collecting errors and
   * Privacy Budget Units.
   *
   * @param totalReportCount a counter that holds the report count as it processes.
   * @param job the current job.
   * @param aggregationEngine aggregates the contributions from the reports and stores the result.
   * @param errorAggregator collects the report errors when processing.
   */
  void processReports(
      AtomicLong totalReportCount,
      Job job,
      AggregationEngine aggregationEngine,
      ErrorSummaryAggregator errorAggregator)
      throws AggregationJobProcessException {
    RequestInfo requestInfo = job.requestInfo();
    ImmutableList<DataLocation> dataShards = reportReader.getInputReportsShards(requestInfo);
    // Initialize reportDecrypterAndValidator once per job here for all threads in the flowable
    // block below.
    ReportDecrypterAndValidator reportDecrypterAndValidator =
        reportDecrypterAndValidatorProvider.get();
    Flowable.fromStream(dataShards.stream())
        // This would open connections with data and max concurrency is NUM_READ_THREADS.
        .flatMap(
            dataLocation ->
                reportReader
                    .getEncryptedReports(dataLocation)
                    .subscribeOn(Schedulers.from(blockingThreadPool)),
            false,
            NUM_READ_THREADS,
            MAX_REPORTS_READ_BUFFER_SIZE)
        // Specify the number of reports are grouped into a list.
        .buffer(MAX_REPORTS_PROCESS_BUFFER_SIZE)
        .doOnNext(encryptedReports -> totalReportCount.addAndGet(encryptedReports.size()))
        .flatMap(
            encryptedReportList ->
                Flowable.just(encryptedReportList)
                    .subscribeOn(Schedulers.from(nonBlockingThreadPool))
                    .map(
                        encryptedReports ->
                            decryptAndAggregateReports(
                                encryptedReports,
                                job,
                                aggregationEngine,
                                errorAggregator,
                                reportDecrypterAndValidator)),
            NUM_PROCESS_THREADS)
        .buffer(NUM_PROCESS_THREADS) // Waits for all NUM_PROCESS_THREADS observables emitted by the
        // preceding flatMap operation to complete before continuing.
        // Effectively, this will execute the subsequent operations after
        // one complete pass of flatMap stage has finished.
        .takeUntil(
            unused -> {
              return errorAggregator.countsAboveThreshold();
            })
        .blockingSubscribe();
  }

  private Observable decryptAndAggregateReports(
      List<EncryptedReport> reports,
      Job job,
      AggregationEngine aggregationEngine,
      ErrorSummaryAggregator errorAggregator,
      ReportDecrypterAndValidator reportDecrypterAndValidator) {
    reports.forEach(
        report -> {
          DecryptionValidationResult result;
          try (Timer t =
              oTelConfiguration.createDebugTimerStarted(
                  "decryption_time_per_report", toJobKeyString(job.jobKey()))) {
            result = reportDecrypterAndValidator.decryptAndValidate(report, job);
          }
          if (result.report().isPresent()) {
            aggregationEngine.accept(result.report().get());
          } else {
            errorAggregator.add(result);
          }
        });
    return Observable.empty();
  }
}
