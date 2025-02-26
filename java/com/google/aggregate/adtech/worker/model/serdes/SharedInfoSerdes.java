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

package com.google.aggregate.adtech.worker.model.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.shared.mapper.TimeObjectMapper;
import com.google.common.base.Converter;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to/from JSON formatted String and the SharedInfo.
 *
 * <p>Optionals are used in lieu of checked exceptions.
 */
public final class SharedInfoSerdes extends Converter<String, Optional<SharedInfo>> {

  private static final Logger logger = LoggerFactory.getLogger(SharedInfoSerdes.class);

  TimeObjectMapper objectMapper;

  @Inject
  SharedInfoSerdes(TimeObjectMapper objectMapper) {
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
    // Setting DEFAULT_VIEW_INCLUSION false, to not include properties in serialization by default.
    objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    // Setting SORT_PROPERTIES_ALPHABETICALLY to serialize sharedInfo in fixed order.
    objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    this.objectMapper = objectMapper;
  }

  /**
   * Convert a JSON String to a {@link SharedInfo}.
   *
   * @param sharedInfoJsonString JSON string of a serialized SharedInfo object
   * @return {@link Optional} with SharedInfo present if deserialization succeeds, empty if it
   *     fails. If an empty string is provided as input an empty Optional is returned.
   */
  @Override
  protected Optional<SharedInfo> doForward(String sharedInfoJsonString) {
    if (!sharedInfoJsonString.isEmpty()) {
      try {
        return Optional.of(objectMapper.readValue(sharedInfoJsonString, SharedInfo.class));
      } catch (JsonProcessingException ignored) {
        // Ignore exceptions and instead return Optional.empty() if deserialization fails
      }
    }
    logger.warn(
        String.format(
            "Failed to deserialize shared_info from string. Value was: '%s'",
            sharedInfoJsonString));
    return Optional.empty();
  }

  /**
   * Convert a {@link SharedInfo} to JSON String.
   *
   * @param sharedInfo the optional SharedInfo to convert
   * @return JSON String of the SharedInfo. String will be empty if the input SharedInfo is empty or
   *     if serialization fails.
   */
  @Override
  protected String doBackward(Optional<SharedInfo> sharedInfo) {
    if (sharedInfo.isPresent()) {
      try {
        return objectMapper.writeValueAsString(sharedInfo.get());
      } catch (JsonProcessingException ignored) {
        // Ignore exceptions and instead return empty string if deserialization fails
      }
    }
    logger.warn("Failed to serialize shared_info to JSON String. Value was: " + sharedInfo);
    return "";
  }

  protected String doBackwardWithView(Optional<SharedInfo> sharedInfo, Class<?> view) {
    if (sharedInfo.isPresent()) {
      try {
        return objectMapper.writerWithView(view).writeValueAsString(sharedInfo.get());
      } catch (JsonProcessingException exception) {
        logger.warn(
            String.format(
                "Exception while serializing shared_info with view %s. shared_info was: %s. Error"
                    + " message: %s",
                view.getName(), sharedInfo, exception.getMessage()));
      }
    }
    logger.warn("Failed to serialize shared_info to JSON String. Value was: " + sharedInfo);
    return "";
  }
}
