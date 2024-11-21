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

package com.google.aggregate.adtech.worker.encryption.hybrid.key.cloud;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKey;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService.ReencryptionKeyFetchException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.scp.shared.api.util.HttpClientResponse;
import com.google.scp.shared.api.util.HttpClientWrapper;
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
public class CloudReEncryptionKeyServiceTest {

  private static final String KEY_ID_1 = "00000000-0000-0000-0000-000000000000";
  private static final String KEY_ID_2 = "00000000-0000-0000-0000-111111111111";
  private static final String KEY_ID_3 = "00000000-0000-0000-0000-222222222222";
  private static final String KEY_ID_4 = "00000000-0000-0000-0000-333333333333";
  private static final String KEY_ID_5 = "00000000-0000-0000-0000-444444444444";
  private static final ImmutableList<String> keySet =
      ImmutableList.of(KEY_ID_1, KEY_ID_2, KEY_ID_3, KEY_ID_4, KEY_ID_5);

  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpClientWrapper httpClient;
  CloudReEncryptionKeyService cloudReEncryptionKeyService;
  String keyVendingResponse;
  String publicKey;
  String keyVendingServiceUri =
      "https://publickeyservice.aggregationhelper.com/.well-known/aggregation-service/v1/public-keys";

  @Before
  public void setup() {
    cloudReEncryptionKeyService = new CloudReEncryptionKeyService(httpClient);
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
  }

  @Test
  public void getCloudProviderKey_succeeds() throws Exception {
    HttpClientResponse response = buildFakeResponse(200, keyVendingResponse);
    when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);

    EncryptionKey key = cloudReEncryptionKeyService.getEncryptionPublicKey(keyVendingServiceUri);

    assertThat(keySet).contains(key.id());
  }

  @Test
  public void getCloudProviderKey_fails() throws Exception {
    HttpClientResponse response = buildFakeResponse(500, keyVendingResponse);
    when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);

    assertThrows(
        ReencryptionKeyFetchException.class,
        () -> cloudReEncryptionKeyService.getEncryptionPublicKey(keyVendingServiceUri));
  }

  private HttpClientResponse buildFakeResponse(int statusCode, String body) {
    HttpClientResponse response = HttpClientResponse.create(statusCode, body, ImmutableMap.of());
    return response;
  }
}
