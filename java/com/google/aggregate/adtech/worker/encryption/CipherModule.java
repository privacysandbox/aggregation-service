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

package com.google.aggregate.adtech.worker.encryption;

import com.google.inject.AbstractModule;

/** Generic guice module to encrypt and decrypt records in simulation using Tink. */
public abstract class CipherModule extends AbstractModule {

  /**
   * Returns the {@code Class} for the {@code EncryptionCipherFactory} implementation the module
   * will use.
   */
  public abstract Class<? extends EncryptionCipherFactory>
      getEncryptionCipherFactoryImplementation();

  /**
   * Arbitrary configurations that can be done by the implementing class to support dependencies
   * that are specific to that implementation.
   */
  public void configureModule() {}

  @Override
  protected void configure() {
    bind(EncryptionCipherFactory.class).to(getEncryptionCipherFactoryImplementation());
    configureModule();
  }
}
