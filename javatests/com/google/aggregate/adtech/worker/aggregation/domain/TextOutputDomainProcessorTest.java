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

package com.google.aggregate.adtech.worker.aggregation.domain;

import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromInt;
import static com.google.aggregate.adtech.worker.util.NumericConversions.createBucketFromString;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.acai.TestScoped;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.CustomForkJoinThreadPool;
import com.google.aggregate.adtech.worker.Annotations.DomainOptional;
import com.google.aggregate.adtech.worker.Annotations.EnableThresholding;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.ParallelAggregatedFactNoising;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngine;
import com.google.aggregate.adtech.worker.aggregation.engine.AggregationEngineFactory;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDelta;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingDistribution;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingEpsilon;
import com.google.aggregate.adtech.worker.configs.PrivacyParametersSupplier.NoisingL1Sensitivity;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.model.serdes.AvroResultsSerdes;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams;
import com.google.aggregate.privacy.noise.JobScopedPrivacyParams.LaplaceDpParams;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.NoisedAggregationRunnerImpl;
import com.google.aggregate.privacy.noise.ThresholdSupplier;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.model.SummaryReportAvroSet;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.privacy.noise.testing.FakeThresholdSupplier;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class TextOutputDomainProcessorTest {

  private static final JobScopedPrivacyParams DEFAULT_PRIVACY_PARAMS =
      JobScopedPrivacyParams.ofLaplace(
          LaplaceDpParams.builder().setEpsilon(10).setL1Sensitivity(1234).setDelta(0.01).build());

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  @Rule public final Acai acai = new Acai(TestEnv.class);
  // Under test
  @Inject TextOutputDomainProcessor outputDomainProcessor;
  @Inject AggregationEngineFactory aggregationEngineFactory;
  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject NoisedAggregationRunnerImpl noisedAggregationRunner;
  @Inject AvroResultsSerdes resultsSerdes;
  private AggregationEngine aggregationEngine;
  private Path outputDomainDirectory;
  private DataLocation outputDomainLocation;
  @TestParameter boolean streamingOutputDomainTestParam;

  @Before
  public void setUp() throws Exception {
    outputDomainDirectory = testWorkingDir.getRoot().toPath().resolve("output_domain");
    Files.createDirectory(outputDomainDirectory);
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ outputDomainDirectory.toAbsolutePath().toString(), /* key= */ ""));
    aggregationEngine =
        aggregationEngineFactory.createKeyAggregationEngine(ImmutableSet.of(UnsignedLong.ZERO));
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
  }

  @Test
  public void readDomain_singleFile() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.txt");
    writeOutputDomain(singleFilePath, "11", "22", "33");
    // Set the location to be this single file, rather than its parent directory, to ensure that
    // this works when it's pointed to single files
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ singleFilePath.getParent().toAbsolutePath().toString(),
                /* key= */ singleFilePath.getFileName().toString()));

    ImmutableSet<BigInteger> keys =
        streamingOutputDomainTestParam ? readOutputDomainStreaming() : readOutputDomain();

    assertThat(keys)
        .containsExactly(createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
  }

  @Test
  public void readDomainStream() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.txt");
    writeOutputDomain(singleFilePath, "11", "22", "33");

    try (InputStream textInputStream = Files.newInputStream(singleFilePath)) {
      List<BigInteger> keys =
          outputDomainProcessor.readInputStream(textInputStream).collect(Collectors.toList());

      assertThat(keys)
          .containsExactly(
              createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
    }
  }

  @Test
  public void readDomainStream_emptyStream() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.txt");
    writeOutputDomain(singleFilePath);
    try (InputStream textInputStream = Files.newInputStream(singleFilePath)) {

      List<BigInteger> keys =
          outputDomainProcessor.readInputStream(textInputStream).collect(Collectors.toList());

      assertThat(keys).isEmpty();
    }
  }

  @Test
  public void readStringDomain() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.txt"), "foo", "bar");
    writeOutputDomain(outputDomainDirectory.resolve("domain_2.txt"), "baz");

    ImmutableSet<BigInteger> keys =
        streamingOutputDomainTestParam ? readOutputDomainStreaming() : readOutputDomain();

    assertThat(keys)
        .containsExactly(
            createBucketFromString("foo"),
            createBucketFromString("bar"),
            createBucketFromString("baz"));
  }

  @Test
  public void deduplicate() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.txt"), "11", "22", "11", "11");
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.txt"), "11", "22", "11", "11", "22", "33");

    ImmutableSet<BigInteger> keys =
        streamingOutputDomainTestParam ? readOutputDomainStreaming() : readOutputDomain();

    assertThat(keys)
        .containsExactly(createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
  }

  @Test
  public void skipZeroByteDomains() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.txt"));
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.txt"), "11", "22", "11", "11", "22", "33");

    ImmutableSet<BigInteger> keys =
        streamingOutputDomainTestParam ? readOutputDomainStreaming() : readOutputDomain();

    assertThat(keys)
        .containsExactly(createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
  }

  @Test
  public void ioProblem() {
    // No file written, path pointing to a non-existing file, this should be an IO exception.
    assertThrows(
        DomainReadException.class,
        streamingOutputDomainTestParam ? this::readOutputDomainStreaming : this::readOutputDomain);
  }

  @Test
  public void readDomain_notReadableTextFile() throws Exception {
    String badString = "abcdabcdabcdabcdabcdabcdabcdabcd";
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.txt"), badString);

    assertThrows(
        DomainReadException.class,
        streamingOutputDomainTestParam ? this::readOutputDomainStreaming : this::readOutputDomain);
  }

  private ImmutableSet<BigInteger> readOutputDomainStreaming() {
    SummaryReportAvroSet summaryReportAvroSet =
        outputDomainProcessor
            .adjustAggregationWithDomainAndNoiseStreaming(
                aggregationEngine,
                Optional.of(outputDomainLocation),
                outputDomainProcessor.listShards(outputDomainLocation),
                noisedAggregationRunner,
                DEFAULT_PRIVACY_PARAMS,
                /* debugRun= */ false)
            .summaryReportAvroSet()
            .get();

    return summaryReportAvroSet.summaryReports().stream()
        .flatMap(
            summaryReportAvro ->
                resultsSerdes.reverse().convert(summaryReportAvro.reportBytes()).stream())
        .map(AggregatedFact::getBucket)
        .collect(ImmutableSet.toImmutableSet());
  }

  private ImmutableSet<BigInteger> readOutputDomain() {
    NoisedAggregatedResultSet noisedResultset =
        outputDomainProcessor
            .adjustAggregationWithDomainAndNoise(
                aggregationEngine,
                Optional.of(outputDomainLocation),
                outputDomainProcessor.listShards(outputDomainLocation),
                noisedAggregationRunner,
                DEFAULT_PRIVACY_PARAMS,
                /* debugRun= */ false)
            .noisedAggregatedResultSet()
            .get();

    return noisedResultset.noisedResult().noisedAggregatedFacts().stream()
        .map(AggregatedFact::getBucket)
        .collect(ImmutableSet.toImmutableSet());
  }

  private void writeOutputDomain(Path path, String... keys) throws IOException {
    Files.write(path, ImmutableList.copyOf(keys), US_ASCII, WRITE, CREATE);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new FSBlobStorageClientModule());
      install(new PrivacyBudgetKeyGeneratorModule());

      bind(FileSystem.class).toInstance(FileSystems.getDefault());
      bind(OutputDomainProcessor.class).to(TextOutputDomainProcessor.class);
      bind(Boolean.class).annotatedWith(DomainOptional.class).toInstance(true);
      bind(Boolean.class).annotatedWith(EnableThresholding.class).toInstance(true);

      bind(FakeNoiseApplierSupplier.class).in(TestScoped.class);
      bind(NoisedAggregationRunner.class).to(NoisedAggregationRunnerImpl.class);
      bind(boolean.class).annotatedWith(ParallelAggregatedFactNoising.class).toInstance(true);
      bind(Distribution.class)
          .annotatedWith(NoisingDistribution.class)
          .toInstance(Distribution.LAPLACE);
      bind(double.class).annotatedWith(NoisingEpsilon.class).toInstance(0.1);
      bind(long.class).annotatedWith(NoisingL1Sensitivity.class).toInstance(4L);
      bind(double.class).annotatedWith(NoisingDelta.class).toInstance(5.00);
    }

    @Provides
    ThresholdSupplier provideThreshold() {
      return new FakeThresholdSupplier(0.0);
    }

    @Provides
    Supplier<NoiseApplier> provideNoiseApplierSupplier(
        FakeNoiseApplierSupplier fakeNoiseApplierSupplier) {
      return fakeNoiseApplierSupplier;
    }

    @Provides
    @Singleton
    @CustomForkJoinThreadPool
    ListeningExecutorService provideCustomForkJoinThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    Supplier<PrivacyParameters> providePrivacyParamConfig(PrivacyParametersSupplier supplier) {
      return () -> supplier.get().toBuilder().setDelta(1e-5).build();
    }

    @Provides
    @NonBlockingThreadPool
    ListeningExecutorService provideNonBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    @BlockingThreadPool
    ListeningExecutorService provideBlockingThreadPool() {
      return newDirectExecutorService();
    }

    @Provides
    Ticker provideTimingTicker() {
      return Ticker.systemTicker();
    }
  }
}
