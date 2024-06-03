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

package com.google.aggregate.adtech.worker.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.aggregate.adtech.worker.util.NumericConversions;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Deserializes the {@link Fact} from the format Chrome produces which uses byte strings for 128-bit
 * and 32-bit unsigned integers
 */
public final class FactDeserializer extends StdDeserializer<Fact> {

  FactDeserializer() {
    super(Fact.class);
  }

  @Override
  public Fact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.readValueAsTree();
    byte[] bucketBytes = node.get("bucket").binaryValue();
    byte[] valueBytes = node.get("value").binaryValue();

    // Decode bucket and value from bytes
    BigInteger bucket = NumericConversions.uInt128FromBytes(bucketBytes);
    long value = NumericConversions.uInt32FromBytes(valueBytes);

    Fact.Builder fact = Fact.builder().setBucket(bucket).setValue(value);
    if (node.has("id")) {
      fact.setId(NumericConversions.getUnsignedLongFromBytes(node.get("id").binaryValue()));
    }

    return fact.build();
  }
}
