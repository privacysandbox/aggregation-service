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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.aggregate.adtech.worker.model.AggregatedFact;
import com.google.aggregate.adtech.worker.testing.AvroResultsFileReader;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/** Reads an Avro results file and writes a CSV of the data */
final class AvroResultsDeserializerRunner {
  AvroResultsFileReader reader;

  @Inject
  public AvroResultsDeserializerRunner(AvroResultsFileReader reader) {
    this.reader = reader;
  }

  public static void main(String[] cliArgs) throws IOException {
    AvroResultsDeserializerRunner runner =
        Guice.createInjector(new AbstractModule() {})
            .getInstance(AvroResultsDeserializerRunner.class);
    AvroResultsDeserializerRunnerArgs args = new AvroResultsDeserializerRunnerArgs();
    JCommander.newBuilder().addObject(args).build().parse(cliArgs);

    runner.runResultDeserialization(args);
  }

  private void runResultDeserialization(AvroResultsDeserializerRunnerArgs args) throws IOException {
    ImmutableList<AggregatedFact> writtenResults =
        reader.readAvroResultsFile(Paths.get(args.getResultsFilePath()));
    BufferedWriter writer = new BufferedWriter(new FileWriter(args.getOutputPath()));
    writer.write("bucket,metric" + "\n");
    for (AggregatedFact fact : writtenResults) {
      writer.write(formatOutput(fact, args.isOutputBucketAsHex()) + "\n");
    }
    writer.close();
  }

  private static String formatOutput(AggregatedFact fact, boolean outputAsHex) {
    String bucketString = fact.getBucket().toString();
    if (outputAsHex) {
      byte[] bucketBytes = NumericConversions.toUnsignedByteArray(fact.getBucket());
      bucketString = "0x" + BaseEncoding.base16().encode(bucketBytes);
    }
    return bucketString + "," + fact.getMetric();
  }

  static final class AvroResultsDeserializerRunnerArgs {

    @Parameter(names = "--results_file_path", required = true)
    private String resultsFilePath;

    @Parameter(names = "--output_path", required = true)
    private String outputPath;

    @Parameter(names = "--output_bucket_as_hex")
    private boolean outputBucketAsHex = false;

    public String getResultsFilePath() {
      return resultsFilePath;
    }

    public String getOutputPath() {
      return outputPath;
    }

    public boolean isOutputBucketAsHex() {
      return outputBucketAsHex;
    }
  }
}
