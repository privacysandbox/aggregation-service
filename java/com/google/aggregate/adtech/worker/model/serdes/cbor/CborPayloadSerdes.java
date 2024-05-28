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

package com.google.aggregate.adtech.worker.model.serdes.cbor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.serdes.PayloadSerdes;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to/from a ByteSource containing a CBOR serialized {@link Payload}.
 *
 * <p>Optionals are used in lieu of checked exceptions.
 */
public final class CborPayloadSerdes extends PayloadSerdes {

  private static final Logger logger = LoggerFactory.getLogger(CborPayloadSerdes.class);

  private final EnhancedCborMapper cborMapper;

  @Inject
  public CborPayloadSerdes(EnhancedCborMapper cborMapper) {
    this.cborMapper = cborMapper;
  }

  /**
   * Convert bytes to a {@link Payload}.
   *
   * @param byteSource raw, plaintext bytes of a CBOR serialized Payload object
   * @return {@link Optional} with Payload present if deserialization succeeds, empty if it fails.
   */
  @Override
  protected Optional<Payload> doForward(ByteSource byteSource) {
    try {
      if (byteSource.isEmpty()) {
        logger.warn("Empty byte source for deserializing");
        return Optional.empty();
      }
      return Optional.of(cborMapper.readValue(byteSource.read(), Payload.class));
    } catch (IOException | ClassCastException | NullPointerException e) {
      // Exception is not included because stack trace includes the decrypted payload which is
      // private information
      logger.warn("Failed to deserialize from CBOR bytes to Payload");
      return Optional.empty();
    }
  }

  /**
   * Convert a {@link Payload} to bytes.
   *
   * @param payload the optional payload object
   * @return raw, plaintext, bytes of a CBOR serialized payload object. ByteSource will be empty if
   *     the input Optional is empty.
   */
  @Override
  protected ByteSource doBackward(Optional<Payload> payload) {
    if (payload.isPresent()) {
      try {
        return ByteSource.wrap(cborMapper.writeValueAsBytes(payload.get()));
      } catch (JsonProcessingException e) {
        // Exception is not included because stack trace includes the decrypted payload which is
        // private information
        logger.warn("Failed to serialize Payload to CBOR bytes");
      }
    }
    return ByteSource.empty();
  }
}
