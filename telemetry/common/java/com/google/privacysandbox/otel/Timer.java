/*
 * Copyright 2023 Google LLC
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

package com.google.privacysandbox.otel;

/** Timer interface to measure latency of a code-block by implementing {@link AutoCloseable} */
public interface Timer extends AutoCloseable {
  /** Add an event within a time span */
  void addEvent(String text);

  /** Overrides {@link AutoCloseable} close method so as to not throw exception */
  @Override
  void close();
}
