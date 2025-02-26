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

package com.google.aggregate.adtech.worker.model.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Converter;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts to/from JSON formatted String and the PrivacyBudgetExhaustedInfo. */
public final class PrivacyBudgetExhaustedInfoSerdes
    extends Converter<String, Optional<PrivacyBudgetExhaustedInfo>> {

  private static final Logger logger =
      LoggerFactory.getLogger(PrivacyBudgetExhaustedInfoSerdes.class);

  TimeObjectMapper objectMapper;

  @Inject
  PrivacyBudgetExhaustedInfoSerdes(TimeObjectMapper objectMapper) {
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
    // Setting DEFAULT_VIEW_INCLUSION false, to not include properties in serialization by default.
    objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    // Setting SORT_PROPERTIES_ALPHABETICALLY to serialize sharedInfo in fixed order.
    objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    this.objectMapper = objectMapper;
  }

  /**
   * Convert a JSON String to a {@link PrivacyBudgetExhaustedInfo}.
   *
   * @param privacyBudgetExhaustedInfoString JSON string of a serialized PrivacyBudgetExhaustedInfo
   *     object
   * @return {@link Optional} with PrivacyBudgetExhaustedInfo present if deserialization succeeds,
   *     empty if it fails. If an empty string is provided as input an empty Optional is returned.
   */
  @Override
  protected Optional<PrivacyBudgetExhaustedInfo> doForward(
      String privacyBudgetExhaustedInfoString) {
    if (!privacyBudgetExhaustedInfoString.isEmpty()) {
      try {
        return Optional.of(
            objectMapper.readValue(
                privacyBudgetExhaustedInfoString, PrivacyBudgetExhaustedInfo.class));
      } catch (JsonProcessingException exception) {
        logger.warn(
            String.format(
                "Exception while deserializing PrivacyBudgetExhaustedInfo : %s",
                exception.getMessage()));
      }
    }
    logger.warn(
        String.format(
            "Failed to deserialize privacyBudgetExhaustedInfo from string. Value was: '%s'",
            privacyBudgetExhaustedInfoString));
    return Optional.empty();
  }

  /**
   * Convert a {@link PrivacyBudgetExhaustedInfo} to JSON String.
   *
   * @param privacyBudgetExhaustedInfo to convert
   * @return JSON String of the PrivacyBudgetExhaustedInfo. String will be empty if the input
   *     SharedInfo is empty or if serialization fails.
   */
  @Override
  protected String doBackward(Optional<PrivacyBudgetExhaustedInfo> privacyBudgetExhaustedInfo) {
    if (privacyBudgetExhaustedInfo.isPresent()) {
      try {
        return objectMapper.writeValueAsString(privacyBudgetExhaustedInfo.get());
      } catch (JsonProcessingException exception) {
        logger.warn(
            String.format(
                "Exception while serializing privacyBudgetExhaustedInfo ."
                    + " privacyBudgetExhaustedInfo was: %s. Error message: %s",
                privacyBudgetExhaustedInfo, exception.getMessage()));
      }
    }
    logger.warn(
        "Failed to serialize privacyBudgetExhaustedInfo to JSON String. Value was: "
            + privacyBudgetExhaustedInfo);
    return "";
  }

  /**
   * Convert a {@link PrivacyBudgetExhaustedInfo} to JSON String with given view.
   *
   * @param privacyBudgetExhaustedInfo to convert
   * @param view used for serializing
   * @return JSON String of the PrivacyBudgetExhaustedInfo. String will be empty if the input
   *     SharedInfo is empty or if serialization fails.
   */
  public String doBackwardWithView(
      Optional<PrivacyBudgetExhaustedInfo> privacyBudgetExhaustedInfo, Class<?> view) {
    if (privacyBudgetExhaustedInfo.isPresent()) {
      try {
        return objectMapper
            .writerWithView(view)
            .writeValueAsString(privacyBudgetExhaustedInfo.get());
      } catch (JsonProcessingException exception) {
        logger.warn(
            String.format(
                "Exception while serializing privacyBudgetExhaustedInfo with view %s."
                    + " privacyBudgetExhaustedInfo was: %s. Error message: %s",
                view.getName(), privacyBudgetExhaustedInfo, exception.getMessage()));
      }
    }
    logger.warn(
        "Failed to serialize privacyBudgetExhaustedInfo to JSON String. Value was: "
            + privacyBudgetExhaustedInfo);
    return "";
  }
}
