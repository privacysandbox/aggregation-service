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
import static org.junit.Assert.assertThrows;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
import com.google.aggregate.protocol.avro.AvroOutputDomainRecord;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriter;
import com.google.aggregate.protocol.avro.AvroOutputDomainWriterFactory;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation;
import com.google.scp.operator.cpio.blobstorageclient.model.DataLocation.BlobStoreDataLocation;
import com.google.scp.operator.cpio.blobstorageclient.testing.FSBlobStorageClientModule;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
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
  private Path outputDomainDirectory;
  private DataLocation outputDomainLocation;

  @Before
  public void setUp() throws Exception {
    outputDomainDirectory = testWorkingDir.getRoot().toPath().resolve("output_domain");
    Files.createDirectory(outputDomainDirectory);
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ outputDomainDirectory.toAbsolutePath().toString(), /* key= */ ""));
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
  public void readShardedDeduplicate() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), Stream.of(11, 22, 11, 11));
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.avro"), Stream.of(11, 22, 11, 11, 22, 33));

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(BigInteger.valueOf(11), BigInteger.valueOf(22), BigInteger.valueOf(33));
  }

  @Test
  public void ioProblem() {
    // No file written, path pointing to a non-existing file, this should be an IO exception.

    DomainReadException error = assertThrows(DomainReadException.class, this::readOutputDomain);

    assertThat(error).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void readOutputDomain_emptyOutputDomain_throwsException() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), Stream.of());
    writeOutputDomain(outputDomainDirectory.resolve("domain_2.avro"), Stream.of());

    ExecutionException error = assertThrows(ExecutionException.class, this::readOutputDomain);

    assertThat(error).hasCauseThat().isInstanceOf(DomainReadException.class);
    assertThat(error.getCause()).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    assertThat(error.getCause())
        .hasMessageThat()
        .containsMatch("No output domain provided in the location.*");
  }

  private ImmutableSet<BigInteger> readOutputDomain()
      throws ExecutionException, InterruptedException {
    return outputDomainProcessor.readAndDedupDomain(outputDomainLocation).get();
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

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new FSBlobStorageClientModule());
      bind(FileSystem.class).toInstance(FileSystems.getDefault());
      bind(OutputDomainProcessor.class).to(AvroOutputDomainProcessor.class);
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
