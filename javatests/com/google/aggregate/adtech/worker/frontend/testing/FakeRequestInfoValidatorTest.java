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

package com.google.aggregate.adtech.worker.frontend.testing;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.aggregate.protos.shared.backend.RequestInfoProto.RequestInfo;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeRequestInfoValidatorTest {

  // Under test
  FakeRequestInfoValidator fakeRequestInfoValidator;

  RequestInfo requestInfo;

  @Before
  public void setUp() {
    fakeRequestInfoValidator = new FakeRequestInfoValidator();
    requestInfo =
        RequestInfo.newBuilder()
            .setJobRequestId("foo")
            .setInputDataBlobPrefix("file.avro")
            .setInputDataBucketName("bucket")
            .setOutputDataBlobPrefix("outputfile.avro")
            .setOutputDataBucketName("bucket")
            .setPostbackUrl("http://bar.com/api/endpoint")
            .putAllJobParameters(
                ImmutableMap.of(
                    "attribution_report_to",
                    "bar.com",
                    "output_domain_blob_prefix",
                    "outputfile.avro",
                    "output_domain_bucket_name",
                    "bucket",
                    "debug_privacy_budget_limit",
                    "5"))
            .build();
  }

  @Test
  public void returnsEmptyByDefault() {
    // No setup

    Optional<String> errorMessage = fakeRequestInfoValidator.validate(requestInfo);

    assertThat(errorMessage).isEmpty();
  }

  @Test
  public void returnsErrorIfSet() {
    String errorMessageContents = "Oh no an error";
    fakeRequestInfoValidator.setValidateReturnValue(Optional.of(errorMessageContents));

    Optional<String> errorMessage = fakeRequestInfoValidator.validate(requestInfo);

    assertThat(errorMessage).isPresent();
    assertThat(errorMessage).hasValue(errorMessageContents);
  }
}
