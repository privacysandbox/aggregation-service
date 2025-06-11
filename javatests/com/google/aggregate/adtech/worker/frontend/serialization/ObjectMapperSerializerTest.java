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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.inject.AbstractModule;
import com.google.aggregate.adtech.worker.frontend.testing.TestCreateRequest;
import com.google.scp.shared.mapper.GuavaObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ObjectMapperSerializerTest {

  @Rule public Acai acai = new Acai(TestEnv.class);
  private ObjectMapperSerializerFacade target;

  @Before
  public void setup() {
    target = new ObjectMapperSerializerFacade(new GuavaObjectMapper());
  }

  @Test
  public void serializeSerializesInputToJson() throws JsonSerializerFacadeException {
    TestCreateRequest request = TestCreateRequest.builder().id(5).name("susan").build();

    String json = target.serialize(request);

    assertThat(json).isEqualTo("{\"name\":\"susan\",\"id\":5}");
  }

  @Test
  public void deserializeDeserializesInputToObject() throws JsonSerializerFacadeException {
    TestCreateRequest result =
        target.deserialize("{\"name\":\"susan\",\"id\":5}", TestCreateRequest.class);

    assertThat(result).isEqualTo(TestCreateRequest.builder().id(5).name("susan").build());
  }

  @Test
  public void assertThrowsExceptionOnSerializeError() {
    assertThrows(
        JsonSerializerFacadeException.class, () -> target.serialize(new FailOnSerialize()));
  }

  @Test
  public void assertThrowsExceptionOnDeserializeError() {
    assertThrows(
        JsonSerializerFacadeException.class,
        () -> target.deserialize(null, TestCreateRequest.class));
  }

  private static class TestEnv extends AbstractModule {

    @Override
    public void configure() {
      bind(ObjectMapper.class).to(GuavaObjectMapper.class);
    }
  }

  private static final class FailOnSerialize {
    public int id;
    private String data;

    @JsonProperty("data")
    public String getTheData() throws Exception {
      throw new Exception("Error returning data");
    }

    @JsonProperty("data")
    public void setTheData(String data) {
      this.data = data;
    }
  }
}
