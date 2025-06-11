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

package com.google.aggregate.adtech.worker.shared.testing;

import com.google.common.base.Converter;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Converter for the {@code String} and {@code ByteSource} classes. Used in testing.
 *
 * <p>The contents of {@code ByteSource} instances cannot be compared directly, but they can be
 * converted into String instances that can be compared.
 */
public final class StringToByteSourceConverter extends Converter<String, ByteSource> {

  /** Converts a String into an instance of the {@code ByteSource} class. */
  @Override
  protected ByteSource doForward(String s) {
    return ByteSource.wrap(s.getBytes(StandardCharsets.UTF_8));
  }

  /** Converts an instance of the {@code ByteSource} class into a String. */
  @Override
  protected String doBackward(ByteSource byteSource) {
    try {
      return new String(byteSource.read(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      // Okay to do in tests but not in worker.
      throw new RuntimeException(e);
    }
  }
}
