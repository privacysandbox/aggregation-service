/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.frontend.service.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.frontend.service.aws.model.DdbStreamBatchInfo;

/** Reads metadata messages for failed lambda events to create {@link DdbStreamBatchInfo}. */
public final class DdbStreamBatchInfoParser {

  private static final String STREAM_BATCH_INFO_FIELD = "DDBStreamBatchInfo";

  private final ObjectMapper objectMapper;

  /** Creates a new instance of the {@code DdbStreamBatchInfoParser} class. */
  @Inject
  DdbStreamBatchInfoParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Read the messageBody to produce a {@link DdbStreamBatchInfo}.
   *
   * @throws DdbStreamBatchInfoParseException if the object could not be read
   */
  public DdbStreamBatchInfo batchInfoFromMessageBody(String messageBody) {
    try {
      JsonNode body = objectMapper.readTree(messageBody);
      if (!body.has(STREAM_BATCH_INFO_FIELD)) {
        throw new DdbStreamBatchInfoParseException(
            new IllegalArgumentException(
                STREAM_BATCH_INFO_FIELD
                    + " was missing from messageBody, could not handle message"));
      }
      return objectMapper.readValue(
          body.get(STREAM_BATCH_INFO_FIELD).toString(), DdbStreamBatchInfo.class);
    } catch (JsonProcessingException e) {
      throw new DdbStreamBatchInfoParseException(e);
    }
  }

  /** Exception for when the message couldn't be parsed. */
  public static final class DdbStreamBatchInfoParseException extends RuntimeException {
    DdbStreamBatchInfoParseException(Throwable e) {
      super(e);
    }
  }
}
