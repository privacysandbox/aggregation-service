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

package com.google.aggregate.adtech.worker.encryption.publickeyuri;

import java.util.HashMap;
import java.util.Map;

/** Factory class to get EncryptionKeyConfig based on public key uri cloud provider type. */
public final class EncryptionKeyConfigFactory {
  public static String CLOUD_PROVIDER_NAME_GCP = "GCP";
  private static final Map<String, CloudEncryptionKeyConfig> cloudEncryptionKeyConfigMap =
      new HashMap<>();

  /**
   * Returns EncryptionKeyConfigType for the given cloud provider.
   *
   * @throws IllegalArgumentException when invalid cloud provider name is provided.
   */
  public static CloudEncryptionKeyConfig getCloudEncryptionKeyConfig(String cloudProviderName) {
    if (cloudProviderName.isEmpty()) {
      throw new IllegalArgumentException("Cloud provider name not set.");
    } else if (cloudProviderName.equals(CLOUD_PROVIDER_NAME_GCP)) {
      cloudEncryptionKeyConfigMap.putIfAbsent(
          CLOUD_PROVIDER_NAME_GCP,
          CloudEncryptionKeyConfig.builder()
              .setKeyVendingServiceUri(
                  "https://publickeyservice-a.postsb-a.test.aggregationhelper.com/.well-known/aggregation-service/v1/public-keys")
              .build());
      return cloudEncryptionKeyConfigMap.get(CLOUD_PROVIDER_NAME_GCP);
    } else {
      throw new IllegalArgumentException("Invalid cloud provider.");
    }
  }
}
