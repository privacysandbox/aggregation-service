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

package com.google.aggregate.adtech.worker.frontend.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

/** ObjectMapper implementation of the {@code JsonSerializerFacade} interface. */
public final class ObjectMapperSerializerFacade implements JsonSerializerFacade {

  private final ObjectMapper objectMapper;

  /** Creates a new instance of the {@code ObjectMapperSerializerFacade} class. */
  @Inject
  public ObjectMapperSerializerFacade(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Serializes an object to json.
   *
   * @return {@code String} that is json serialized object
   * @throws JsonSerializerFacadeException if serialization failed
   */
  @Override
  public String serialize(Object toSerialize) throws JsonSerializerFacadeException {
    try {
      return objectMapper.writeValueAsString(toSerialize);
    } catch (JsonProcessingException e) {
      throw new JsonSerializerFacadeException(e);
    }
  }

  /**
   * Deserializes an object from json.
   *
   * @return {@code T} tha is deserialized oject
   * @throws JsonSerializerFacadeException if deserialization failed
   */
  @Override
  public <T> T deserialize(String json, Class<T> valueType) throws JsonSerializerFacadeException {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw new JsonSerializerFacadeException(e);
    }
  }
}
