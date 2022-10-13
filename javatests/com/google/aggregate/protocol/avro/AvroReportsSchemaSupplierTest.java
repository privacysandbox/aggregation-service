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

package com.google.aggregate.protocol.avro;

import static com.google.common.truth.Truth.assertThat;

import org.apache.avro.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvroReportsSchemaSupplierTest {

  AvroReportsSchemaSupplier schemaSupplier;

  @Before
  public void setUp() {
    schemaSupplier = new AvroReportsSchemaSupplier();
  }

  @Test
  public void correctSchema() {
    Schema schema = schemaSupplier.get();

    assertThat(schema.getFields()).hasSize(3);
    Schema.Field payloadField = schema.getFields().get(0);
    Schema.Field keyIdField = schema.getFields().get(1);
    Schema.Field sharedInfoField = schema.getFields().get(2);
    assertThat(payloadField.name()).isEqualTo("payload");
    assertThat(keyIdField.name()).isEqualTo("key_id");
    assertThat(sharedInfoField.name()).isEqualTo("shared_info");
  }

  @Test
  public void schemaIsCached() {
    Schema schema = schemaSupplier.get();
    Schema schemaAgain = schemaSupplier.get();

    assertThat(schema).isSameInstanceAs(schemaAgain);
  }
}
