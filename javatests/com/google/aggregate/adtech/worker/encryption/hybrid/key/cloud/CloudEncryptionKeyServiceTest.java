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

package com.google.aggregate.adtech.worker.encryption.hybrid.key.cloud;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.acai.Acai;
import com.google.aggregate.protocol.proto.EncryptionKeyConfigProto.EncryptionKeyConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.inject.AbstractModule;
import com.google.scp.shared.api.util.HttpClientResponse;
import com.google.scp.shared.api.util.HttpClientWrapper;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class CloudEncryptionKeyServiceTest {

  private static final String KEY_ID_1 = "00000000-0000-0000-0000-000000000000";
  private static final String KEY_ID_2 = "00000000-0000-0000-0000-111111111111";
  private static final String KEY_ID_3 = "00000000-0000-0000-0000-222222222222";
  private static final String KEY_ID_4 = "00000000-0000-0000-0000-333333333333";
  private static final String KEY_ID_5 = "00000000-0000-0000-0000-444444444444";

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpClientWrapper httpClient;

  @Rule public final Acai acai = new Acai(TestEnv.class);
  CloudEncryptionKeyService cloudEncryptionKeyService;
  String keyVendingResponse;
  String publicKey;
  ImmutableList expected;

  @Before
  public void setup() {
    cloudEncryptionKeyService =
        new CloudEncryptionKeyService(
            httpClient,
            EncryptionKeyConfig.newBuilder()
                .setNumEncryptionKeys(5)
                .setKeyVendingServiceUri("https://mock.uri.com")
                .build(),
            new Random(123));
    publicKey =
        "EkQKBAgCEAMSOhI4CjB0eXBlLmdvb2dsZWFwaXMuY29tL2dvb2dsZS5jcnlwdG8udGluay5BZXNH"
            + "Y21LZXkSAhAQGAEYARohAJryfZtZSsWNdh86h3sOuxRurI4q/Qg2ECaABVGfgOu6IiEAjAYDniN7v5mb"
            + "bMhPbXVSkPhEZFx84sB7MKB/AiN6KBI=";
    keyVendingResponse =
        String.format(
            "{\"keys\":[{\"id\":\"%s\",\"key\":\"%s\"},"
                + "{\"id\":\"%s\",\"key\":\"%s\"},"
                + "{\"id\":\"%s\",\"key\":\"%s\"},"
                + "{\"id\":\"%s\",\"key\":\"%s\"},"
                + "{\"id\":\"%s\",\"key\":\"%s\"}]}",
            KEY_ID_1, publicKey, KEY_ID_2, publicKey, KEY_ID_3, publicKey, KEY_ID_4, publicKey,
            KEY_ID_5, publicKey);
    expected = ImmutableList.of(KEY_ID_1, KEY_ID_2, KEY_ID_3, KEY_ID_4, KEY_ID_5);
  }

  @Test
  public void getKey_singleThread() throws Exception {
    HttpClientResponse response = buildFakeResponse(200, keyVendingResponse);
    when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);
    Set keyIdSet = new HashSet();

    for (int i = 0; i < 1000; i++) {
      keyIdSet.add(cloudEncryptionKeyService.getKey());
    }

    assertThat(keyIdSet.containsAll(expected));
  }

  @Test
  public void getKey_multiThreads() throws Exception {
    Set keyIdSet = new HashSet();
    HttpClientResponse response = buildFakeResponse(200, keyVendingResponse);
    when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);
    ExecutorService executor = Executors.newFixedThreadPool(5);
    IntStream stream = IntStream.range(1, 1000);

    ImmutableList<Future> keyIdSetFutures =
        stream
            .boxed()
            .map(unused -> executor.submit(() -> cloudEncryptionKeyService.getKey().id()))
            .collect(toImmutableList());
    for (Future future : keyIdSetFutures) {
      keyIdSet.add(future.get());
    }
    executor.shutdown();

    assertThat(keyIdSet.containsAll(expected));
  }

  @Test
  public void getKey_outOfRange() throws Exception {
    CloudEncryptionKeyService cloudEncryptionKeyService =
        new CloudEncryptionKeyService(
            httpClient,
            EncryptionKeyConfig.newBuilder()
                .setNumEncryptionKeys(6)
                .setKeyVendingServiceUri("https://mock.uri.com")
                .build(),
            new Random(123));
    HttpClientResponse response = buildFakeResponse(200, keyVendingResponse);
    when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);

    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> {
          for (int i = 0; i < 1000; i++) {
            cloudEncryptionKeyService.getKey();
          }
        });
  }

  private HttpClientResponse buildFakeResponse(int statusCode, String body) {
    HttpClientResponse response = HttpClientResponse.create(statusCode, body, ImmutableMap.of());
    return response;
  }

  static class TestEnv extends AbstractModule {
    @Override
    public void configure() {
      // Register the Tink HybridConfig so that hybrid encryption can be used
      try {
        HybridConfig.register();
      } catch (GeneralSecurityException e) {
        // Throw as unchecked exception since this should be fatal
        throw new IllegalStateException("Could not register Tink HybridConfig", e);
      }
    }
  }
}
