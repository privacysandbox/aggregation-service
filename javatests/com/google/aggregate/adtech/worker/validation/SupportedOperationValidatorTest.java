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

package com.google.aggregate.adtech.worker.validation;

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.common.truth.Truth.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.ErrorCounter;
import com.google.aggregate.adtech.worker.model.ErrorMessage;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.aggregate.adtech.worker.model.Report;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.aggregate.adtech.worker.jobclient.model.Job;
import com.google.aggregate.adtech.worker.jobclient.testing.FakeJobGenerator;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SupportedOperationValidatorTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private SupportedOperationValidator validator;

  private Report.Builder reportBuilder;
  private Job ctx;

  @Before
  public void setUp() {
    reportBuilder =
        Report.builder()
            .setSharedInfo(
                SharedInfo.builder()
                    .setVersion(LATEST_VERSION)
                    .setReportingOrigin("")
                    .setDestination("")
                    .setSourceRegistrationTime(Instant.now())
                    .setScheduledReportTime(Instant.now())
                    .build());
    ctx = FakeJobGenerator.generate("");
  }

  @Test
  public void testHistogramPasses() {
    Report report =
        reportBuilder
            .setPayload(Payload.builder().setOperation(Payload.HISTOGRAM_OPERATION).build())
            .build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isEmpty();
  }

  @Test
  public void testNotHistogramFails() {
    Report report = reportBuilder.setPayload(Payload.builder().setOperation("foo").build()).build();

    Optional<ErrorMessage> validationError = validator.validate(report, ctx);

    assertThat(validationError).isPresent();
    assertThat(validationError.get().category()).isEqualTo(ErrorCounter.UNSUPPORTED_OPERATION);
  }

  public static final class TestEnv extends AbstractModule {}
}
