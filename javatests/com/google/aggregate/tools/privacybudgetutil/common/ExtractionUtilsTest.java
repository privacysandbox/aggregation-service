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

package com.google.aggregate.tools.privacybudgetutil.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.acai.Acai;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExtractionUtilsTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private PrivacyBudgetKeyGeneratorFactory privacyBudgetKeyGeneratorFactory;

  private static final String KEYFILE_NAME = "key";

  @Test
  public void testPrivacyBudgetKeyFromAvro() throws Exception {
    ExtractionUtils.KeyFile goldenKeyFile =
        ExtractionUtils.KeyFile.create(KEYFILE_NAME, getKeySetAsString(buildUnitSet()));
    Path avro =
        Paths.get(
            "javatests/com/google/aggregate/tools/privacybudgetutil/common/test_data/input_version_01.avro");

    try {
      InputStream stream = Files.newInputStream(avro);
      ExtractionUtils.KeyFile file =
          ExtractionUtils.processAvro(
              stream,
              privacyBudgetKeyGeneratorFactory,
              KEYFILE_NAME,
              ImmutableList.of(UnsignedLong.ZERO));
      assertThat(file).isEqualTo(goldenKeyFile);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testPrivacyBudgetKeyFromAvroWithFilteringIds() throws Exception {
    ExtractionUtils.KeyFile goldenKeyFile =
        ExtractionUtils.KeyFile.create(KEYFILE_NAME, getKeySetAsString(buildFilteringIdsUnitSet()));
    Path avro =
        Paths.get(
            "javatests/com/google/aggregate/tools/privacybudgetutil/common/test_data/input_version_1.avro");

    InputStream stream = Files.newInputStream(avro);
    ExtractionUtils.KeyFile file =
        ExtractionUtils.processAvro(
            stream,
            privacyBudgetKeyGeneratorFactory,
            KEYFILE_NAME,
            ImmutableList.of(
                UnsignedLong.valueOf(1), UnsignedLong.valueOf(2), UnsignedLong.valueOf(3)));
    assertThat(file).isEqualTo(goldenKeyFile);
  }

  private static Set<PrivacyBudgetUnit> buildUnitSet() {
    return ImmutableSet.of(
        PrivacyBudgetUnit.create(
            "4c136a585949aefbd6180b817933393353a27bdd3936d246a10c694066d5a79e",
            Instant.ofEpochSecond(1710288000),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "4c136a585949aefbd6180b817933393353a27bdd3936d246a10c694066d5a79e",
            Instant.ofEpochSecond(1710284400),
            "https://privacy-sandbox-demos-dsp.dev"));
  }

  private static Set<PrivacyBudgetUnit> buildFilteringIdsUnitSet() {
    return ImmutableSet.of(
        PrivacyBudgetUnit.create(
            "3bd6a0d3635c6d16f5c8c70bd63005f6f8e2ad31b72c95907afea9b35919cdc1",
            Instant.ofEpochSecond(1710284400),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "b3eb3651049728d8edc02542fc685586a9ffea9c559852e0746c6cef6ddcc137",
            Instant.ofEpochSecond(1710288000),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "3bd6a0d3635c6d16f5c8c70bd63005f6f8e2ad31b72c95907afea9b35919cdc1",
            Instant.ofEpochSecond(1710288000),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "db4bb8c6d309ce494bb8f9fc0f17c2b2dc7b34ed0338e7b92319da82456bf03e",
            Instant.ofEpochSecond(1710284400),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "b3eb3651049728d8edc02542fc685586a9ffea9c559852e0746c6cef6ddcc137",
            Instant.ofEpochSecond(1710284400),
            "https://privacy-sandbox-demos-dsp.dev"),
        PrivacyBudgetUnit.create(
            "db4bb8c6d309ce494bb8f9fc0f17c2b2dc7b34ed0338e7b92319da82456bf03e",
            Instant.ofEpochSecond(1710288000),
            "https://privacy-sandbox-demos-dsp.dev"));
  }

  private static String getKeySetAsString(Set<PrivacyBudgetUnit> unitSet)
      throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    return om.writeValueAsString(unitSet);
  }

  static final class TestEnv extends AbstractModule {

    @Override
    protected void configure() {
      install(new PrivacyBudgetKeyGeneratorModule());
    }
  }
}
