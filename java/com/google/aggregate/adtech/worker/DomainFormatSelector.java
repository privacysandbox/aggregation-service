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

package com.google.aggregate.adtech.worker;

import com.google.aggregate.adtech.worker.aggregation.domain.AvroOutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.OutputDomainProcessor;
import com.google.aggregate.adtech.worker.aggregation.domain.TextOutputDomainProcessor;

/** CLI enum to select which {@link OutputDomainProcessor} implementation to use in the binary. */
public enum DomainFormatSelector {
  TEXT_FILE(TextOutputDomainProcessor.class),
  AVRO(AvroOutputDomainProcessor.class);
  private final Class<? extends OutputDomainProcessor> domainProcessorClass;

  DomainFormatSelector(Class<? extends OutputDomainProcessor> domainProcessorClass) {
    this.domainProcessorClass = domainProcessorClass;
  }

  public Class<? extends OutputDomainProcessor> getDomainProcessorClass() {
    return domainProcessorClass;
  }
}
