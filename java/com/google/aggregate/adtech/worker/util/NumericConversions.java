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

import static java.nio.charset.StandardCharsets.US_ASCII;

import com.google.errorprone.annotations.Var;
import java.math.BigInteger;
import java.util.Arrays;

/** Utilities for handling conversions of byte arrays to numeric types */
public final class NumericConversions {

  private static final int POSITIVE_SIGN = 1; // For use when reading unsigned values from bytes

  public static final BigInteger UINT_128_MAX =
      new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); // 2^128 - 1
  public static final long UINT_32_MAX = 0xffffffffL; // 2^32 - 1

  /**
   * Reads a positive 32-bit integer from a big-endian byte array. The range of returned values is 0
   * to 2^32-1 inclusive.
   *
   * @param bytes the byte array to read from. Must be 4 bytes or shorter.
   * @return the decoded 16-bit integer as a {@link Long}
   */
  public static Long uInt32FromBytes(byte[] bytes) {
    if (bytes.length > 4) {
      throw new IllegalArgumentException(
          "Byte array provided was too long. Must be 4 bytes or shorter. Length was "
              + bytes.length);
    }

    // Decode from big-endian bytes. BigInteger assumes the byte array is big endian.
    BigInteger bigIntegerValue = new BigInteger(POSITIVE_SIGN, bytes);
    long longValue = bigIntegerValue.longValue();

    // Check that the range is valid (0 <= value <= 2^32-1)
    boolean lessThanMin = longValue < 0;
    boolean greaterThanMax = longValue > UINT_32_MAX;
    if (lessThanMin || greaterThanMax) {
      throw new IllegalArgumentException(
          "Value outside of valid range. Valid range is 0 to " + UINT_32_MAX);
    }
    return longValue;
  }

  /**
   * Reads a positive 128-bit integer from a big-endian byte array. The range of returned values is
   * 0 to 2^128-1 inclusive.
   *
   * @param bytes the byte array to read from. Must be 16 bytes or shorter.
   * @return the decoded 128-bit integer as a {@link BigInteger}
   */
  public static BigInteger uInt128FromBytes(byte[] bytes) {
    if (bytes.length > 16) {
      throw new IllegalArgumentException(
          "Byte array provided was too long. Must be 16 bytes or shorter. Length was "
              + bytes.length);
    }

    // Decode from big-endian bytes. BigInteger assumes the byte array is big endian.
    BigInteger value = new BigInteger(POSITIVE_SIGN, bytes);

    // Check that the range is valid (0 <= value <= 2^128-1)
    boolean lessThanMin = value.compareTo(BigInteger.ZERO) < 0;
    boolean greaterThanMax = value.compareTo(UINT_128_MAX) > 0;
    if (lessThanMin || greaterThanMax) {
      throw new IllegalArgumentException(
          "Bucket outside of valid range. Valid range is 0 to " + UINT_128_MAX);
    }
    return value;
  }

  /**
   * Converts a {@link BigInteger} to a big-endian byte array representing the unsigned value
   *
   * @param value the value to convert. Must be greater than or equal to 0.
   * @return the value converted to bytes
   */
  public static byte[] toUnSignedByteArray(BigInteger value) {
    // Check that the range is valid (0 <= value <= 2^128-1)
    boolean lessThanMin = value.compareTo(BigInteger.ZERO) < 0;
    boolean greaterThanMax = value.compareTo(UINT_128_MAX) > 0;
    if (lessThanMin || greaterThanMax) {
      throw new IllegalArgumentException(
          "Provided value must be in the range of 0 to 2^128-1 inclusive. Value was " + value);
    }
    byte[] bytes = value.toByteArray();

    // Remove any leading 0 byte. A leading 0 byte is used by BigInteger to indicate sign for some
    // values. This byte isn't needed since this function only operates on positive or 0-valued
    // numbers.
    if (bytes[0] == 0) {
      return Arrays.copyOfRange(bytes, 1, bytes.length);
    }
    return bytes;
  }

  /** Simple utility to create BigInteger from string rep from an int */
  public static BigInteger createBucketFromInt(int bucket) {
    return NumericConversions.uInt128FromBytes((String.valueOf(bucket)).getBytes(US_ASCII));
  }

  /** Simple utility to create BigInteger from string rep from string */
  public static BigInteger createBucketFromString(String bucket) {
    return NumericConversions.uInt128FromBytes(bucket.getBytes(US_ASCII));
  }

  /**
   * Converts string representation of percentage value to double.
   *
   * @param percentageInString
   */
  public static double getPercentageValue(String percentageInString) {
    @Var String percentageInStringTrimmed = percentageInString.trim();
    if (percentageInStringTrimmed == null || percentageInStringTrimmed.isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid string representation of percentage value. The string is empty: %s",
              percentageInString));
    }
    if (percentageInStringTrimmed.endsWith("%")) {
      percentageInStringTrimmed =
          percentageInStringTrimmed.substring(0, percentageInStringTrimmed.length() - 1);
    }

    double percentValue;
    try {
      percentValue = Double.parseDouble(percentageInStringTrimmed);
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid string representation of percentage value: %s", percentageInString),
          nfe);
    }
    if (percentValue < 0 || percentValue > 100) {
      throw new IllegalArgumentException(
          String.format("Invalid value for percentage: %s", percentageInString));
    }

    return percentValue;
  }

  /** This class should not be instantiated */
  private NumericConversions() {}
}
