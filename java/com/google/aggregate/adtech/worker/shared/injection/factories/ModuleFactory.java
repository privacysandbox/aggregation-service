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

package com.google.aggregate.adtech.worker.shared.injection.factories;

import static com.google.common.collect.MoreCollectors.onlyElement;

import com.google.common.collect.Streams;
import com.google.inject.Module;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/** Exposes a method to retrieve dynamically registered modules. */
public final class ModuleFactory {

  /** Dynamically load a module with {@code @AutoService(Class&lt;S&gt;)} annotation specified. */
  public static <S extends Module> S getModule(Class<S> serviceType) {
    try {
      return Streams.stream(ServiceLoader.load(serviceType)).collect(onlyElement());
    } catch (NoSuchElementException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }
}
