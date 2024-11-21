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

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static java.time.temporal.ChronoUnit.HOURS;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge.PrivacyBudgetUnit;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGeneratorFactory;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractionUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionUtils.class);

  /** Takes a .avro file as an input and generates the correct key for the API. */
  public static ExtractionUtils.KeyFile processAvro(
      InputStream stream,
      PrivacyBudgetKeyGeneratorFactory generatorFactory,
      String key,
      List<UnsignedLong> filteringIds)
      throws IOException {
    GenericRecord avroRecord = null;
    SharedInfo info = null;
    Set<PrivacyBudgetUnit> keyset = new HashSet<PrivacyBudgetUnit>();
    final DataFileStream<GenericRecord> dfStream =
        new DataFileStream(stream, new GenericDatumReader<GenericRecord>());
    while (dfStream.hasNext()) {
      avroRecord = dfStream.next(avroRecord);
      if (!avroRecord.hasField("shared_info")) {
        LOGGER.warn(String.format("No shared info in %s, skipping.%n", key));
        continue;
      }
      Object sharedInfo = avroRecord.get("shared_info");
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode root = objectMapper.readTree(sharedInfo.toString());
      BigDecimal reportTime =
          new BigDecimal(root.get("scheduled_report_time").toString().replace("\"", ""));

      SharedInfo.Builder builder =
          SharedInfo.builder()
              .setApi(root.get("api").asText())
              .setVersion(root.get("version").asText())
              .setScheduledReportTime(
                  Instant.ofEpochSecond(reportTime.setScale(0, RoundingMode.DOWN).longValue()))
              .setReportingOrigin(root.get("reporting_origin").asText());
      if (!root.has("attribution_destination")
              && root.get("api").toString().equals(ATTRIBUTION_REPORTING_API)
          || root.get("api").toString().equals(ATTRIBUTION_REPORTING_DEBUG_API)) {
        LOGGER.error("shared info has no attribution destination. skipping");
        continue;
      }
      if (root.has("attribution_destination")) {
        builder.setDestination(root.get("attribution_destination").asText());
      }

      if (root.has("source_registration_time")) {
        BigDecimal sourceRegistrationTime =
            new BigDecimal(root.get("source_registration_time").toString().replace("\"", ""));

        builder.setSourceRegistrationTime(
            Instant.ofEpochSecond(
                sourceRegistrationTime.setScale(0, RoundingMode.DOWN).longValue()));
      }
      if (root.has("debug_mode")) {
        builder.setReportDebugModeString(root.get("debug_mode").asText());
      }
      info = builder.build();

      for (UnsignedLong filteringId : filteringIds) {
        keyset.add(
            PrivacyBudgetUnit.create(
                getPrivacyBudgetKey(info, filteringId, generatorFactory),
                info.scheduledReportTime().truncatedTo(HOURS),
                info.reportingOrigin()));
      }
    }
    if (keyset.isEmpty()) {
      return null;
    }
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    ExtractionUtils.KeyFile keyFile =
        ExtractionUtils.KeyFile.create(key, om.writeValueAsString(keyset));
    return keyFile;
  }

  /** Calculates Privacy Budget Keys */
  private static String getPrivacyBudgetKey(
      SharedInfo sharedInfo,
      UnsignedLong filteringId,
      PrivacyBudgetKeyGeneratorFactory generatorFactory) {
    try {
      PrivacyBudgetKeyInput input =
          PrivacyBudgetKeyInput.builder()
              .setSharedInfo(sharedInfo)
              .setFilteringId(filteringId)
              .build();
      Optional<PrivacyBudgetKeyGenerator> privacyBudgetKeyGenerator =
          generatorFactory.getPrivacyBudgetKeyGenerator(input);
      return privacyBudgetKeyGenerator.get().generatePrivacyBudgetKey(input);
    } catch (IllegalArgumentException | NoSuchElementException e) {
      throw new AssertionError("failed to get generator", e);
    }
  }

  @AutoValue
  public abstract static class KeyFile {

    public static ExtractionUtils.KeyFile create(String key, String body) {
      return new AutoValue_ExtractionUtils_KeyFile(key, body);
    }

    public abstract String key();

    public abstract String body();
  }
}
