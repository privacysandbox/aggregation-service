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

package com.google.aggregate.tools.privacybudgetutil.aws;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.aggregate.tools.privacybudgetutil.common.PrivacyBudgetUnitExtractionConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Provides bindings for {@link AwsPrivacyBudgetUnitExtraction}. */
final class AwsPrivacyBudgetUnitExtractionModule extends AbstractModule {

  @Provides
  @Client
  S3Client provideS3Client(PrivacyBudgetUnitExtractionConfig config) {
    S3Client client =
        S3Client.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .region(Region.of(config.getRegion()))
            .build();
    return client;
  }

  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface Client {}
}
