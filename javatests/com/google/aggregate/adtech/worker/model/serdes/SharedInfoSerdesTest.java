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

package com.google.aggregate.adtech.worker.model.serdes;

import static com.google.aggregate.adtech.worker.model.SharedInfo.LATEST_VERSION;
import static com.google.aggregate.adtech.worker.model.SharedInfo.SHARED_STORAGE_API;
import static com.google.aggregate.adtech.worker.model.SharedInfo.ATTRIBUTION_REPORTING_DEBUG_API;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertFalse;

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.SharedInfo;
import com.google.inject.AbstractModule;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SharedInfoSerdesTest {

  // FIXED_TIME = Jan 01 2021 00:00:00 GMT+0000
  private static final Instant FIXED_TIME = Instant.ofEpochSecond(1609459200);

  private static final String ATTRIBUTION_REPORTING_API = "attribution-reporting";

  private static final String VERSION_ZERO_DOT_ONE = "0.1";

  private static final String DESTINATION_CHROME_GOLDEN_REPORT = "https://conversion.test";

  private static final String REPORTING_ORIGIN_CHROME_GOLDEN_REPORT = "https://report.test";

  private static final String SAMPLE_REPORT_ID = "129470d5-3095-4385-81e2-08f5a9063549";

  private static final String DESTINATION = "destination.com";

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private SharedInfoSerdes sharedInfoSerdes;

  private SharedInfo sharedInfo;

  private SharedInfo sharedInfoWithoutOptionals;

  private SharedInfo sharedInfoWithAllOptionals;

  private SharedInfo sharedInfoWithoutDebugMode;

  private SharedInfo sharedInfoWithChromeGoldenReport;

  @Before
  public void setUp() {
    sharedInfo =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setReportId(SAMPLE_REPORT_ID)
            .setScheduledReportTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .setReportingOrigin("bar.com")
            .setReportDebugMode(true)
            .build();

    sharedInfoWithoutOptionals =
        SharedInfo.builder()
            .setVersion("")
            .setReportId(SAMPLE_REPORT_ID)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin("origin.com")
            .build();

    sharedInfoWithAllOptionals =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setReportId(SAMPLE_REPORT_ID)
            .setScheduledReportTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .setReportingOrigin("origin.com")
            .setSourceRegistrationTime(FIXED_TIME)
            .build();

    sharedInfoWithoutDebugMode =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setReportId(SAMPLE_REPORT_ID)
            .setScheduledReportTime(FIXED_TIME)
            .setDestination(DESTINATION)
            .setReportingOrigin("bar.com")
            .setSourceRegistrationTime(FIXED_TIME)
            .build();

    sharedInfoWithChromeGoldenReport =
        SharedInfo.builder()
            .setVersion(VERSION_ZERO_DOT_ONE)
            .setApi(ATTRIBUTION_REPORTING_API)
            .setReportId("21abd97f-73e8-4b88-9389-a9fee6abda5e")
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setSourceRegistrationTime(Instant.ofEpochSecond(1234483200))
            .setReportDebugMode(true)
            .build();
  }

  @Test
  public void testDeserializesFromStringWithUnknownFields() {
    String sharedInfoJsonString =
        "{\"version\": \"0.1\", \"debug_mode\":\"enabled\","
            + " \"scheduled_report_time\": \"1609459200\", \"reporting_origin\": \"bar.com\","
            + " \"unknown_field\": \"fizzbuzz\", \"attribution_destination\": \"destination.com\","
            + " \"report_id\":\"129470d5-3095-4385-81e2-08f5a9063549\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);
    assertThat(deserialized).hasValue(sharedInfo);
  }

  @Test
  public void testDeserializeFromSharedInfoJsonWithOutOptionals() {
    String sharedInfoJsonString =
        "{\"version\": \"\", \"scheduled_report_time\": \"1609459200\","
            + " \"unknown_field\": \"fizzbuzz\", \"reporting_origin\":\"origin.com\","
            + " \"report_id\":\"129470d5-3095-4385-81e2-08f5a9063549\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).hasValue(sharedInfoWithoutOptionals);
  }

  @Test
  public void testDeserializeFromSharedInfoJsonWithAllOptionals() {
    String sharedInfoJsonString =
        "{\"version\": \"0.1\", \"api\":\"attribution-reporting\", \"scheduled_report_time\":"
            + " \"1609459200\",  \"unknown_field\": \"fizzbuzz\", \"attribution_destination\":"
            + " \"destination.com\", \"reporting_origin\":\"origin.com\","
            + " \"source_registration_time\":\"1609459200\",  \"privacy_budget_key\":"
            + " \"test_privacy_budget_key\", \"report_id\":\"129470d5-3095-4385-81e2-08f5a9063549\""
            + " }";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).hasValue(sharedInfoWithAllOptionals);
  }

  /** Deserialize from a sample shared_info generated by Chrome without debug mode */
  @Test
  public void testDeserializeFromStringWithoutDebugMode() {
    String sharedInfoJsonString =
        "{\"version\": \"0.1\", \"scheduled_report_time\":"
            + " \"1609459200\", \"reporting_origin\": \"bar.com\", \"unknown_field\": \"fizzbuzz\","
            + " \"report_id\":\"129470d5-3095-4385-81e2-08f5a9063549\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setVersion(VERSION_ZERO_DOT_ONE)
                .setReportId("129470d5-3095-4385-81e2-08f5a9063549")
                .setReportingOrigin("bar.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1609459200))
                .build());
  }

  @Test
  public void testSerializeAndDeserialize() {
    // No setup

    String serialized = sharedInfoSerdes.reverse().convert(Optional.of(sharedInfo));
    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(serialized);

    assertThat(deserialized).hasValue(sharedInfo);
  }

  /** Deserialize from a sample shared_info generated by Chrome without debug mode */
  @Test
  public void testSerializeAndDeserializeWithoutDebugMode() {
    // No setup

    String serialized = sharedInfoSerdes.reverse().convert(Optional.of(sharedInfoWithoutDebugMode));
    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(serialized);

    assertThat(deserialized).hasValue(sharedInfoWithoutDebugMode);
  }

  /**
   * Test if setting debug_mode false in SharedInfo object doesn't add "debug_mode" string to
   * serialized SharedInfo string
   */
  @Test
  public void testSerializeSharedInfoDebugModeDisabled() {
    // No setup
    SharedInfo sharedInfoDebugModeDisabled =
        SharedInfo.builder()
            .setVersion(LATEST_VERSION)
            .setReportId(SAMPLE_REPORT_ID)
            .setScheduledReportTime(FIXED_TIME)
            .setReportingOrigin("bar.com")
            .setReportDebugMode(false)
            .build();

    String serialized =
        sharedInfoSerdes.reverse().convert(Optional.ofNullable(sharedInfoDebugModeDisabled));

    assertFalse(serialized.contains("debug_mode"));
  }

  /** Serialize share-info for simulation. Check @JsonIgnore and @JsonProperty setting correctly */
  @Test
  public void testSerializeSharedInfoIgnoreField() {
    // No setup

    String serialized = sharedInfoSerdes.reverse().convert(Optional.ofNullable(sharedInfo));

    assertFalse(serialized.contains("reportDebugMode"));
  }

  @Test
  public void testReturnsEmptyOptionalForBadInput() {
    String badInput = "invalid string";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(badInput);

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void testReturnsEmptyStringForEmptyOptional() throws Exception {
    // No setup

    String serialized = sharedInfoSerdes.reverse().convert(Optional.empty());

    assertThat(serialized.isEmpty()).isTrue();
  }

  /*
   * Test based on Chrome Aggregatable Golden Report
   * https://source.chromium.org/chromium/chromium/src/+/main:content/test/data/attribution_reporting/aggregatable_report_goldens/latest/report_1.json
   * */
  @Test
  public void testDeserializeOnChromeAggregatableGoldenReport() {
    String sharedInfoJsonString =
        "{\"api\":\"attribution-reporting\",\"attribution_destination\":\"https://conversion.test\","
            + "\"debug_mode\":\"enabled\",\"report_id\":\"21abd97f-73e8-4b88-9389-a9fee6abda5e\","
            + "\"reporting_origin\":\"https://report.test\",\"scheduled_report_time\":\"1234486400\","
            + "\"source_registration_time\":\"1234483200\",\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).hasValue(sharedInfoWithChromeGoldenReport);
  }

  @Test
  public void deserialize_withGoldenReportVersion1() {
    String sharedInfoJsonString =
        "{\"api\":\"shared-storage\",\"debug_mode\":\"enabled\",\"report_id\":\"21abd97f-73e8-4b88-9389-a9fee6abda5e\",\"reporting_origin\":\"https://report.test\",\"scheduled_report_time\":\"1234486400\",\"version\":\"1.0\"}";
    SharedInfo expectedSharedInfo =
        SharedInfo.builder()
            .setVersion("1.0")
            .setApi(SHARED_STORAGE_API)
            .setReportId("21abd97f-73e8-4b88-9389-a9fee6abda5e")
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setReportDebugMode(true)
            .build();

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).hasValue(expectedSharedInfo);
  }

  /** Test with Chrome generated attribution-reporting-debug reports used from here - b/324143474 */
  @Test
  public void deserialize_withGoldenReport_debugApi() {
    String sharedInfoJsonString =
        "{\"api\":\"attribution-reporting-debug\",\"attribution_destination\":\"https://conversion.test\",\"debug_mode\":\"enabled\",\"report_id\":\"21abd97f-73e8-4b88-9389-a9fee6abda5e\",\"reporting_origin\":\"https://report.test\",\"scheduled_report_time\":\"1234486400\",\"version\":\"0.1\"}";
    SharedInfo expectedSharedInfo =
        SharedInfo.builder()
            .setVersion("0.1")
            .setApi(ATTRIBUTION_REPORTING_DEBUG_API)
            .setReportId("21abd97f-73e8-4b88-9389-a9fee6abda5e")
            .setReportingOrigin(REPORTING_ORIGIN_CHROME_GOLDEN_REPORT)
            .setDestination(DESTINATION_CHROME_GOLDEN_REPORT)
            .setScheduledReportTime(Instant.ofEpochSecond(1234486400))
            .setReportDebugMode(true)
            .build();

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).hasValue(expectedSharedInfo);
  }

  /** Chrome generated fledge and shared storage reports used from here - b/265960702 */
  @Test
  public void testDeserializeSharedInfoFromChrome_fledge1() {
    String sharedInfoJsonString =
        "{\"api\":\"fledge\",\"debug_mode\":\"enabled\",\"report_id\":\"90bcae5e-224d-47ae-bbd8-b4f2d634e6d1\","
            + "\"reporting_origin\":\"https://example.com\",\"scheduled_report_time\":\"1674078788\","
            + "\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setApi("fledge")
                .setReportDebugModeString("enabled")
                .setReportId("90bcae5e-224d-47ae-bbd8-b4f2d634e6d1")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1674078788))
                .setVersion(VERSION_ZERO_DOT_ONE)
                .build());
  }

  @Test
  public void testDeserializeSharedInfoFromChrome_sharedStorage1() {
    String sharedInfoJsonString =
        "{\"api\":\"shared-storage\",\"debug_mode\":\"enabled\",\"report_id\":\"0222ce51-8596-4dec-9994-90df2508ae90\","
            + "\"reporting_origin\":\"https://example.com\",\"scheduled_report_time\":\"1674079133\","
            + "\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setApi("shared-storage")
                .setReportDebugModeString("enabled")
                .setReportId("0222ce51-8596-4dec-9994-90df2508ae90")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1674079133))
                .setVersion(VERSION_ZERO_DOT_ONE)
                .build());
  }

  @Test
  public void convert_withSourceRegistrationTimeZero() {
    String sharedInfoJsonString =
        "{\"source_registration_time\":0,\"privacy_budget_key\":\"test_privacy_budget_key\",\""
            + "report_id\":\"cbc6fb00-c946-4eb6-a401-aac133f7f0b8\",\"reporting_origin\":"
            + "\"https://example.com\",\"scheduled_report_time\":\"1648673933\","
            + "\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setSourceRegistrationTime(Instant.ofEpochSecond(0))
                .setVersion(VERSION_ZERO_DOT_ONE)
                .setReportId("cbc6fb00-c946-4eb6-a401-aac133f7f0b8")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1648673933))
                .build());
  }

  @Test
  public void convert_withSourceRegistrationTimeNegative() {
    String sharedInfoJsonString =
        "{\"source_registration_time\":-1,\""
            + "report_id\":\"cbc6fb00-c946-4eb6-a401-aac133f7f0b8\",\"reporting_origin\":"
            + "\"https://example.com\",\"scheduled_report_time\":\"1648673933\","
            + "\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setSourceRegistrationTime(Instant.ofEpochSecond(-1))
                .setVersion(VERSION_ZERO_DOT_ONE)
                .setReportId("cbc6fb00-c946-4eb6-a401-aac133f7f0b8")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1648673933))
                .build());
  }

  @Test
  public void convert_withSourceRegistrationTimeMinValue() {
    String sharedInfoJsonString =
        "{\"source_registration_time\":-31557014167219199,\""
            + "report_id\":\"cbc6fb00-c946-4eb6-a401-aac133f7f0b8\",\"reporting_origin\":"
            + "\"https://example.com\",\"scheduled_report_time\":\"1648673933\",\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setSourceRegistrationTime(Instant.ofEpochSecond(-31557014167219199L))
                .setVersion(VERSION_ZERO_DOT_ONE)
                .setReportId("cbc6fb00-c946-4eb6-a401-aac133f7f0b8")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1648673933))
                .build());
  }

  @Test
  public void convert_withInvalidValueForSourceRegistrationTime_failsParsing() {
    // Setting source_registration_time value higher than maximum limit.
    String sharedInfoJsonString =
        "{\"source_registration_time\":31556889864403200,\""
            + "report_id\":\"cbc6fb00-c946-4eb6-a401-aac133f7f0b8\",\"reporting_origin\":"
            + "\"https://example.com\",\"scheduled_report_time\":\"1648673933\",\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void convert_withSourceRegistrationNotSet() {
    String sharedInfoJsonString =
        "{ \"report_id\":\"cbc6fb00-c946-4eb6-a401-aac133f7f0b8\",\"reporting_origin\":"
            + "\"https://example.com\",\"scheduled_report_time\":\"1648673933\",\"version\":\"0.1\"}";

    Optional<SharedInfo> deserialized = sharedInfoSerdes.convert(sharedInfoJsonString);

    assertThat(deserialized)
        .hasValue(
            SharedInfo.builder()
                .setVersion(VERSION_ZERO_DOT_ONE)
                .setReportId("cbc6fb00-c946-4eb6-a401-aac133f7f0b8")
                .setReportingOrigin("https://example.com")
                .setScheduledReportTime(Instant.ofEpochSecond(1648673933))
                .build());
  }

  private static final class TestEnv extends AbstractModule {}
}
