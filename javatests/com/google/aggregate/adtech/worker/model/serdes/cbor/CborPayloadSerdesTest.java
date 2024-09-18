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

package com.google.aggregate.adtech.worker.model.serdes.cbor;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.common.io.ByteSource;
import com.google.common.primitives.UnsignedLong;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CborPayloadSerdesTest {

  @Rule public final Acai acai = new Acai(TestEnv.class);

  @Inject private CborPayloadSerdes cborPayloadSerdes;
  private Payload payload;

  @Before
  public void setUp() {
    payload =
        Payload.builder()
            .addFact(
                Fact.builder().setBucket(BigInteger.valueOf(123456789)).setValue(12345).build())
            .addFact(
                Fact.builder().setBucket(BigInteger.valueOf(987654321)).setValue(98765).build())
            .build();
  }

  @Test
  public void testDeserializeGoldenReports() throws Exception {
    // Get the space-separated paths to Chromium's golden reports for
    // Attribution Reporting and Private Aggregation.
    String paths_from_env_private_aggregation =
        System.getenv("CHROMIUM_LATEST_GOLDENS_PRIVATE_AGGREGATION");
    String paths_from_env_attribution_reporting =
        System.getenv("CHROMIUM_LATEST_GOLDENS_ATTRIBUTION_REPORTING");

    assertThat(paths_from_env_private_aggregation).isNotNull();
    assertThat(paths_from_env_attribution_reporting).isNotNull();

    List<String> golden_paths = new ArrayList<>();
    golden_paths.addAll(Arrays.asList(paths_from_env_private_aggregation.split(" ")));
    golden_paths.addAll(Arrays.asList(paths_from_env_attribution_reporting.split(" ")));

    for (String golden_path : golden_paths) {
      Path path = Path.of(golden_path);
      File file = path.toFile();

      assertThat(file.exists()).isTrue();

      String jsonString = Files.readString(path);
      JsonNode parsed = new ObjectMapper().readTree(jsonString);

      // These JSON files contain an array of base64 CBOR strings.
      if (file.getName().endsWith("_cleartext_payloads.json")) {
        for (var it = parsed.iterator(); it.hasNext(); ) {
          JsonNode payload_obj = it.next();
          byte[] payload_bytes = payload_obj.binaryValue();
          Optional<Payload> parsed_payload =
              cborPayloadSerdes.convert(ByteSource.wrap(payload_bytes));
          assertThat(parsed_payload).isPresent();
        }

      } else {
        for (var it = parsed.at("/aggregation_service_payloads").iterator(); it.hasNext(); ) {
          JsonNode payload_obj = it.next();
          // Make sure that we can parse the payload. Note that the "payload"
          // field is encrypted; this is why the "*_cleartext_payloads.json"
          // files exist.
          if (payload_obj.has("/debug_cleartext_payload")) {
            byte[] debug_payload = payload_obj.at("/debug_cleartext_payload").binaryValue();
            Optional<Payload> parsed_debug_payload =
                cborPayloadSerdes.convert(ByteSource.wrap(debug_payload));
            assertThat(parsed_debug_payload).isPresent();
          }
        }
      }
    }
  }

  @Test
  public void testDeserializeFromCborBytes_debugReport1() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x3)).setValue(4).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_DEBUG_REPORT_1_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_debugReport2() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x0)).setValue(0).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_DEBUG_REPORT_2_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report1() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_1_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report2() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_2_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report3() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x3)).setValue(4).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_3_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report4() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x1)).setValue(2).build())
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x3)).setValue(4).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_4_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report5() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(
                        BigInteger.valueOf(1)
                            .shiftLeft(128)
                            .subtract(BigInteger.valueOf(1) /* = 2^128-1 */))
                    .setValue(1000)
                    .build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_5_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report6() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(new BigInteger("340282366920938463463374607431768211455"))
                    .setValue(1000)
                    .build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_6_LOCATION")), expectedPayload);
  }

  @Test
  public void deserializeFromCborBytes_nullReport() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(new BigInteger("0")).setValue(0).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_NULL_REPORT_LOCATION")), expectedPayload);
  }

  private void readCborBytesFromFileAndAssert(Path path, Payload expectedPayload)
      throws IOException {
    ByteSource cborBytes = ByteSource.wrap(Files.readAllBytes(path));

    Optional<Payload> deserialized = cborPayloadSerdes.convert(cborBytes);

    assertThat(deserialized).hasValue(expectedPayload);
  }

  @Test
  public void testSerializeAndDeserialize() throws IOException {
    // No setup

    ByteSource serialized = cborPayloadSerdes.reverse().convert(Optional.of(payload));

    Optional<Payload> deserialized = cborPayloadSerdes.convert(serialized);

    assertThat(deserialized).hasValue(payload);
  }

  @Test
  public void testWithInvalidValue() {
    Long largerThanUnsignedIntMax = 0xffffffffL + 1; // 4294967296, 2^32
    Payload payloadLargerThanUnsignedIntMax =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(1))
                    .setValue(largerThanUnsignedIntMax)
                    .build())
            .build();

    ByteSource serialized =
        cborPayloadSerdes.reverse().convert(Optional.of(payloadLargerThanUnsignedIntMax));
    Optional<Payload> deserialized = cborPayloadSerdes.convert(serialized);

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void testWithInvalidBucket() {
    BigInteger bucketTooLarge = BigInteger.valueOf(1).shiftLeft(128); // 2^128
    Payload payloadBucketTooLarge =
        Payload.builder()
            .addFact(Fact.builder().setBucket(bucketTooLarge).setValue(1).build())
            .build();

    ByteSource serialized = cborPayloadSerdes.reverse().convert(Optional.of(payloadBucketTooLarge));
    Optional<Payload> deserialized = cborPayloadSerdes.convert(serialized);

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void testReturnsEmptyOptionalForBadInput() {
    ByteSource badInput = ByteSource.wrap(new byte[] {0x01, 0x02});

    Optional<Payload> deserialized = cborPayloadSerdes.convert(badInput);

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void testReturnsEmptyOptionalForEmptyByteSource() {
    // No setup

    Optional<Payload> deserialized = cborPayloadSerdes.convert(ByteSource.empty());

    assertThat(deserialized).isEmpty();
  }

  @Test
  public void testReturnsEmptyByteSourceForEmptyOptional() throws Exception {
    // No setup

    ByteSource serialized = cborPayloadSerdes.reverse().convert(Optional.empty());

    assertThat(serialized.isEmpty()).isTrue();
  }

  @Test
  public void withIdsInFact() {
    Payload payload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(12345))
                    .setValue(12345)
                    .setId(UnsignedLong.valueOf((1L << 64) - 1))
                    .build())
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(987654321))
                    .setValue(987654321)
                    .setId(UnsignedLong.valueOf(Integer.MAX_VALUE + 1L))
                    .build())
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(8563215486562L))
                    .setValue(5555556)
                    .setId(
                        UnsignedLong.valueOf(
                            BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)))
                    .build())
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(4444))
                    .setValue(4444)
                    .setId(UnsignedLong.ZERO)
                    .build())
            .build();

    ByteSource serialized = cborPayloadSerdes.reverse().convert(Optional.of(payload));
    Optional<Payload> deserialized = cborPayloadSerdes.convert(serialized);

    assertThat(deserialized).hasValue(payload);
  }

  @Test
  public void deserializeFromCborBytes_reportWithId() throws Exception {
    Payload.Builder expectedPayload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(new BigInteger("1"))
                    .setValue(2)
                    .setId(UnsignedLong.ZERO)
                    .build())
            .addFact(
                Fact.builder()
                    .setBucket(new BigInteger("3"))
                    .setValue(4)
                    .setId(UnsignedLong.valueOf(1))
                    .build());
    // null padding to 20 contributions.
    for (int ind = 0; ind < 18; ind++) {
      expectedPayload.addFact(
          Fact.builder()
              .setBucket(new BigInteger("0"))
              .setValue(0)
              .setId(UnsignedLong.ZERO)
              .build());
    }

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_WITH_ID_1_LOCATION")), expectedPayload.build());
  }

  @Test
  public void deserializeFromCborBytes_reportWith32BitId() throws Exception {
    Payload.Builder expectedPayload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(new BigInteger("1"))
                    .setValue(2)
                    .setId(UnsignedLong.valueOf(1))
                    .build());
    // null padding to 20 contributions.
    for (int ind = 0; ind < 19; ind++) {
      expectedPayload.addFact(
          Fact.builder()
              .setBucket(new BigInteger("0"))
              .setValue(0)
              .setId(UnsignedLong.ZERO)
              .build());
    }

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_WITH_ID_2_LOCATION")), expectedPayload.build());
  }

  /** No overrides or bindings needed */
  private static final class TestEnv extends AbstractModule {}
}
