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

import static com.google.aggregate.adtech.worker.encryption.publickeyuri.CloudEncryptionKeyConfig.NUM_ENCRYPTION_KEYS;

import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKey;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.EncryptionKeyService;
import com.google.aggregate.adtech.worker.encryption.hybrid.key.ReEncryptionKeyService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

/**
 * TODO(b/321088264): Merge CloudReEncryptionKeyService and CloudEncryptionKeyService {@link
 * EncryptionKeyService} implementation to get a random encryption key from public key endpoint for
 * reencryption.
 */
public final class CloudReEncryptionKeyService implements ReEncryptionKeyService {

  private static final int REQUEST_TIMEOUT_DURATION =
      Ints.checkedCast(Duration.ofMinutes(1).toMillis());
  private static final RequestConfig REQUEST_CONFIG =
      RequestConfig.custom()
          .setConnectionRequestTimeout(REQUEST_TIMEOUT_DURATION)
          .setConnectTimeout(REQUEST_TIMEOUT_DURATION)
          .setSocketTimeout(REQUEST_TIMEOUT_DURATION)
          .build();
  private static final int MAX_CACHE_SIZE = 5;
  private static final long CACHE_ENTRY_TTL_SEC = 3600;
  private static final Random RANDOM = new Random();
  private final HttpClientWrapper httpClient;
  private final LoadingCache<String, ImmutableList<EncodedPublicKey>> encryptionKeysCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_CACHE_SIZE)
          .expireAfterWrite(CACHE_ENTRY_TTL_SEC, TimeUnit.SECONDS)
          .concurrencyLevel(Runtime.getRuntime().availableProcessors())
          .build(
              new CacheLoader<>() {
                @Override
                public ImmutableList<EncodedPublicKey> load(String uri)
                    throws ReencryptionKeyFetchException {
                  return getPublicKeysFromService(uri);
                }
              });

  @Inject
  public CloudReEncryptionKeyService(HttpClientWrapper httpClient) {
    this.httpClient = httpClient;
  }

  /** Throws ReencryptionKeyFetchException. */
  @Override
  public EncryptionKey getEncryptionPublicKey(String keyVendingUri)
      throws ReencryptionKeyFetchException {
    try {
      ImmutableList<EncodedPublicKey> publicKeys = encryptionKeysCache.get(keyVendingUri);
      EncodedPublicKey publicKey = publicKeys.get(randomIndex());
      return EncryptionKey.builder()
          .setKey(PublicKeyConversionUtil.getKeysetHandle(publicKey.getKey()))
          .setId(publicKey.getId())
          .build();
    } catch (GeneralSecurityException | ExecutionException e) {
      throw new ReencryptionKeyFetchException(e);
    }
  }

  private ImmutableList<EncodedPublicKey> getPublicKeysFromService(String publicKeyServiceUri)
      throws ReencryptionKeyFetchException {
    try {
      HttpGet request = new HttpGet(URI.create(publicKeyServiceUri));
      request.setConfig(REQUEST_CONFIG);
      HttpClientResponse response = httpClient.execute(request);
      if (response.statusCode() != 200) {
        throw new ReencryptionKeyFetchException(response.responseBody());
      }
      GetActivePublicKeysResponse.Builder builder = GetActivePublicKeysResponse.newBuilder();
      JsonFormat.parser().merge(response.responseBody(), builder);
      GetActivePublicKeysResponse keys = builder.build();
      return ImmutableList.copyOf(keys.getKeysList());
    } catch (Exception e) {
      throw new ReencryptionKeyFetchException(e);
    }
  }

  private int randomIndex() {
    return RANDOM.nextInt(NUM_ENCRYPTION_KEYS);
  }
}
