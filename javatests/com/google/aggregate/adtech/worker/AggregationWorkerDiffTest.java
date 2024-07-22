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

package com.google.aggregate.adtech.worker;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.testing.LocalAggregationWorkerRunner;
import com.google.aggregate.tools.diff.ResultDiffer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;
import com.google.inject.AbstractModule;
import com.google.privacysandbox.otel.OTelConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/*
 * Test aggregation worker generate the same results as local golden reports under specific
 * condition (noising_epsilon and noising_l1_sensitivity). This test use sharded domain and reports
 * as input.
 */
@RunWith(JUnit4.class)
public class AggregationWorkerDiffTest {

  @Rule public final TemporaryFolder testWorkingDir = new TemporaryFolder();

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private AvroResultsFileReader resultsFileReader;

  private Path reportPath;
  private Path domainPath;
  private Path key;
  private Path golden;
  private ImmutableList<AggregatedFact> goldenFacts;

  // The aggregator takes in all the files in the reportsDirectory for reports input and takes all
  // the files in domainDirectory for domain input.
  private Path reportsDirectory;
  private Path domainDirectory;

  @Before
  public void setUp() throws Exception {
    OTelConfiguration.resetForTest();
    reportsDirectory = testWorkingDir.getRoot().toPath().resolve("reports");
    domainDirectory = testWorkingDir.getRoot().toPath().resolve("domain");
    Files.createDirectory(reportsDirectory);
    Files.createDirectory(domainDirectory);
    reportPath = reportsDirectory.toAbsolutePath();
    domainPath = domainDirectory.toAbsolutePath();

    key = Paths.get("worker/testing/data/encryption_key.pb");
    golden = Paths.get("worker/testing/data/1k/diff_1k_output.golden");
    goldenFacts = resultsFileReader.readAvroResultsFile(golden);
    // Make sure we don't accidentally use empty golden.
    assertThat(goldenFacts).isNotEmpty();
  }

  /**
   * Copy the shards from the provided directory to the target directory for use with the concurrent
   * processor.
   *
   * <p>NOTE: This method assumes there are no subdirectories in the directory provided, only files.
   */
  public void copyShards(Path fromDir, Path toDir) throws Exception {
    Files.list(fromDir)
        .forEach(
            file -> {
              try {
                Files.copy(file, toDir.resolve(file.getFileName()));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  // To update the golden:
  // bazel build worker/testing/data/1k:diff_1k_cbor
  // bazel build worker/testing/data/1k/sharded:diff_1k_cbor
  // bazel build worker/testing/data/1k/sharded:diff_1k_cbor_domain
  // bazel run java/com/google/aggregate/tools/diff:DiffRunner -- \
  // --test_input $PWD/bazel-bin/worker/testing/data/1k/sharded/diff_1k_cbor_shards \
  // --test_output_domain_dir
  // $PWD/bazel-bin/worker/testing/data/1k/sharded/diff_1k_cbor_domain_shards \
  // --test_key $PWD/worker/testing/data/encryption_key.pb \
  // --test_golden $PWD/worker/testing/data/1k/diff_1k_output.golden \
  // --noising_epsilon 0.1 \
  // --noising_l1_sensitivity 4 \
  // --update_golden
  // Note: The domain generated in diff_1k_cbor_domain is fully overlapped with reports keys.

  @Test
  public void diffTestConstantNoising() throws Exception {
    copyShards(Paths.get("worker/testing/data/1k/sharded/diff_1k_cbor_shards/"), reportsDirectory);
    copyShards(
        Paths.get("worker/testing/data/1k/sharded/diff_1k_cbor_domain_shards/"), domainDirectory);

    MapDifference<BigInteger, AggregatedFact> diff = diff();

    assertWithMessage("Found diffs between left(test) and right(golden).\n" + diff.toString())
        .that(diff.areEqual())
        .isTrue();
  }

  // Default diff helper: Run aggregation on input file with default flags, then compare the result
  // with the given golden.
  private MapDifference<BigInteger, AggregatedFact> diff() throws Exception {
    ImmutableList<AggregatedFact> goldenFacts = resultsFileReader.readAvroResultsFile(golden);

    String[] args =
        new String[] {
          "--local_file_single_puller_path",
          reportPath.toString(),
          "--local_file_decryption_key_path",
          key.toString(),
          // Use noise parameters that match those provided when the golden output were generated
          "--noising_epsilon",
          "0.1",
          "--noising_l1_sensitivity",
          "4",
          "--local_output_domain_path",
          domainPath.toString(),
        };
    LocalAggregationWorkerRunner workerRunner =
        LocalAggregationWorkerRunner.create(testWorkingDir.getRoot().toPath(), args);
    workerRunner.run();
    ImmutableList<AggregatedFact> testFacts = workerRunner.waitForAggregation();

    // Create AggregatedFact with only two fields: bucket and metric
    Stream<AggregatedFact> testFactsTwoFields =
        testFacts.stream().map(fact -> AggregatedFact.create(fact.getBucket(), fact.getMetric()));
    return ResultDiffer.diffResults(testFactsTwoFields, goldenFacts.stream());
  }

  private static final class TestEnv extends AbstractModule {}
}
