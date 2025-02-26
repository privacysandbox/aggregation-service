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

package com.google.aggregate.adtech.worker.model.serdes;

import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.PROTECTED_AUDIENCE_API;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.AggregatableInputBudgetConsumptionInfo;
import com.google.aggregate.adtech.worker.model.PrivacyBudgetExhaustedInfo;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.aggregate.adtech.worker.model.Views;
import com.google.aggregate.privacy.budgeting.budgetkeygenerator.PrivacyBudgetKeyGenerator.PrivacyBudgetKeyInput;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivacyBudgetExhaustedInfoSerdesTest {

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String SAMPLE_REPORT_ID = "129470d5-3095-4385-81e2-08f5a9063549";
  private static final String SAMPLE_REPORT_ID_2 = "541270d5-3095-4385-81e2-08f5a8909140";
  private static final UnsignedLong FILTERING_ID_ZERO = UnsignedLong.valueOf(0);
  private static final UnsignedLong FILTERING_ID_ONE = UnsignedLong.valueOf(1);

  private static final String DESTINATION = "destination.com";

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private PrivacyBudgetExhaustedInfoSerdes privacyBudgetExhaustedInfoSerdes;

  private static SharedInfo relaventSharedInfo =
      SharedInfo.builder()
          .setApi(ATTRIBUTION_REPORTING_API)
          .setVersion(LATEST_VERSION)
          .setSourceRegistrationTime(FIXED_TIME)
          .setScheduledReportTime(FIXED_TIME)
          .setDestination(DESTINATION)
          .setReportingOrigin("bar.com")
          .build();
  private static SharedInfo sharedInfoARA =
      SharedInfo.builder()
          .setApi(ATTRIBUTION_REPORTING_API)
          .setVersion(LATEST_VERSION)
          .setReportId(SAMPLE_REPORT_ID)
          .setSourceRegistrationTime(FIXED_TIME)
          .setScheduledReportTime(FIXED_TIME)
          .setDestination(DESTINATION)
          .setReportingOrigin("bar.com")
          .build();
  private static SharedInfo sharedInfoARA2 =
      SharedInfo.builder()
          .setApi(ATTRIBUTION_REPORTING_API)
          .setVersion(LATEST_VERSION)
          .setReportId(SAMPLE_REPORT_ID_2)
          .setSourceRegistrationTime(FIXED_TIME)
          .setScheduledReportTime(FIXED_TIME)
          .setDestination(DESTINATION)
          .setReportingOrigin("bar.com")
          .build();
  private static SharedInfo sharedInfoPAA =
      SharedInfo.builder()
          .setApi(PROTECTED_AUDIENCE_API)
          .setVersion(LATEST_VERSION)
          .setReportId(SAMPLE_REPORT_ID)
          .setScheduledReportTime(FIXED_TIME)
          .setReportingOrigin("bar.com")
          .build();

  @Test
  public void testDeserializeStringToPrivacyBudgetExhaustedInfo() {
    String privacyBudgetExhaustedInfoString =
        "{\"privacy_budget_exhausted_info\":{\"aggregatable_input_budget_consumption_info\":"
            + "[{\"aggregateable_input_budget_id\":{\"filtering_id\":0,\"relevant_shared_info\":"
            + "{\"api\":\"attribution-reporting\",\"attribution_destination\":\"destination.com"
            + "\",\"reporting_origin\":\"bar.com\",\"scheduled_report_time\""
            + ":1609459200.000000000,\"source_registration_time\":1609459200.000000000,"
            + "\"version\":\"1.0\"}}}]}}";

    Optional<PrivacyBudgetExhaustedInfo> privacyBudgetExhaustedInfo =
        privacyBudgetExhaustedInfoSerdes.convert(privacyBudgetExhaustedInfoString);

    assertThat(privacyBudgetExhaustedInfo.get())
        .isEqualTo(
            PrivacyBudgetExhaustedInfo.builder()
                .setAggregatableInputBudgetConsumptionInfos(
                    ImmutableSet.of(
                        AggregatableInputBudgetConsumptionInfo.builder()
                            .setPrivacyBudgetKeyInput(
                                PrivacyBudgetKeyInput.builder()
                                    .setSharedInfo(relaventSharedInfo)
                                    .setFilteringId(FILTERING_ID_ZERO)
                                    .build())
                            .build()))
                .build());
  }

  @Test
  public void testDeserializeMalformedString_fails() {
    String privacyBudgetExhaustedInfoString =
        "{\"privacy_budget_exhausted_info\":{\"aggregatable_input_budget_consumption_info\":"
            + "[{\"aggregateable_input_budget_id\":{\"filtering_id\":0,\"relevant_shared_info\":"
            + "{\"api\":\"attribution-reporting\",\"attribution_destination\":\"destination.com"
            + "\",\"reporting_origin\":\"bar.com\",\"scheduled_report_time\""
            + ":1609459200.000000000,\"source_registration_time\":1609459200.000000000,"
            + "\"version\":\"1.0\", [malformed,]}}}]}}";

    Optional<PrivacyBudgetExhaustedInfo> privacyBudgetExhaustedInfo =
        privacyBudgetExhaustedInfoSerdes.convert(privacyBudgetExhaustedInfoString);

    assertThat(privacyBudgetExhaustedInfo.isEmpty());
  }

  @Test
  public void testSerializePrivacyBudgetExhaustedInfo_ARAReport() {
    PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo =
        PrivacyBudgetExhaustedInfo.builder()
            .setAggregatableInputBudgetConsumptionInfos(
                ImmutableSet.of(
                    AggregatableInputBudgetConsumptionInfo.builder()
                        .setPrivacyBudgetKeyInput(
                            PrivacyBudgetKeyInput.builder()
                                .setSharedInfo(sharedInfoARA)
                                .setFilteringId(FILTERING_ID_ZERO)
                                .build())
                        .build()))
            .build();

    String serialized =
        privacyBudgetExhaustedInfoSerdes.reverse().convert(Optional.of(privacyBudgetExhaustedInfo));

    assertThat(serialized)
        .isEqualTo(
            "{\n"
                + "  \"privacy_budget_exhausted_info\" : {\n"
                + "    \"aggregatable_input_budget_consumption_info\" : [ {\n"
                + "      \"aggregateable_input_budget_id\" : {\n"
                + "        \"filtering_id\" : 0,\n"
                + "        \"relevant_shared_info\" : {\n"
                + "          \"api\" : \"attribution-reporting\",\n"
                + "          \"attribution_destination\" : \"destination.com\",\n"
                + "          \"report_id\" : \"129470d5-3095-4385-81e2-08f5a9063549\",\n"
                + "          \"reporting_origin\" : \"bar.com\",\n"
                + "          \"scheduled_report_time\" : 1609459200.000000000,\n"
                + "          \"source_registration_time\" : 1609459200.000000000,\n"
                + "          \"version\" : \"1.0\"\n"
                + "        }\n"
                + "      }\n"
                + "    } ]\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void testSerializePrivacyBudgetExhaustedInfo_withUsedInPrivacyBudgetingView_ARAReport() {
    PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo =
        PrivacyBudgetExhaustedInfo.builder()
            .setAggregatableInputBudgetConsumptionInfos(
                ImmutableSet.of(
                    AggregatableInputBudgetConsumptionInfo.builder()
                        .setPrivacyBudgetKeyInput(
                            PrivacyBudgetKeyInput.builder()
                                .setSharedInfo(sharedInfoARA)
                                .setFilteringId(FILTERING_ID_ZERO)
                                .build())
                        .build()))
            .build();

    String serialized =
        privacyBudgetExhaustedInfoSerdes.doBackwardWithView(
            Optional.of(privacyBudgetExhaustedInfo), Views.UsedInPrivacyBudgeting.class);

    // assert that serialized string only has fields that are marked with
    // Views.UsedInPrivacyBudgeting.
    // report_id should not be present in the serialized string.
    assertThat(serialized)
        .isEqualTo(
            "{\n"
                + "  \"privacy_budget_exhausted_info\" : {\n"
                + "    \"aggregatable_input_budget_consumption_info\" : [ {\n"
                + "      \"aggregateable_input_budget_id\" : {\n"
                + "        \"filtering_id\" : 0,\n"
                + "        \"relevant_shared_info\" : {\n"
                + "          \"api\" : \"attribution-reporting\",\n"
                + "          \"attribution_destination\" : \"destination.com\",\n"
                + "          \"reporting_origin\" : \"bar.com\",\n"
                + "          \"scheduled_report_time\" : 1609459200.000000000,\n"
                + "          \"source_registration_time\" : 1609459200.000000000,\n"
                + "          \"version\" : \"1.0\"\n"
                + "        }\n"
                + "      }\n"
                + "    } ]\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void
      testSerializePrivacyBudgetExhaustedInfo_withUsedInPrivacyBudgetingView_MultipleAggregatableInputs() {

    AggregatableInputBudgetConsumptionInfo aggregatableInputBudgetConsumptionInfo1 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder()
                    .setSharedInfo(sharedInfoARA)
                    .setFilteringId(FILTERING_ID_ZERO)
                    .build())
            .build();
    AggregatableInputBudgetConsumptionInfo aggregatableInputBudgetConsumptionInfo2 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder()
                    .setSharedInfo(sharedInfoARA2)
                    .setFilteringId(FILTERING_ID_ZERO)
                    .build())
            .build();
    AggregatableInputBudgetConsumptionInfo aggregatableInputBudgetConsumptionInfo3 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder()
                    .setSharedInfo(sharedInfoARA)
                    .setFilteringId(FILTERING_ID_ONE)
                    .build())
            .build();
    AggregatableInputBudgetConsumptionInfo aggregatableInputBudgetConsumptionInfo4 =
        AggregatableInputBudgetConsumptionInfo.builder()
            .setPrivacyBudgetKeyInput(
                PrivacyBudgetKeyInput.builder()
                    .setSharedInfo(sharedInfoARA2)
                    .setFilteringId(FILTERING_ID_ONE)
                    .build())
            .build();

    PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo =
        PrivacyBudgetExhaustedInfo.builder()
            .setAggregatableInputBudgetConsumptionInfos(
                ImmutableSet.of(
                    aggregatableInputBudgetConsumptionInfo1,
                    aggregatableInputBudgetConsumptionInfo2,
                    aggregatableInputBudgetConsumptionInfo3,
                    aggregatableInputBudgetConsumptionInfo4))
            .build();

    String serialized =
        privacyBudgetExhaustedInfoSerdes.doBackwardWithView(
            Optional.of(privacyBudgetExhaustedInfo), Views.UsedInPrivacyBudgeting.class);

    /*
     * The aggregatable_input_budget_consumption_info Set will only have PrivacyBudgetKeyInput
     * i.e. {shared_info, filtering_id} combinations that generate unique privacy budget keys.
     * serialized json beautified -
     */
    assertThat(serialized)
        .isEqualTo(
            "{\n"
                + "  \"privacy_budget_exhausted_info\" : {\n"
                + "    \"aggregatable_input_budget_consumption_info\" : [ {\n"
                + "      \"aggregateable_input_budget_id\" : {\n"
                + "        \"filtering_id\" : 0,\n"
                + "        \"relevant_shared_info\" : {\n"
                + "          \"api\" : \"attribution-reporting\",\n"
                + "          \"attribution_destination\" : \"destination.com\",\n"
                + "          \"reporting_origin\" : \"bar.com\",\n"
                + "          \"scheduled_report_time\" : 1609459200.000000000,\n"
                + "          \"source_registration_time\" : 1609459200.000000000,\n"
                + "          \"version\" : \"1.0\"\n"
                + "        }\n"
                + "      }\n"
                + "    }, {\n"
                + "      \"aggregateable_input_budget_id\" : {\n"
                + "        \"filtering_id\" : 1,\n"
                + "        \"relevant_shared_info\" : {\n"
                + "          \"api\" : \"attribution-reporting\",\n"
                + "          \"attribution_destination\" : \"destination.com\",\n"
                + "          \"reporting_origin\" : \"bar.com\",\n"
                + "          \"scheduled_report_time\" : 1609459200.000000000,\n"
                + "          \"source_registration_time\" : 1609459200.000000000,\n"
                + "          \"version\" : \"1.0\"\n"
                + "        }\n"
                + "      }\n"
                + "    } ]\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void testSerializePrivacyBudgetExhaustedInfo_withUsedInPrivacyBudgetingView_PAAReport() {
    PrivacyBudgetExhaustedInfo privacyBudgetExhaustedInfo =
        PrivacyBudgetExhaustedInfo.builder()
            .setAggregatableInputBudgetConsumptionInfos(
                ImmutableSet.of(
                    AggregatableInputBudgetConsumptionInfo.builder()
                        .setPrivacyBudgetKeyInput(
                            PrivacyBudgetKeyInput.builder()
                                .setSharedInfo(sharedInfoPAA)
                                .setFilteringId(FILTERING_ID_ZERO)
                                .build())
                        .build()))
            .build();

    String serialized =
        privacyBudgetExhaustedInfoSerdes.doBackwardWithView(
            Optional.of(privacyBudgetExhaustedInfo), Views.UsedInPrivacyBudgeting.class);

    assertThat(serialized)
        .isEqualTo(
            "{\n"
                + "  \"privacy_budget_exhausted_info\" : {\n"
                + "    \"aggregatable_input_budget_consumption_info\" : [ {\n"
                + "      \"aggregateable_input_budget_id\" : {\n"
                + "        \"filtering_id\" : 0,\n"
                + "        \"relevant_shared_info\" : {\n"
                + "          \"api\" : \"protected-audience\",\n"
                + "          \"reporting_origin\" : \"bar.com\",\n"
                + "          \"scheduled_report_time\" : 1609459200.000000000,\n"
                + "          \"version\" : \"1.0\"\n"
                + "        }\n"
                + "      }\n"
                + "    } ]\n"
                + "  }\n"
                + "}");
  }

  private static final class TestEnv extends AbstractModule {}
}
