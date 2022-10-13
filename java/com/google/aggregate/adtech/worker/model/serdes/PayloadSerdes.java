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

package com.google.aggregate.adtech.worker.model.serdes;

import com.google.aggregate.adtech.worker.model.Payload;
import com.google.common.base.Converter;
import com.google.common.io.ByteSource;
import java.util.Optional;

/**
 * Serializing and Deserialization of ByteSource to/from {@link Payload} for encryption.
 *
 * <p>If conversion is successful then the {@code Optional} will have the result inside it. If
 * conversion fails then {@code Optional.empty()} will be returned.
 *
 * <p>Optionals are used in lieu of checked exceptions.
 */
public abstract class PayloadSerdes extends Converter<ByteSource, Optional<Payload>> {}
