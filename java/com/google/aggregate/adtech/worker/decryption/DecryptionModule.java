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

package com.google.aggregate.adtech.worker.decryption;

import com.google.inject.AbstractModule;

/** Extension of AbstractModule to be implemented for various decryption methods. */
public abstract class DecryptionModule extends AbstractModule {

  /**
   * Returns the {@code Class} for the {@code DecryptionCipherFactory} implementation the module
   * will use.
   */
  public abstract Class<? extends DecryptionCipherFactory>
      getDecryptionCipherFactoryImplementation();

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  public void configureModule() {}

  @Override
  protected final void configure() {
    bind(DecryptionCipherFactory.class).to(getDecryptionCipherFactoryImplementation());
    configureModule();
  }
}
