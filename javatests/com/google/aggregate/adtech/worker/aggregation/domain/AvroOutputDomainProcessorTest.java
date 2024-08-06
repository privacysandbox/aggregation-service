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

import static com.google.common.collect.ImmutableList.toImmutableList;
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
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.privacy.noise.Annotations.Threshold;
import com.google.aggregate.privacy.noise.NoiseApplier;
import com.google.aggregate.privacy.noise.NoisedAggregationRunner;
import com.google.aggregate.privacy.noise.NoisedAggregationRunnerImpl;
import com.google.aggregate.privacy.noise.model.NoisedAggregatedResultSet;
import com.google.aggregate.privacy.noise.proto.Params.NoiseParameters.Distribution;
import com.google.aggregate.privacy.noise.proto.Params.PrivacyParameters;
import com.google.aggregate.privacy.noise.testing.ConstantNoiseModule.ConstantNoiseApplier;
import com.google.aggregate.privacy.noise.testing.FakeNoiseApplierSupplier;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriter;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
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
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroOutputDomainProcessorTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  @Rule public final Acai acai = new Acai(TestEnv.class);
  @Inject AvroOutputDomainWriterFactory avroOutputDomainWriterFactory;
  // Under test
  @Inject AvroOutputDomainProcessor outputDomainProcessor;
  @Inject AggregationEngineFactory aggregationEngineFactory;
  @Inject FakeNoiseApplierSupplier fakeNoiseApplierSupplier;
  @Inject NoisedAggregationRunnerImpl noisedAggregationRunner;
  private Path outputDomainDirectory;
  private DataLocation outputDomainLocation;
  private AggregationEngine aggregationEngine;

  @Before
  public void setUp() throws Exception {
    outputDomainDirectory = testWorkingDir.getRoot().toPath().resolve("output_domain");
    Files.createDirectory(outputDomainDirectory);
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ outputDomainDirectory.toAbsolutePath().toString(), /* key= */ ""));
    aggregationEngine = aggregationEngineFactory.create(ImmutableSet.of());
    fakeNoiseApplierSupplier.setFakeNoiseApplier(new ConstantNoiseApplier(0));
  }

  @Test
  public void readDomain_singleFile() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.avro");
    writeOutputDomain(singleFilePath, Stream.of(11, 22, 33));
    // Set the location to be this single file, rather than its parent directory, to ensure that
    // this works when it's pointed to single file
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ singleFilePath.getParent().toAbsolutePath().toString(),
                /* key= */ singleFilePath.getFileName().toString()));

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(BigInteger.valueOf(11), BigInteger.valueOf(22), BigInteger.valueOf(33));
  }

  @Test
  public void readDomainStream() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.avro");
    writeOutputDomain(singleFilePath, Stream.of(11, 22, 33));
    try (InputStream avroInputStream = Files.newInputStream(singleFilePath)) {
      List<BigInteger> keys =
          outputDomainProcessor.readInputStream(avroInputStream).collect(Collectors.toList());

      assertThat(keys)
          .containsExactly(BigInteger.valueOf(11), BigInteger.valueOf(22), BigInteger.valueOf(33));
    }
  }

  @Test
  public void readDomainStream_emptyStream() throws Exception {
    Path singleFilePath = outputDomainDirectory.resolve("domain.avro");
    writeOutputDomain(singleFilePath, Stream.of());
    try (InputStream avroInputStream = Files.newInputStream(singleFilePath)) {
      List<BigInteger> keys =
          outputDomainProcessor.readInputStream(avroInputStream).collect(Collectors.toList());

      assertThat(keys).isEmpty();
    }
  }

  @Test
  public void readShardedDeduplicate() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), Stream.of(11, 22, 11, 11));
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.avro"), Stream.of(11, 22, 11, 11, 22, 33));

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(BigInteger.valueOf(11), BigInteger.valueOf(22), BigInteger.valueOf(33));
  }

  @Test
  public void skipsZeroByteDomains() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), Stream.of());
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.avro"), Stream.of(11, 22, 11, 11, 22, 33));

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(BigInteger.valueOf(11), BigInteger.valueOf(22), BigInteger.valueOf(33));
  }

  @Test
  public void ioProblem() {
    // No file written, path pointing to a non-existing file, this should be an IO exception.
    assertThrows(DomainReadException.class, this::readOutputDomain);
  }

  @Test
  public void readOutputDomain_emptyOutputDomain_throwsException() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), Stream.of());
    writeOutputDomain(outputDomainDirectory.resolve("domain_2.avro"), Stream.of());

    DomainReadException error = assertThrows(DomainReadException.class, this::readOutputDomain);

    assertThat(error).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    assertThat(error.getCause())
        .hasMessageThat()
        .containsMatch("No output domain provided in the location.*");
  }

  @Test
  public void readOutputDomain_notReadableOutputDomain_throwsException() throws Exception {
    writeOutputDomainTextFile(outputDomainDirectory.resolve("domain_1.avro"), "bad domain");

    assertThrows(DomainReadException.class, this::readOutputDomain);
  }

  private ImmutableSet<BigInteger> readOutputDomain() {
    NoisedAggregatedResultSet noisedResultset =
        outputDomainProcessor.adjustAggregationWithDomainAndNoiseStreaming(
            aggregationEngine,
            Optional.of(outputDomainLocation),
            outputDomainProcessor.listShards(outputDomainLocation),
            noisedAggregationRunner,
            Optional.empty(),
            false);

    return noisedResultset.noisedResult().noisedAggregatedFacts().stream()
        .map(AggregatedFact::getBucket)
        .collect(ImmutableSet.toImmutableSet());
  }

  private void writeOutputDomain(Path path, Stream<Integer> keys) throws IOException {
    AvroOutputDomainWriter writer =
        avroOutputDomainWriterFactory.create(Files.newOutputStream(path));
    ImmutableList<AvroOutputDomainRecord> records =
        keys.map(BigInteger::valueOf)
            .map(AvroOutputDomainRecord::create)
            .collect(toImmutableList());
    writer.writeRecords(ImmutableList.of(), records);
  }

  private void writeOutputDomainTextFile(Path outputDomainPath, String... keys) throws IOException {
    Files.write(outputDomainPath, ImmutableList.copyOf(keys), US_ASCII, WRITE, CREATE);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new FSBlobStorageClientModule());
      install(new PrivacyBudgetKeyGeneratorModule());

      bind(FileSystem.class).toInstance(FileSystems.getDefault());
      bind(OutputDomainProcessor.class).to(AvroOutputDomainProcessor.class);
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
    @Threshold
    Supplier<Double> provideThreshold() {
      return () -> 0.0;
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
    Ticker provideTimingTicker() {
      return Ticker.systemTicker();
    }
  }
}
