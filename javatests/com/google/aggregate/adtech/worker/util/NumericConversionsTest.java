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

package com.google.aggregate.adtech.worker.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Bytes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NumericConversionsTest {

  @Test
  public void testUInt32FromBytes_zeroValue() {
    byte[] bytes = new byte[] {};
    long expected = 0L;

    convertUInt32FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt32FromBytes_oneByte() {
    byte[] bytes = new byte[] {0xf}; // 1111
    long expected = 15;

    convertUInt32FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt32FromBytes_multipleBytes() {
    byte[] bytes = new byte[] {(byte) 0xff, (byte) 0xd0, 0x15}; // 11111111 11010000 00010101
    long expected = 16764949;

    convertUInt32FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt32FromBytes_maxValue() {
    // 32 bits all 1s
    byte[] bytes = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    long expected = (1L << 32) - 1; // 2^32-1

    convertUInt32FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt32FromBytes_aboveRange() {
    // 1 bit followed by 32 0 bits, 2^32
    byte[] bytes = new byte[] {0x01, 0x00, 0x00, 0x00, 0x00};

    assertThrows(IllegalArgumentException.class, () -> NumericConversions.uInt32FromBytes(bytes));
  }

  @Test
  public void testUInt128FromBytes_zeroValue() {
    byte[] bytes = new byte[] {};
    BigInteger expected = BigInteger.valueOf(0);

    convertUInt128FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt128FromBytes_oneByte() {
    byte[] bytes = new byte[] {0xe}; // 1110
    BigInteger expected = BigInteger.valueOf(14);

    convertUInt128FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt128FromBytes_multipleBytes() {
    // 11101111 10110111 01000010 10101100 01010000
    byte[] bytes = new byte[] {(byte) 0xef, (byte) 0xb7, 0x42, (byte) 0xac, 0x50};
    BigInteger expected = BigInteger.valueOf(1029571783760L);

    convertUInt128FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt128FromBytes_maxValue() {
    // 128 bits all 1s
    byte[] bytes =
        new byte[] {
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff
        };
    BigInteger expected = BigInteger.valueOf(1).shiftLeft(128).subtract(BigInteger.valueOf(1));

    convertUInt128FromBytesAndAssert(bytes, expected);
  }

  @Test
  public void testUInt128FromBytes_aboveMaxValue() {
    // 1 bit followed by 128 0-bits, 2^128
    byte[] bytes =
        new byte[] {
          (byte) 0x01,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00,
          (byte) 0x00
        };

    assertThrows(IllegalArgumentException.class, () -> NumericConversions.uInt128FromBytes(bytes));
  }

  @Test
  public void testToUnsingedByteArray_zero() {
    BigInteger value = BigInteger.valueOf(0);
    byte[] expected = new byte[] {};

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_one() {
    BigInteger value = BigInteger.valueOf(1);
    byte[] expected = new byte[] {0x01};

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_oneByte() {
    BigInteger value = BigInteger.valueOf(255); // 2^8-1
    byte[] expected = new byte[] {(byte) 0xff};

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_uInt32Max() {
    BigInteger value =
        BigInteger.valueOf(1).shiftLeft(32).subtract(BigInteger.valueOf(1)); // 2^32-1
    byte[] expected = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_manyBytes() {
    BigInteger value = BigInteger.valueOf(12345678901234567L);
    byte[] expected = new byte[] {(byte) 0x2b, (byte) 0xdc, 0x54, 0x5d, 0x6b, 0x4b, (byte) 0x87};

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_uInt128Max() {
    BigInteger value =
        BigInteger.valueOf(1).shiftLeft(128).subtract(BigInteger.valueOf(1)); // 2^128-1
    byte[] expected =
        new byte[] {
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff,
          (byte) 0xff
        };

    convertToByteArrayAndAssert(value, expected);
  }

  @Test
  public void testToUnsingedByteArray_negativeValue() {
    BigInteger value = BigInteger.valueOf(-1);

    assertThrows(
        IllegalArgumentException.class, () -> NumericConversions.toUnSignedByteArray(value));
  }

  @Test
  public void testToUnsingedByteArray_greaterThanMaxValue() {
    BigInteger value = BigInteger.valueOf(1).shiftLeft(128); // 2^128

    assertThrows(
        IllegalArgumentException.class, () -> NumericConversions.toUnSignedByteArray(value));
  }

  @Test
  public void testCreateBucketFromInt() {
    int a = 61;
    BigInteger bigInteger = NumericConversions.createBucketFromInt(a);
    byte[] bytesInAvro = NumericConversions.toUnSignedByteArray(bigInteger);
    String inTextFile = new String(bytesInAvro, StandardCharsets.US_ASCII);

    assertThat("61").isEqualTo(inTextFile);
  }

  @Test
  public void testCreateBucketFromString() {
    String a = "61"; // in  ascii it represents 1
    BigInteger bigInteger = NumericConversions.createBucketFromString(a);
    byte[] bytesInAvro = NumericConversions.toUnSignedByteArray(bigInteger);
    String inTextFile = new String(bytesInAvro, StandardCharsets.US_ASCII);

    assertThat("61").isEqualTo(inTextFile);
  }

  private void convertUInt32FromBytesAndAssert(byte[] bytes, long expected) {
    Long value = NumericConversions.uInt32FromBytes(bytes);

    assertThat(value).isEqualTo(expected);
  }

  private void convertUInt128FromBytesAndAssert(byte[] bytes, BigInteger expected) {
    BigInteger value = NumericConversions.uInt128FromBytes(bytes);

    assertThat(value).isEqualTo(expected);
  }

  private void convertToByteArrayAndAssert(BigInteger value, byte[] expected) {
    byte[] bytes = NumericConversions.toUnSignedByteArray(value);

    assertThat(Bytes.asList(bytes)).containsExactlyElementsIn(Bytes.asList(expected));
  }
}
