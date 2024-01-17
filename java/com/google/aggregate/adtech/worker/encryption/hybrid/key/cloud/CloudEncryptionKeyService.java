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

import static com.google.common.base.Suppliers.memoize;
import static com.google.common.base.Suppliers.synchronizedSupplier;

import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKey;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService;
import com.google.aggregate.protocol.proto.EncryptionKeyConfigProto.EncryptionKeyConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.protobuf.util.JsonFormat;
import com.google.scp.coordinator.protos.keymanagement.keyhosting.api.v1.EncodedPublicKeyProto.EncodedPublicKey;
import com.google.scp.coordinator.protos.keymanagement.keyhosting.api.v1.GetActivePublicKeysResponseProto.GetActivePublicKeysResponse;
import com.google.scp.shared.api.util.HttpClientResponse;
import com.google.scp.shared.api.util.HttpClientWrapper;
import com.google.scp.shared.util.PublicKeyConversionUtil;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

/** {@link EncryptionKeyService} implementation to get a random encryption key from DynamoDb. */
public final class CloudEncryptionKeyService implements EncryptionKeyService {

  private final HttpClientWrapper httpClient;
  private static final int REQUEST_TIMEOUT_DURATION =
      Ints.checkedCast(Duration.ofMinutes(1).toMillis());
  private final EncryptionKeyConfig encryptionKeyConfig;
  private final Random random;

  private final Supplier<ImmutableList<EncodedPublicKey>> keysFetchMemoizedSupplier;

  @Inject
  public CloudEncryptionKeyService(
      HttpClientWrapper httpClient, EncryptionKeyConfig encryptionKeyConfig, Random random) {
    this.httpClient = httpClient;
    this.encryptionKeyConfig = encryptionKeyConfig;
    this.random = random;
    keysFetchMemoizedSupplier = synchronizedSupplier(memoize(this::getPublicKeysFromService));
  }

  @Override
  public EncryptionKey getKey() throws KeyFetchException {
    try {
      ImmutableList<EncodedPublicKey> publicKeys = keysFetchMemoizedSupplier.get();

      // Find key with key.id() equal to KeyId.
      EncodedPublicKey publicKey = publicKeys.get(randomIndex());
      return EncryptionKey.builder()
          .setKey(PublicKeyConversionUtil.getKeysetHandle(publicKey.getKey()))
          .setId(publicKey.getId())
          .build();
    } catch (KeyFetchUncheckedException e) {
      throw new KeyFetchException(e.getCause());
    } catch (GeneralSecurityException e) {
      throw new KeyFetchException(e);
    }
  }

  private ImmutableList<EncodedPublicKey> getPublicKeysFromService() {
    try {
      HttpGet request = new HttpGet(URI.create(encryptionKeyConfig.getKeyVendingServiceUri()));
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectionRequestTimeout(REQUEST_TIMEOUT_DURATION)
              .setConnectTimeout(REQUEST_TIMEOUT_DURATION)
              .setSocketTimeout(REQUEST_TIMEOUT_DURATION)
              .build();
      request.setConfig(requestConfig);
      HttpClientResponse response = httpClient.execute(request);
      if (response.statusCode() != 200) {
        throw new KeyFetchException(response.responseBody());
      }
      GetActivePublicKeysResponse.Builder builder = GetActivePublicKeysResponse.newBuilder();
      JsonFormat.parser().merge(response.responseBody(), builder);
      GetActivePublicKeysResponse keys = builder.build();
      return ImmutableList.copyOf(keys.getKeysList());
    } catch (Exception e) {
      throw new KeyFetchUncheckedException(e);
    }
  }

  private int randomIndex() {
    return random.nextInt(encryptionKeyConfig.getNumEncryptionKeys());
  }

  private static final class KeyFetchUncheckedException extends RuntimeException {

    private KeyFetchUncheckedException(Throwable cause) {
      super(cause);
    }
  }
}
