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

package com.google.aggregate.adtech.worker.model;

/**
 * This class can be used to specify Views for models. Jackson's ObjectMapper provides capability to
 * add views as annotation to Java object properties. This can be used for controlling which
 * properties of a Java object are included in its serialization or populated during
 * deserialization. More info - <a
 * href="https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html">...</a>
 */
public class Views {
  public static class UsedInPrivacyBudgeting {}
}
