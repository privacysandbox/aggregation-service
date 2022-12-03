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
import com.google.aggregate.adtech.worker.Annotations.BlockingThreadPool;
import com.google.aggregate.adtech.worker.Annotations.NonBlockingThreadPool;
import com.google.aggregate.adtech.worker.exceptions.DomainReadException;
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
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TextOutputDomainProcessorTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();
  @Rule public final Acai acai = new Acai(TestEnv.class);
  // Under test
  @Inject TextOutputDomainProcessor outputDomainProcessor;
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
    writeOutputDomain(singleFilePath, "11", "22", "33");
    // Set the location to be this single file, rather than its parent directory, to ensure that
    // this works when it's pointed to single files
    outputDomainLocation =
        DataLocation.ofBlobStoreDataLocation(
            BlobStoreDataLocation.create(
                /* bucket= */ singleFilePath.getParent().toAbsolutePath().toString(),
                /* key= */ singleFilePath.getFileName().toString()));

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
  }

  @Test
  public void readStringDomain() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), "foo", "bar");
    writeOutputDomain(outputDomainDirectory.resolve("domain_2.avro"), "baz");

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(
            createBucketFromString("foo"),
            createBucketFromString("bar"),
            createBucketFromString("baz"));
  }

  @Test
  public void deduplicate() throws Exception {
    writeOutputDomain(outputDomainDirectory.resolve("domain_1.avro"), "11", "22", "11", "11");
    writeOutputDomain(
        outputDomainDirectory.resolve("domain_2.avro"), "11", "22", "11", "11", "22", "33");

    ImmutableSet<BigInteger> keys = readOutputDomain();

    assertThat(keys)
        .containsExactly(createBucketFromInt(11), createBucketFromInt(22), createBucketFromInt(33));
  }

  @Test
  public void ioProblem() throws Exception {
    // No file written, path pointing to a non-existing file, this should be an IO exception.

    DomainReadException error = assertThrows(DomainReadException.class, () -> readOutputDomain());

    assertThat(error).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
  }

  private ImmutableSet<BigInteger> readOutputDomain()
      throws ExecutionException, InterruptedException {
    return outputDomainProcessor.readAndDedupDomain(outputDomainLocation).get();
  }

  private void writeOutputDomain(Path path, String... keys) throws IOException {
    Files.write(path, ImmutableList.copyOf(keys), US_ASCII, WRITE, CREATE);
  }

  private static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new FSBlobStorageClientModule());
      bind(FileSystem.class).toInstance(FileSystems.getDefault());
      bind(OutputDomainProcessor.class).to(TextOutputDomainProcessor.class);
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
