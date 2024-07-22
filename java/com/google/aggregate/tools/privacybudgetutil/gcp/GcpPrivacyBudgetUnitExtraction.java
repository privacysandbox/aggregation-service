/*
 * Copyright 2024 Google LLC
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

package com.google.aggregate.tools.privacybudgetutil.gcp;

import static com.google.cloud.storage.Storage.BlobListOption.*;

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.tools.privacybudgetutil.common.ExtractionUtils;
import com.google.aggregate.tools.privacybudgetutil.common.PrivacyBudgetUnitExtractionConfig;
import com.google.aggregate.tools.privacybudgetutil.gcp.GcpPrivacyBudgetUnitExtractionModule.StorageClient;
import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts privacy budget units from avros in GCP. */
final class GcpPrivacyBudgetUnitExtraction {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(GcpPrivacyBudgetUnitExtraction.class);

  private final PrivacyBudgetUnitExtractionConfig config;
  private final Storage storage;
  private final PrivacyBudgetKeyGeneratorFactory generatorFactory;
  private final List<ExtractionUtils.KeyFile> keys;

  @Inject
  GcpPrivacyBudgetUnitExtraction(
      PrivacyBudgetUnitExtractionConfig config,
      PrivacyBudgetKeyGeneratorFactory generatorFactory,
      @StorageClient Storage storage) {
    this.config = config;
    this.storage = storage;
    this.generatorFactory = generatorFactory;
    this.keys = new ArrayList<ExtractionUtils.KeyFile>();
  }

  public static void main(String[] args) throws Exception {
    PrivacyBudgetUnitExtractionConfig config =
        new PrivacyBudgetUnitExtractionConfig(
            PrivacyBudgetUnitExtractionConfig.CloudPlatform.GCP, args);
    if (config.printHelp()) {
      return;
    }
    Guice.createInjector(
            new GcpPrivacyBudgetUnitExtractionModule(),
            new PrivacyBudgetKeyGeneratorModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(PrivacyBudgetUnitExtractionConfig.class).toInstance(config);
              }
            })
        .getInstance(GcpPrivacyBudgetUnitExtraction.class)
        .run();
  }

  public void run() throws Exception {

    if (this.config.getFunction().equals("generate_keys")) {

      Page<Blob> blobs =
          storage.list(this.config.getBucket(), prefix(this.config.getInputPrefix()));
      if (!this.config.getSingleFile()) {
        blobs
            .streamAll()
            .filter(blob -> blob.getName().endsWith(".avro"))
            .map(this::accept)
            .forEach(System.out::println);
      } else {
        String builder =
            blobs
                .streamAll()
                .filter(blob -> blob.getName().endsWith(".avro"))
                .map(this::accept)
                .filter(s -> s != null)
                .map(ExtractionUtils.KeyFile::body)
                .collect(Collectors.joining(",", "[", "]"));
        LOGGER.info(builder);
      }
    }

    if (this.config.getFunction().equals("write_keys")) {
      Page<Blob> blobs =
          storage.list(this.config.getBucket(), prefix(this.config.getInputPrefix()));
      if (!this.config.getSingleFile()) {
        blobs
            .streamAll()
            .filter(blob -> blob.getName().endsWith(".avro"))
            .map(this::accept)
            .forEach(this::writeKeyFile);
      } else {
        String builder =
            blobs
                .streamAll()
                .filter(blob -> blob.getName().endsWith(".avro"))
                .map(this::accept)
                .filter(s -> s != null)
                .map(ExtractionUtils.KeyFile::body)
                .collect(Collectors.joining(",", "[", "]"));
        String filename = storage.getOptions().getProjectId();
        ExtractionUtils.KeyFile keyFile = ExtractionUtils.KeyFile.create(filename, builder);
        writeKeyFile(keyFile);
      }
    }
  }

  private ExtractionUtils.KeyFile readBucketObject(BlobId blobId, String key) {
    ReadChannel reader = storage.reader(blobId);
    InputStream inputStream = Channels.newInputStream(reader);
    try {
      return ExtractionUtils.processAvro(
          inputStream, this.generatorFactory, key, this.config.getFilteringIds());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Writes keys to destination */
  private void writeKeyFile(ExtractionUtils.KeyFile keyFile) {
    String filename =
        String.format(
            "%s/%s_%s.json",
            this.config.getOutputPrefix(),
            keyFile.key(),
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM_dd_yy")));
    LOGGER.info(
        String.format(
            "writing %s/%s to %s", config.getBucket(), config.getInputPrefix(), filename));
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    if (!this.config.getDryRun()) {
      BlobId blobId = BlobId.of(this.config.getBucket(), filename);
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
      try {
        stream.write(keyFile.body().getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      try (WriteChannel writer = storage.writer(blobInfo)) {
        writer.write(ByteBuffer.wrap(stream.toByteArray()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private ExtractionUtils.KeyFile accept(Blob blob) {
    return readBucketObject(blob.getBlobId(), blob.getName());
  }
}
