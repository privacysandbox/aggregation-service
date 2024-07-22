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

package com.google.aggregate.tools.privacybudgetutil.aws;

import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.aggregate.tools.privacybudgetutil.aws.AwsPrivacyBudgetUnitExtractionModule.Client;
import com.google.aggregate.tools.privacybudgetutil.common.ExtractionUtils;
import com.google.aggregate.tools.privacybudgetutil.common.PrivacyBudgetUnitExtractionConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/** Extracts budget keys from avros in AWS */
final class AwsPrivacyBudgetUnitExtraction {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AwsPrivacyBudgetUnitExtraction.class);

  private final PrivacyBudgetUnitExtractionConfig config;
  private final S3Client client;
  private final PrivacyBudgetKeyGeneratorFactory generatorFactory;

  @Inject
  AwsPrivacyBudgetUnitExtraction(
      PrivacyBudgetUnitExtractionConfig config,
      PrivacyBudgetKeyGeneratorFactory generatorFactory,
      @Client S3Client client) {
    this.config = config;
    this.client = client;
    this.generatorFactory = generatorFactory;
  }

  public static void main(String[] args) throws Exception {
    PrivacyBudgetUnitExtractionConfig config =
        new PrivacyBudgetUnitExtractionConfig(
            PrivacyBudgetUnitExtractionConfig.CloudPlatform.AWS, args);
    if (config.printHelp()) {
      return;
    }
    Guice.createInjector(
            new AwsPrivacyBudgetUnitExtractionModule(),
            new PrivacyBudgetKeyGeneratorModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(PrivacyBudgetUnitExtractionConfig.class).toInstance(config);
              }
            })
        .getInstance(AwsPrivacyBudgetUnitExtraction.class)
        .run();
  }

  public void run() throws Exception {

    if (this.config.getFunction().equals("generate_keys")) {
      ListObjectsV2Iterable response = listObjects();
      if (!this.config.getSingleFile()) {
        for (ListObjectsV2Response page : response) {
          page.contents().stream()
              .filter(s -> s.key().endsWith(".avro"))
              .map(this::accept)
              .forEach(System.out::println);
        }
      } else {
        for (ListObjectsV2Response page : response) {
          String builder =
              page.contents().stream()
                  .filter(s -> s.key().endsWith(".avro"))
                  .map(this::accept)
                  .filter(s -> s != null)
                  .map(ExtractionUtils.KeyFile::body)
                  .collect(Collectors.joining(",", "[", "]"));
          LOGGER.info(builder);
        }
      }
    }

    if (this.config.getFunction().equals("write_keys")) {
      ListObjectsV2Iterable response = listObjects();
      if (!this.config.getSingleFile()) {
        for (ListObjectsV2Response page : response) {
          page.contents().stream()
              .filter(s -> s.key().endsWith(".avro"))
              .map(this::accept)
              .forEach(this::writeKeyFile);
        }
      } else {
        for (ListObjectsV2Response page : response) {
          String builder =
              page.contents().stream()
                  .filter(s -> s.key().endsWith(".avro"))
                  .map(this::accept)
                  .filter(s -> s != null)
                  .map(ExtractionUtils.KeyFile::body)
                  .collect(Collectors.joining(",", "[", "]"));
          String filename = this.config.getOutputPrefix();
          ExtractionUtils.KeyFile keyFile = ExtractionUtils.KeyFile.create(filename, builder);
          writeKeyFile(keyFile);
        }
      }
    }
  }

  private ListObjectsV2Iterable listObjects() {
    ListObjectsV2Request request =
        ListObjectsV2Request.builder()
            .bucket(this.config.getBucket())
            .prefix(this.config.getInputPrefix())
            .build();

    return client.listObjectsV2Paginator(request);
  }

  private ExtractionUtils.KeyFile readBucketObject(String key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(this.config.getBucket()).key(key).build();
    try {
      ResponseInputStream<GetObjectResponse> responseInputStream =
          client.getObject(getObjectRequest);

      InputStream stream = new ByteArrayInputStream(responseInputStream.readAllBytes());
      ExtractionUtils.KeyFile keyfile =
          ExtractionUtils.processAvro(
              stream, this.generatorFactory, key, this.config.getFilteringIds());
      return keyfile;
    } catch (IOException e) {
      throw new AssertionError("Failed to get credentials", e);
    }
  }

  /** Writes keys to destination */
  private void writeKeyFile(ExtractionUtils.KeyFile keyFile) {
    if (keyFile == null) {
      return;
    }
    String filename =
        String.format(
            "%s/%s_%s.json",
            this.config.getOutputPrefix(),
            keyFile.key(),
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM_dd_yy")));
    LOGGER.info(
        String.format(
            "Writing shared_ids from %s/%s to %s",
            this.config.getBucket(), this.config.getInputPrefix(), filename));
    if (keyFile != null) {
      PutObjectRequest putOb =
          PutObjectRequest.builder().bucket(this.config.getBucket()).key(filename).build();

      client.putObject(putOb, RequestBody.fromString(keyFile.body()));
    }
  }

  private ExtractionUtils.KeyFile accept(S3Object object) {
    return readBucketObject(object.key());
  }
}
