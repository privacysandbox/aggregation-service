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

import com.google.acai.Acai;
import com.google.aggregate.adtech.worker.model.Fact;
import com.google.aggregate.adtech.worker.model.Payload;
import com.google.common.io.ByteSource;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public void testDeserializeFromCborBytes_report1() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x4d2)).setValue(5).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_1_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report2() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x4d2)).setValue(500).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_2_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report3() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0x45b352)).setValue(123).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_3_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report4() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(
                Fact.builder()
                    .setBucket(BigInteger.valueOf(1).shiftLeft(120)) /* = 2^120 */
                    .setValue(2)
                    .build())
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
                    .setValue(345)
                    .build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_5_LOCATION")), expectedPayload);
  }

  @Test
  public void testDeserializeFromCborBytes_report6() throws Exception {
    Payload expectedPayload =
        Payload.builder()
            .addFact(Fact.builder().setBucket(BigInteger.valueOf(0)).setValue(345).build())
            .build();

    readCborBytesFromFileAndAssert(
        Path.of(System.getenv("CBOR_REPORT_6_LOCATION")), expectedPayload);
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

  /** No overrides or bindings needed */
  private static final class TestEnv extends AbstractModule {}
}
